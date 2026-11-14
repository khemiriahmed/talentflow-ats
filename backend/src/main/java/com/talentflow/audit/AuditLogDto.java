package com.talentflow.audit;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        String action,
        String entityType,
        Long entityId,
        Long actorId,
        String actorName,
        Instant createdAt) {
}
