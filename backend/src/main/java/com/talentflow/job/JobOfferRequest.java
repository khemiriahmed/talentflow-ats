package com.talentflow.job;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobOfferRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 100) String department,
        @Size(max = 100) String location,
        @NotNull ContractType contractType,
        @PositiveOrZero BigDecimal salaryMin,
        @PositiveOrZero BigDecimal salaryMax,
        @FutureOrPresent LocalDate deadline,
        Long managerId) {
}
