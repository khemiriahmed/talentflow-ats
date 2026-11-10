package com.talentflow.interview;

import java.time.Instant;

public record FeedbackDto(
        Long id,
        Long interviewId,
        Long authorId,
        String authorName,
        Integer rating,
        String strengths,
        String weaknesses,
        Recommendation recommendation,
        Instant createdAt) {
}
