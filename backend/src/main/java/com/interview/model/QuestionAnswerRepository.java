package com.interview.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findBySessionIdOrderByQuestionNumber(String sessionId);
}