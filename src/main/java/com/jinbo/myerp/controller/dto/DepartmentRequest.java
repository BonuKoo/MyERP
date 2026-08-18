package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 50) String name
) {
    public Department toDomain() {
        return Department.builder().name(name).build();
    }
}
