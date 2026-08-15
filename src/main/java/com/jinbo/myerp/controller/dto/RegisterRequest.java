package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Schema(example = "staff@myerp.com") String email,
        @NotBlank @Size(min = 8, max = 100) @Schema(description = "8자 이상", example = "password123") String password,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Schema(description = "OWNER는 대표/관리자, STAFF는 일반 직원") UserRole role
) {
}
