package com.talentflow.application;

import java.time.Instant;

public record NoteDto(
        Long id,
        String content,
        Long authorId,
        String authorName,
        Instant createdAt) {
}
