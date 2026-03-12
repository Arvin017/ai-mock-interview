package com.interview.controller;

import com.interview.dto.InterviewDTOs.*;
import com.interview.service.InterviewService;
import com.interview.service.ResumeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "*")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    @Autowired private InterviewService interviewService;
    @Autowired private ResumeParserService resumeParser;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    /**
     * POST /api/interview/upload-resume
     * Upload and parse resume file, return extracted text
     */
    @PostMapping("/upload-resume")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received resume: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
            String text = resumeParser.extractText(file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "text", text,
                    "filename", file.getOriginalFilename(),
                    "charCount", text.length()
            ));
        } catch (Exception e) {
            log.error("Resume upload failed", e);
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder().error("PARSE_ERROR").message(e.getMessage()).build()
            );
        }
    }

    /**
     * POST /api/interview/start
     * Start a new interview session, returns questions
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request) {
        try {
            log.info("Starting session for {}", request.getCandidateName());
            SessionResponse session = interviewService.startSession(request);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Failed to start session", e);
            return ResponseEntity.internalServerError().body(
                    ErrorResponse.builder().error("SESSION_ERROR").message(e.getMessage()).build()
            );
        }
    }

    /**
     * POST /api/interview/submit-answer
     * Submit an answer and get AI feedback in real-time via WebSocket
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<?> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        try {
            log.info("Evaluating answer for session {} Q{}", request.getSessionId(), request.getQuestionNumber());

            // Send "evaluating" status via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/feedback/" + request.getSessionId(),
                    Map.of("status", "evaluating", "questionNumber", request.getQuestionNumber())
            );

            FeedbackResponse feedback = interviewService.submitAnswer(request);

            // Send feedback via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/feedback/" + request.getSessionId(),
                    Map.of("status", "complete", "feedback", feedback)
            );

            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("Answer submission failed", e);
            return ResponseEntity.internalServerError().body(
                    ErrorResponse.builder().error("EVAL_ERROR").message(e.getMessage()).build()
            );
        }
    }

    /**
     * POST /api/interview/complete/{sessionId}
     * Complete the interview and get full dashboard
     */
    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<?> completeSession(@PathVariable String sessionId) {
        try {
            log.info("Completing session {}", sessionId);
            DashboardResponse dashboard = interviewService.completeSession(sessionId);

            // Broadcast completion via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId,
                    Map.of("status", "completed", "sessionId", sessionId)
            );

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Session completion failed", e);
            return ResponseEntity.internalServerError().body(
                    ErrorResponse.builder().error("COMPLETE_ERROR").message(e.getMessage()).build()
            );
        }
    }

    /**
     * GET /api/interview/dashboard/{sessionId}
     * Get session results dashboard
     */
    @GetMapping("/dashboard/{sessionId}")
    public ResponseEntity<?> getDashboard(@PathVariable String sessionId) {
        try {
            return ResponseEntity.ok(interviewService.getDashboard(sessionId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/interview/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "AI Mock Interview API"));
    }
}
