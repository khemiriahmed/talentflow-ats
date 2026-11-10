package com.talentflow.application;

import java.time.Instant;

public record StageHistoryDto(
        Long id,
        Stage fromStage,
        Stage toStage,
        String reason,
        Long changedById,
        String changedByName,
        Instant changedAt) {
}
