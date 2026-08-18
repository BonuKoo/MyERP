package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Department;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String name,
        boolean active,
        LocalDateTime createdAt
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(), department.getName(), department.isActive(), department.getCreatedAt());
    }
}
