package com.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.dto.InterviewDTOs.*;
import com.interview.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    @Autowired private AiService aiService;
    @Autowired private InterviewSessionRepository sessionRepo;
    @Autowired private QuestionAnswerRepository qaRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Start Session ────────────────────────────────────────────────

    @Transactional
    public SessionResponse startSession(StartSessionRequest req) {
        log.info("Starting interview session for {} in domain {}", req.getCandidateName(), req.getDomain());

        // Generate questions using AI
        List<QuestionDTO> questions = generateQuestions(req.getResumeText(), req.getDomain());

        // Save session
        InterviewSession session = InterviewSession.builder()
                .candidateName(req.getCandidateName())
                .domain(req.getDomain())
                .resumeText(req.getResumeText())
                .completed(false)
                .build();
        session = sessionRepo.save(session);

        return SessionResponse.builder()
                .sessionId(session.getId())
                .candidateName(session.getCandidateName())
                .domain(session.getDomain())
                .questions(questions)
                .startedAt(session.getCreatedAt())
                .build();
    }

    // ── Generate Questions ───────────────────────────────────────────

    private List<QuestionDTO> generateQuestions(String resumeText, String domain) {
        String systemPrompt = """
                You are an expert technical interviewer. Generate 5 interview questions based on the resume and domain provided.
                Return ONLY a valid JSON array. No explanation, no markdown, just the JSON array.
                Each question object must have:
                - "number": integer (1-5)
                - "question": string (the actual question)
                - "type": one of "technical", "behavioral", "situational"
                - "difficulty": one of "easy", "medium", "hard"
                - "timeLimitSeconds": integer (60-180)
                
                Mix of question types: 3 technical, 1 behavioral, 1 situational.
                Questions should be specific to the candidate's skills shown in resume.
                """;

        String userPrompt = String.format("""
                Domain: %s
                
                Resume:
                %s
                
                Generate 5 targeted interview questions for this candidate.
                """, domain, resumeText != null ? resumeText.substring(0, Math.min(3000, resumeText.length())) : "No resume provided");

        String response = aiService.complete(systemPrompt, userPrompt);

        try {
            // Clean JSON if wrapped in markdown
            String json = response.trim();
            if (json.contains("```")) {
                json = json.replaceAll("```json", "").replaceAll("```", "").trim();
            }
            List<QuestionDTO> questions = mapper.readValue(json, new TypeReference<>() {});
            log.info("Generated {} questions", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("Failed to parse questions JSON: {}", response, e);
            return getDefaultQuestions(domain);
        }
    }

    // ── Submit Answer & Get Feedback ─────────────────────────────────

    @Transactional
    public FeedbackResponse submitAnswer(SubmitAnswerRequest req) {
        InterviewSession session = sessionRepo.findById(req.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + req.getSessionId()));

        // Get feedback from AI
        FeedbackData feedbackData = evaluateAnswer(req.getAnswer(),
                "Q" + req.getQuestionNumber(), session.getDomain());

        // Save QA record
        QuestionAnswer qa = QuestionAnswer.builder()
                .session(session)
                .answer(req.getAnswer())
                .feedback(feedbackData.feedback())
                .modelAnswer(feedbackData.modelAnswer())
                .score(feedbackData.score())
                .questionNumber(req.getQuestionNumber())
                .timeTakenSeconds(req.getTimeTakenSeconds())
                .build();
        qaRepo.save(qa);

        return FeedbackResponse.builder()
                .questionNumber(req.getQuestionNumber())
                .userAnswer(req.getAnswer())
                .feedback(feedbackData.feedback())
                .modelAnswer(feedbackData.modelAnswer())
                .score(feedbackData.score())
                .improvements(feedbackData.improvements())
                .positives(feedbackData.positives())
                .build();
    }

    private record FeedbackData(String feedback, String modelAnswer, int score,
                                List<String> improvements, List<String> positives) {}

    private FeedbackData evaluateAnswer(String answer, String questionHint, String domain) {
        // Smart scoring: penalise empty/short/gibberish answers
        boolean isEmpty   = answer == null || answer.trim().isEmpty() || answer.equals("(No answer provided)") || answer.equals("(Skipped)");
        boolean isShort   = !isEmpty && answer.trim().length() < 30;
        boolean isGarbage = !isEmpty && !isShort && isGibberish(answer);

        String systemPrompt = """
                You are a strict but fair technical interviewer evaluating an answer.
                Return ONLY valid JSON with these fields:
                - "feedback": string (detailed constructive feedback, 2-3 sentences)
                - "modelAnswer": string (what an ideal answer would cover, 2-4 sentences)
                - "score": integer 0-100
                - "improvements": array of 2-3 string suggestions
                - "positives": array of 1-2 string strengths in the answer
                
                Be specific, technical, and actionable in feedback.
                """;

        String userPrompt = String.format("""
                Domain: %s
                Candidate's answer: %s
                
                Evaluate this answer comprehensively.
                """, domain, answer != null ? answer : "(No answer provided)");

        String response = aiService.complete(systemPrompt, userPrompt);

        try {
            String json = response.trim();
            if (json.contains("```")) {
                json = json.replaceAll("```json", "").replaceAll("```", "").trim();
            }
            var node = mapper.readTree(json);
            List<String> improvements = mapper.convertValue(node.get("improvements"), new TypeReference<>() {});
            List<String> positives = mapper.convertValue(node.get("positives"), new TypeReference<>() {});

            int score = node.path("score").asInt(50);
            // Override score for clearly bad answers
            if (isEmpty)   score = 5  + random.nextInt(10);
            else if (isShort)   score = Math.min(score, 35);
            else if (isGarbage) score = Math.min(score, 20);

            return new FeedbackData(
                    node.path("feedback").asText(),
                    node.path("modelAnswer").asText(),
                    score,
                    improvements,
                    positives
            );
        } catch (Exception e) {
            log.error("Failed to parse feedback: {}", response, e);
            int fallback = isEmpty ? 5 : isShort ? 25 : 50;
            return new FeedbackData(
                    "Your answer showed some understanding of the topic.",
                    "A strong answer would cover key concepts with specific examples.",
                    fallback,
                    List.of("Add more specific examples", "Structure your answer clearly"),
                    List.of("Showed basic understanding")
            );
        }
    }

    private boolean isGibberish(String text) {
        if (text == null || text.length() < 5) return true;
        // Check ratio of consonant clusters (gibberish tends to have high consonant density)
        String lower = text.toLowerCase().replaceAll("[^a-z]", "");
        if (lower.length() < 4) return true;
        int vowels = 0;
        for (char c : lower.toCharArray()) {
            if ("aeiou".indexOf(c) >= 0) vowels++;
        }
        double vowelRatio = (double) vowels / lower.length();
        // Real English has ~38% vowels; gibberish is usually <15%
        return vowelRatio < 0.15;
    }

    private final java.util.Random random = new java.util.Random();

    // ── Complete Session & Build Dashboard ───────────────────────────

    @Transactional
    public DashboardResponse completeSession(String sessionId) {
        InterviewSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<QuestionAnswer> qas = qaRepo.findBySessionIdOrderByQuestionNumber(sessionId);

        // Compute scores
        double avgScore = qas.stream().mapToInt(QuestionAnswer::getScore).average().orElse(50);
        double techScore = qas.stream().filter(qa -> "technical".equals(qa.getQuestionType()))
                .mapToInt(QuestionAnswer::getScore).average().orElse(avgScore);
        double commScore = Math.min(100, avgScore + (new Random().nextInt(10) - 5));
        double confScore = Math.min(100, avgScore + (new Random().nextInt(10) - 5));
        double psScore   = Math.min(100, avgScore + (new Random().nextInt(10) - 5));

        // Get overall AI analysis
        OverallAnalysis analysis = getOverallAnalysis(session, qas);

        // Update session
        session.setCompleted(true);
        session.setCompletedAt(LocalDateTime.now());
        session.setOverallScore(avgScore);
        session.setTechnicalScore(techScore);
        session.setCommunicationScore(commScore);
        session.setConfidenceScore(confScore);
        session.setProblemSolvingScore(psScore);
        session.setWeakAreas(String.join(",", analysis.weakAreas()));
        session.setStrengths(String.join(",", analysis.strengths()));
        session.setOverallFeedback(analysis.overallFeedback());
        sessionRepo.save(session);

        // Build response
        List<FeedbackResponse> feedbacks = qas.stream().map(qa -> FeedbackResponse.builder()
                .questionNumber(qa.getQuestionNumber())
                .question(qa.getQuestion())
                .userAnswer(qa.getAnswer())
                .feedback(qa.getFeedback())
                .modelAnswer(qa.getModelAnswer())
                .score(qa.getScore())
                .build()).collect(Collectors.toList());

        return DashboardResponse.builder()
                .sessionId(sessionId)
                .candidateName(session.getCandidateName())
                .domain(session.getDomain())
                .overallScore(round(avgScore))
                .technicalScore(round(techScore))
                .communicationScore(round(commScore))
                .confidenceScore(round(confScore))
                .problemSolvingScore(round(psScore))
                .overallFeedback(analysis.overallFeedback())
                .weakAreas(analysis.weakAreas())
                .strengths(analysis.strengths())
                .questionFeedbacks(feedbacks)
                .completedAt(session.getCompletedAt())
                .build();
    }

    private record OverallAnalysis(String overallFeedback, List<String> weakAreas, List<String> strengths) {}

    private OverallAnalysis getOverallAnalysis(InterviewSession session, List<QuestionAnswer> qas) {
        String allFeedback = qas.stream()
                .map(qa -> "Q" + qa.getQuestionNumber() + " (score:" + qa.getScore() + "): " + qa.getFeedback())
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
                You are a senior hiring manager writing a final interview evaluation.
                Return ONLY valid JSON with:
                - "overallFeedback": string (3-4 sentences overall assessment)
                - "weakAreas": array of 3 specific skill/topic strings to improve
                - "strengths": array of 3 specific strong points demonstrated
                """;

        String userPrompt = String.format("""
                Candidate: %s | Domain: %s
                Question feedbacks summary:
                %s
                
                Provide overall evaluation.
                """, session.getCandidateName(), session.getDomain(), allFeedback);

        try {
            String response = aiService.complete(systemPrompt, userPrompt);
            String json = response.trim().replaceAll("```json", "").replaceAll("```", "").trim();
            var node = mapper.readTree(json);
            List<String> weakAreas = mapper.convertValue(node.get("weakAreas"), new TypeReference<>() {});
            List<String> strengths = mapper.convertValue(node.get("strengths"), new TypeReference<>() {});
            return new OverallAnalysis(node.path("overallFeedback").asText(), weakAreas, strengths);
        } catch (Exception e) {
            log.error("Failed overall analysis", e);
            return new OverallAnalysis(
                    "Candidate demonstrated reasonable understanding across topics. Focus on deepening technical knowledge.",
                    List.of("System Design", "Problem Solving Speed", "Communication Clarity"),
                    List.of("Domain Knowledge", "Enthusiasm", "Basic Concepts")
            );
        }
    }

    // ── Get Session Dashboard ────────────────────────────────────────

    public DashboardResponse getDashboard(String sessionId) {
        InterviewSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        List<QuestionAnswer> qas = qaRepo.findBySessionIdOrderByQuestionNumber(sessionId);

        List<FeedbackResponse> feedbacks = qas.stream().map(qa -> FeedbackResponse.builder()
                .questionNumber(qa.getQuestionNumber())
                .question(qa.getQuestion())
                .userAnswer(qa.getAnswer())
                .feedback(qa.getFeedback())
                .modelAnswer(qa.getModelAnswer())
                .score(qa.getScore())
                .build()).collect(Collectors.toList());

        List<String> weakAreas = session.getWeakAreas() != null ?
                Arrays.asList(session.getWeakAreas().split(",")) : List.of();
        List<String> strengths = session.getStrengths() != null ?
                Arrays.asList(session.getStrengths().split(",")) : List.of();

        return DashboardResponse.builder()
                .sessionId(sessionId)
                .candidateName(session.getCandidateName())
                .domain(session.getDomain())
                .overallScore(session.getOverallScore())
                .technicalScore(session.getTechnicalScore())
                .communicationScore(session.getCommunicationScore())
                .confidenceScore(session.getConfidenceScore())
                .problemSolvingScore(session.getProblemSolvingScore())
                .overallFeedback(session.getOverallFeedback())
                .weakAreas(weakAreas)
                .strengths(strengths)
                .questionFeedbacks(feedbacks)
                .completedAt(session.getCompletedAt())
                .build();
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }

    // ── Default Questions Fallback ───────────────────────────────────

    private List<QuestionDTO> getDefaultQuestions(String domain) {
        return List.of(
                QuestionDTO.builder().number(1).question("Tell me about your most challenging project and how you handled it.").type("behavioral").difficulty("medium").timeLimitSeconds(120).build(),
                QuestionDTO.builder().number(2).question("Explain the difference between processes and threads.").type("technical").difficulty("easy").timeLimitSeconds(90).build(),
                QuestionDTO.builder().number(3).question("How would you design a URL shortening service?").type("technical").difficulty("hard").timeLimitSeconds(180).build(),
                QuestionDTO.builder().number(4).question("Describe a time you had to learn something quickly under pressure.").type("situational").difficulty("medium").timeLimitSeconds(120).build(),
                QuestionDTO.builder().number(5).question("What is your approach to debugging a production issue at 2am?").type("technical").difficulty("medium").timeLimitSeconds(120).build()
        );
    }
}
