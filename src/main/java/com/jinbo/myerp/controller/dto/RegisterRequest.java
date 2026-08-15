package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 100) @Schema(example = "staff@myerp.com") String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
                message = "비밀번호는 8~100자이며 영문 대문자, 소문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
        )
        @Schema(description = "8~100자, 영문 대/소문자·숫자·특수문자 각 1자 이상 포함", example = "Password123!")
        String password,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Schema(description = "OWNER는 대표/관리자, STAFF는 일반 직원") UserRole role
) {
}
