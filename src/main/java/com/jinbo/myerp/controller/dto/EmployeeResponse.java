package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Employee;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        Long departmentId,
        Long positionId,
        Long companyUserId,
        String name,
        String phone,
        String email,
        LocalDate hireDate,
        LocalDate resignationDate,
        boolean active
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(), employee.getDepartmentId(), employee.getPositionId(), employee.getCompanyUserId(),
                employee.getName(), employee.getPhone(), employee.getEmail(),
                employee.getHireDate(), employee.getResignationDate(), employee.isActive());
    }
}
