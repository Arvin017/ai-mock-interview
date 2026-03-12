package com.interview.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    private Integer score; // 0-100

    private String questionType; // technical, behavioral, situational

    private Integer questionNumber;

    private Long timeTakenSeconds;
}
