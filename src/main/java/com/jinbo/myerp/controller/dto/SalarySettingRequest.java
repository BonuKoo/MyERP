package com.jinbo.myerp.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SalarySettingRequest(
        @NotNull @DecimalMin("0.01") BigDecimal dailyWage
) {
}
