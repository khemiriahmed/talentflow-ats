package com.talentflow.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record TagsRequest(@NotNull @Size(max = 10) Set<@NotBlank @Size(max = 50) String> tags) {
}
