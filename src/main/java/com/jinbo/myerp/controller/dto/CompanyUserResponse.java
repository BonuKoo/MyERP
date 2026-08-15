package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;

import java.time.LocalDateTime;

public record CompanyUserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static CompanyUserResponse from(CompanyUser user) {
        return new CompanyUserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
