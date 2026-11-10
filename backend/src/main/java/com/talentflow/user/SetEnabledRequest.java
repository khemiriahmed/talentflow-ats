package com.talentflow.user;

import jakarta.validation.constraints.NotNull;

public record SetEnabledRequest(@NotNull Boolean enabled) {
}
