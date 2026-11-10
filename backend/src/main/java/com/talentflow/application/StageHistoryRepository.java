package com.talentflow.application;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageHistoryRepository extends JpaRepository<StageHistory, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<StageHistory> findByApplicationIdOrderByChangedAtDescIdDesc(Long applicationId);
}
