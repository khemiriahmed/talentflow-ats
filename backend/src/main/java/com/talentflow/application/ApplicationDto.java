package com.talentflow.application;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Set;

/** "tags" est interne : absent (null) dans les réponses destinées au candidat. */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        Instant createdAt,
        Set<String> tags) {
}
