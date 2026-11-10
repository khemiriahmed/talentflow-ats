package com.talentflow.job;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Vue interne (RH / Manager / Admin). */
public record JobOfferDto(
        Long id,
        String title,
        String description,
        String department,
        String location,
        ContractType contractType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        JobStatus status,
        LocalDate deadline,
        Long createdById,
        Long managerId,
        Instant createdAt) {
}
