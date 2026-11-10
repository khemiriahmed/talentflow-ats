package com.talentflow.interview;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record InterviewRequest(
        @NotNull @Future Instant startAt,
        @NotNull @Min(15) @Max(480) Integer durationMin,
        @NotNull InterviewMode mode,
        @Size(max = 500) String locationOrLink,
        @NotEmpty Set<Long> participantIds) {
}
