package com.talentflow.interview;

import java.time.Instant;
import java.util.List;

public record InterviewDto(
        Long id,
        Long applicationId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        Instant startAt,
        int durationMin,
        InterviewMode mode,
        String locationOrLink,
        InterviewStatus status,
        List<ParticipantDto> participants,
        Instant createdAt) {
}
