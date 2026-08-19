package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.SalarySetting;
import com.jinbo.myerp.mapper.SalarySettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SalarySettingService {

    private static final BigDecimal DEFAULT_DAILY_WAGE = new BigDecimal("100000");

    private final SalarySettingMapper salarySettingMapper;

    @Transactional
    public SalarySetting get() {
        return salarySettingMapper.find().orElseGet(this::createDefault);
    }

    @Transactional
    public SalarySetting updateDailyWage(BigDecimal newDailyWage) {
        SalarySetting setting = get();
        setting.setDailyWage(newDailyWage);
        setting.setUpdatedAt(LocalDateTime.now());
        salarySettingMapper.update(setting);
        return setting;
    }

    private SalarySetting createDefault() {
        SalarySetting setting = SalarySetting.builder()
                .dailyWage(DEFAULT_DAILY_WAGE).updatedAt(LocalDateTime.now()).build();
        salarySettingMapper.insert(setting);
        return setting;
    }
}
