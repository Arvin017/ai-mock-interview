package com.interview.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class InterviewDTOs {

    // ── Request DTOs ──────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StartSessionRequest {
        private String candidateName;
        private String domain; // swe, data, pm, devops, frontend
        private String resumeText;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmitAnswerRequest {
        private String sessionId;
        private Integer questionNumber;
        private String answer;
        private Long timeTakenSeconds;
    }

    // ── Response DTOs ─────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SessionResponse {
        private String sessionId;
        private String candidateName;
        private String domain;
        private List<QuestionDTO> questions;
        private LocalDateTime startedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionDTO {
        private Integer number;
        private String question;
        private String type;       // technical / behavioral / situational
        private String difficulty; // easy / medium / hard
        private Integer timeLimitSeconds;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FeedbackResponse {
        private Integer questionNumber;
        private String question;
        private String userAnswer;
        private String feedback;
        private String modelAnswer;
        private Integer score;
        private List<String> improvements;
        private List<String> positives;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardResponse {
        private String sessionId;
        private String candidateName;
        private String domain;
        private Double overallScore;
        private Double technicalScore;
        private Double communicationScore;
        private Double confidenceScore;
        private Double problemSolvingScore;
        private String overallFeedback;
        private List<String> weakAreas;
        private List<String> strengths;
        private List<FeedbackResponse> questionFeedbacks;
        private LocalDateTime completedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorResponse {
        private String error;
        private String message;
    }
}
