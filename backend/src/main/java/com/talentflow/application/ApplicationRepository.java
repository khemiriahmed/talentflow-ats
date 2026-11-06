package com.talentflow.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobOfferIdAndCandidateId(Long jobOfferId, Long candidateId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findByJobOfferId(Long jobOfferId, Pageable pageable);

    List<Application> findByJobOfferIdAndCurrentStage(Long jobOfferId, Stage stage);

    long countByJobOfferId(Long jobOfferId);
}
