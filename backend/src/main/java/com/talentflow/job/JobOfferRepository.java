package com.talentflow.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobOfferRepository
        extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
}
