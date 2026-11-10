package com.talentflow.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StageChangeRequest(
        @NotNull Stage stage,
        @Size(max = 2000) String reason) {
}
