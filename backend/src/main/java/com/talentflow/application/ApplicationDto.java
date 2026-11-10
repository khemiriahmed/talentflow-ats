package com.talentflow.application;

import java.time.Instant;

public record ApplicationDto(
        Long id,
        Long jobOfferId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String coverLetter,
        Stage currentStage,
        String source,
        Instant createdAt) {
}
