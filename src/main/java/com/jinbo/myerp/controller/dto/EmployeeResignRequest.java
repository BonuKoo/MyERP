package com.jinbo.myerp.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeResignRequest(
        @NotNull LocalDate resignationDate
) {
}
