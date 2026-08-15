package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "이후 API 호출 시 `Authorization: Bearer {accessToken}` 헤더로 전달한다.")
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        Long userId,
        String email,
        String name,
        UserRole role
) {
}
