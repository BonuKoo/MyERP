package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.SalarySetting;
import com.jinbo.myerp.mapper.SalarySettingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalarySettingServiceTest {

    @Mock
    private SalarySettingMapper salarySettingMapper;

    @InjectMocks
    private SalarySettingService salarySettingService;

    @Test
    void get_whenExists_returnsIt() {
        SalarySetting existing = SalarySetting.builder().id(1L).dailyWage(new BigDecimal("100000")).build();
        given(salarySettingMapper.find()).willReturn(Optional.of(existing));

        SalarySetting result = salarySettingService.get();

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void get_whenMissing_createsDefault() {
        given(salarySettingMapper.find()).willReturn(Optional.empty());

        SalarySetting result = salarySettingService.get();

        assertThat(result.getDailyWage()).isEqualByComparingTo("100000");
        verify(salarySettingMapper).insert(result);
    }

    @Test
    void updateDailyWage_appliesChange() {
        SalarySetting existing = SalarySetting.builder().id(1L).dailyWage(new BigDecimal("100000")).build();
        given(salarySettingMapper.find()).willReturn(Optional.of(existing));

        SalarySetting result = salarySettingService.updateDailyWage(new BigDecimal("120000"));

        assertThat(result.getDailyWage()).isEqualByComparingTo("120000");
        verify(salarySettingMapper).update(existing);
    }
}
