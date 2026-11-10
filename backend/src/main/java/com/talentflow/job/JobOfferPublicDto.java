package com.talentflow.job;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Vue publique : aucune information interne (statut, créateur, manager). */
public record JobOfferPublicDto(
        Long id,
        String title,
        String description,
        String department,
        String location,
        ContractType contractType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate deadline,
        Instant createdAt) {
}
