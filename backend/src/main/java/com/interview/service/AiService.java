package com.interview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Collections;

/**
 * FREE Mock AI Service — No API key required!
 * Returns realistic interview questions and feedback using smart templates.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final Random random = new Random();

    private static final Map<String, List<String[]>> QUESTION_BANK = new HashMap<>();

    static {
        QUESTION_BANK.put("swe", List.of(
            new String[]{"Explain the difference between a stack and a queue. When would you use each?", "technical", "easy"},
            new String[]{"What is the time complexity of quicksort in the best, average, and worst cases?", "technical", "medium"},
            new String[]{"Design a system like Twitter — walk me through your high-level architecture.", "technical", "hard"},
            new String[]{"Tell me about a time you had to debug a critical production issue under pressure.", "behavioral", "medium"},
            new String[]{"How would you handle a disagreement with a senior engineer about a technical decision?", "situational", "medium"},
            new String[]{"What is the difference between REST and GraphQL? When would you prefer one over the other?", "technical", "medium"},
            new String[]{"Explain SOLID principles and give an example of each.", "technical", "hard"},
            new String[]{"Describe a project where you had to learn a new technology quickly.", "behavioral", "easy"}
        ));
        QUESTION_BANK.put("data", List.of(
            new String[]{"What is the difference between supervised and unsupervised learning?", "technical", "easy"},
            new String[]{"How would you handle class imbalance in a classification problem?", "technical", "medium"},
            new String[]{"Design an ML pipeline for predicting customer churn for an e-commerce platform.", "technical", "hard"},
            new String[]{"Tell me about the most challenging data cleaning problem you have faced.", "behavioral", "medium"},
            new String[]{"If your model has 99% accuracy but your stakeholder is not happy, what could be wrong?", "situational", "medium"},
            new String[]{"Explain the bias-variance tradeoff with an example.", "technical", "medium"},
            new String[]{"What evaluation metrics would you use for a fraud detection model and why?", "technical", "hard"},
            new String[]{"How do you explain a complex model result to a non-technical business stakeholder?", "behavioral", "easy"}
        ));
        QUESTION_BANK.put("frontend", List.of(
            new String[]{"Explain the difference between == and === in JavaScript.", "technical", "easy"},
            new String[]{"What is the Virtual DOM in React and how does it improve performance?", "technical", "medium"},
            new String[]{"How would you optimize a React app that has performance issues?", "technical", "hard"},
            new String[]{"Describe a project where you implemented a complex UI feature from scratch.", "behavioral", "medium"},
            new String[]{"How do you ensure your web application is accessible (a11y)?", "technical", "medium"},
            new String[]{"What is the difference between localStorage, sessionStorage, and cookies?", "technical", "easy"},
            new String[]{"Explain CSS specificity and how conflicts are resolved.", "technical", "easy"},
            new String[]{"How would you handle state management in a large React application?", "technical", "hard"}
        ));
        QUESTION_BANK.put("devops", List.of(
            new String[]{"What is the difference between Docker and a virtual machine?", "technical", "easy"},
            new String[]{"Explain the concept of Infrastructure as Code and name some tools.", "technical", "medium"},
            new String[]{"Design a CI/CD pipeline for a microservices application on Kubernetes.", "technical", "hard"},
            new String[]{"Tell me about a time you had to respond to a production outage.", "behavioral", "medium"},
            new String[]{"How would you approach reducing deployment downtime to near zero?", "technical", "hard"},
            new String[]{"What are the key differences between horizontal and vertical scaling?", "technical", "easy"},
            new String[]{"Explain blue-green deployment vs canary deployment.", "technical", "medium"},
            new String[]{"How do you monitor the health of a distributed system?", "technical", "medium"}
        ));
        QUESTION_BANK.put("pm", List.of(
            new String[]{"How do you prioritize features when you have limited engineering resources?", "technical", "medium"},
            new String[]{"Walk me through how you define success metrics for a new feature launch.", "technical", "medium"},
            new String[]{"Tell me about a product decision you made with incomplete data.", "behavioral", "hard"},
            new String[]{"How would you approach redesigning the onboarding flow of a mobile app?", "situational", "medium"},
            new String[]{"Describe how you handle conflicting priorities from different stakeholders.", "situational", "hard"},
            new String[]{"What is the difference between a product roadmap and a product backlog?", "technical", "easy"},
            new String[]{"How do you validate a product idea before investing engineering time?", "technical", "medium"},
            new String[]{"Tell me about a time a product you worked on failed. What did you learn?", "behavioral", "medium"}
        ));
        QUESTION_BANK.put("backend", List.of(
            new String[]{"What is the difference between SQL and NoSQL databases? When would you use each?", "technical", "easy"},
            new String[]{"Explain database indexing and when it can hurt performance.", "technical", "medium"},
            new String[]{"Design a rate limiter for a public API that handles 1 million requests per day.", "technical", "hard"},
            new String[]{"How do you handle database migrations in a production system with zero downtime?", "technical", "hard"},
            new String[]{"What are the main differences between monolithic and microservices architecture?", "technical", "medium"},
            new String[]{"Explain JWT authentication — how it works and its security considerations.", "technical", "medium"},
            new String[]{"Tell me about a backend system you built that had to scale to handle high traffic.", "behavioral", "medium"},
            new String[]{"How would you design an API for a food delivery app?", "technical", "hard"}
        ));
    }

    private static final List<String[]> FEEDBACK_TEMPLATES = List.of(
        new String[]{
            "Your answer demonstrates a solid understanding of the core concepts. You covered the key points clearly, though adding a concrete real-world example would have strengthened your response significantly.",
            "An ideal answer would define the concept precisely, compare it to alternatives, give a real-world use case, mention trade-offs, and conclude with when you would use it in practice.",
            "Add specific numbers or metrics to support your claims",
            "Use the STAR method to structure your examples better",
            "Clear explanation of core concepts"
        },
        new String[]{
            "Good attempt! You showed familiarity with the topic but the answer could be more structured. Starting with a brief definition before diving into details would make it easier to follow.",
            "A strong answer here would open with a crisp one-line definition, then walk through 2-3 key aspects with examples, address edge cases, and finish with a real project application.",
            "Structure your answer: Definition, How it works, Example, Trade-offs",
            "Mention specific tools or technologies you have actually used",
            "Demonstrated hands-on familiarity with the topic"
        },
        new String[]{
            "You touched on the right areas but the depth was somewhat surface-level. For a senior-level question, interviewers expect you to discuss trade-offs and edge cases.",
            "The ideal response would cover the fundamentals, discuss at least two alternative approaches with pros and cons, reference a specific scenario from experience, and demonstrate awareness of industry best practices.",
            "Discuss trade-offs — every technical decision has pros and cons",
            "Reference your actual project experience to make the answer concrete",
            "Good intuition about the right approach"
        },
        new String[]{
            "Excellent response! You demonstrated deep understanding and gave a well-structured answer with a relevant example. The mention of trade-offs showed strong engineering maturity.",
            "Your answer was close to ideal. To make it perfect, you could quantify the impact such as reduced latency by 40 percent and mention how you would monitor the solution post-deployment.",
            "Quantify results where possible such as reduced X by Y percent",
            "Mention monitoring and observability for production systems",
            "Outstanding structure and depth of knowledge"
        },
        new String[]{
            "The answer was on the right track but lacked specifics. Vague answers without concrete follow-up can leave interviewers unconvinced of your real experience.",
            "A complete answer would acknowledge the context-dependency, then walk through 2-3 specific scenarios with clear reasoning for each choice, backed by your own experience.",
            "Always follow general statements with concrete criteria and examples",
            "Prepare 1-2 specific stories from your projects in advance",
            "Showed awareness of contextual factors"
        }
    );

    private static final String[] OVERALL_FEEDBACK = {
        "The candidate showed good foundational knowledge across technical topics. Performance was consistent throughout the interview with particularly strong answers on conceptual questions. With more practice on system design and behavioral storytelling, this candidate would be very competitive.",
        "Strong performance overall. The candidate demonstrated clear thinking and structured responses. Technical depth was impressive, though communication could be more concise. Recommended to focus on quantifying past achievements and preparing STAR-format stories.",
        "The interview revealed solid understanding of core concepts with room to grow in advanced topics. The candidate was enthusiastic and showed genuine curiosity. Focusing on depth over breadth and practicing whiteboard-style explanations will significantly improve future performance.",
        "Candidate performed above average with notable strengths in problem-solving approach. The structured thinking was evident throughout. Main areas to work on are system design at scale and more concrete examples from past experience. Overall a promising profile."
    };

    private static final String[][] WEAK_STRENGTHS = {
        {"System Design at Scale", "Time Complexity Analysis", "Behavioral Storytelling using STAR method", "Domain Knowledge", "Problem-Solving Approach", "Communication Clarity"},
        {"Database Optimization", "Concurrency and Threading", "Edge Case Handling", "Technical Breadth", "Logical Reasoning", "Enthusiasm and Attitude"},
        {"Distributed Systems Concepts", "Code Quality and Testing", "Architecture Trade-offs", "Core CS Fundamentals", "Real-World Project Experience", "Structured Thinking"}
    };

    // ── Public API ───────────────────────────────────────────────────

    public String complete(String systemPrompt, String userPrompt) {
        log.info("MockAI: generating response");
        try { Thread.sleep(800 + random.nextInt(1200)); } catch (InterruptedException ignored) {}

        if (systemPrompt != null && systemPrompt.contains("interview questions")) {
            return generateQuestionsJson(userPrompt);
        } else if (systemPrompt != null && systemPrompt.contains("overall")) {
            return generateOverallAnalysisJson();
        }
        return generateFeedbackJson(userPrompt);
    }

    public void completeStreaming(String systemPrompt, String userPrompt, Consumer<String> chunkConsumer) {
        String fullResponse = complete(systemPrompt, userPrompt);
        for (String word : fullResponse.split(" ")) {
            chunkConsumer.accept(word + " ");
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
    }

    // ── Private generators ───────────────────────────────────────────

    private String generateQuestionsJson(String userPrompt) {
        String domain = "swe";
        String lower = userPrompt != null ? userPrompt.toLowerCase() : "";
        for (String d : QUESTION_BANK.keySet()) {
            if (lower.contains(d)) { domain = d; break; }
        }
        List<String[]> pool = new ArrayList<>(QUESTION_BANK.getOrDefault(domain, QUESTION_BANK.get("swe")));
        Collections.shuffle(pool, random);
        List<String[]> selected = pool.subList(0, Math.min(5, pool.size()));

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < selected.size(); i++) {
            String[] q = selected.get(i);
            int time = q[2].equals("easy") ? 90 : q[2].equals("medium") ? 120 : 150;
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"number\":%d,\"question\":\"%s\",\"type\":\"%s\",\"difficulty\":\"%s\",\"timeLimitSeconds\":%d}",
                i + 1, q[0], q[1], q[2], time));
        }
        return json.append("]").toString();
    }

    private String generateFeedbackJson(String userPrompt) {
        String[] t = FEEDBACK_TEMPLATES.get(random.nextInt(FEEDBACK_TEMPLATES.size()));
        int score = 55 + random.nextInt(35);
        if (userPrompt != null && userPrompt.length() > 100) score = Math.min(95, score + 10);
        return String.format(
            "{\"feedback\":\"%s\",\"modelAnswer\":\"%s\",\"score\":%d,\"improvements\":[\"%s\",\"%s\"],\"positives\":[\"%s\"]}",
            t[0], t[1], score, t[2], t[3], t[4]);
    }

    private String generateOverallAnalysisJson() {
        String fb = OVERALL_FEEDBACK[random.nextInt(OVERALL_FEEDBACK.length)];
        String[] ws = WEAK_STRENGTHS[random.nextInt(WEAK_STRENGTHS.length)];
        return String.format(
            "{\"overallFeedback\":\"%s\",\"weakAreas\":[\"%s\",\"%s\",\"%s\"],\"strengths\":[\"%s\",\"%s\",\"%s\"]}",
            fb, ws[0], ws[1], ws[2], ws[3], ws[4], ws[5]);
    }
}
