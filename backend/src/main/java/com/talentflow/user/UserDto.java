package com.talentflow.user;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String linkedinUrl,
        String photoUrl,
        Role role,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt) {
}
