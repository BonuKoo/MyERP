package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.SalarySetting;

import java.math.BigDecimal;

public record SalarySettingResponse(
        Long id,
        BigDecimal dailyWage
) {
    public static SalarySettingResponse from(SalarySetting setting) {
        return new SalarySettingResponse(setting.getId(), setting.getDailyWage());
    }
}
