package com.interview.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String candidateName;

    @Column(length = 50)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private boolean completed;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "problem_solving_score")
    private Double problemSolvingScore;

    @Column(columnDefinition = "TEXT")
    private String weakAreas;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String overallFeedback;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuestionAnswer> questionAnswers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
