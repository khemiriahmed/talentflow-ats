package com.talentflow.notification;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String type,
        String message,
        String referenceType,
        Long referenceId,
        boolean read,
        Instant createdAt) {
}
