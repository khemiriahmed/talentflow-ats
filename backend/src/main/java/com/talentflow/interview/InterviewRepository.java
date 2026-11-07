package com.talentflow.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByStartAtBetweenOrderByStartAt(Instant from, Instant to);
}
