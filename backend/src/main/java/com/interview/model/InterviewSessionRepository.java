package com.interview.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, String> {
    List<InterviewSession> findByDomainOrderByCreatedAtDesc(String domain);
    List<InterviewSession> findTop10ByOrderByCreatedAtDesc();
}