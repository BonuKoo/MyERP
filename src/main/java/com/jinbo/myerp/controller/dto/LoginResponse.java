package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        String name,
        UserRole role
) {
}
