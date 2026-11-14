package com.talentflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mise à jour du profil de l'utilisateur connecté (RF-04). */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 30) String phone,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 500) String photoUrl) {
}
