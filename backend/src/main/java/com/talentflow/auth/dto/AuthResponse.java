package com.talentflow.auth.dto;

import com.talentflow.user.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user) {
}
