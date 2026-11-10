package com.talentflow.interview;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @EntityGraph(attributePaths = {"author", "interview"})
    Optional<Feedback> findByInterviewId(Long interviewId);
}
