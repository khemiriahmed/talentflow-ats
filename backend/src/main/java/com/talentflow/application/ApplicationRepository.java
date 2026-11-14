package com.talentflow.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    boolean existsByJobOfferIdAndCandidateId(Long jobOfferId, Long candidateId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findByJobOfferId(Long jobOfferId, Pageable pageable);

    List<Application> findByJobOfferIdAndCurrentStage(Long jobOfferId, Stage stage);

    List<Application> findAllByCandidateId(Long candidateId);

    long countByJobOfferId(Long jobOfferId);

    /** Charge l'offre et le candidat avec la page (évite le N+1 au mapping). */
    @EntityGraph(attributePaths = {"jobOffer", "candidate"})
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
