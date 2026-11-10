package com.talentflow.job;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull JobStatus status) {
}
