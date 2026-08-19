package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Employee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(
        @NotNull Long departmentId,
        @NotNull Long positionId,
        Long companyUserId,
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String phone,
        @Size(max = 100) String email,
        @NotNull LocalDate hireDate
) {
    public Employee toDomain() {
        return Employee.builder()
                .departmentId(departmentId)
                .positionId(positionId)
                .companyUserId(companyUserId)
                .name(name)
                .phone(phone)
                .email(email)
                .hireDate(hireDate)
                .build();
    }
}
