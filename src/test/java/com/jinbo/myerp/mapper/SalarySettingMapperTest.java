package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.SalarySetting;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class SalarySettingMapperTest {

    @Autowired
    private SalarySettingMapper salarySettingMapper;

    @Test
    void find_whenEmpty_returnsEmpty() {
        assertThat(salarySettingMapper.find()).isEmpty();
    }

    @Test
    void insertAndFind() {
        SalarySetting setting = SalarySetting.builder()
                .dailyWage(new BigDecimal("100000")).updatedAt(LocalDateTime.now()).build();

        salarySettingMapper.insert(setting);

        assertThat(setting.getId()).isNotNull();
        Optional<SalarySetting> found = salarySettingMapper.find();
        assertThat(found).isPresent();
        assertThat(found.get().getDailyWage()).isEqualByComparingTo("100000");
    }

    @Test
    void update_changesDailyWage() {
        SalarySetting setting = SalarySetting.builder()
                .dailyWage(new BigDecimal("100000")).updatedAt(LocalDateTime.now()).build();
        salarySettingMapper.insert(setting);

        setting.setDailyWage(new BigDecimal("120000"));
        salarySettingMapper.update(setting);

        SalarySetting updated = salarySettingMapper.find().orElseThrow();
        assertThat(updated.getDailyWage()).isEqualByComparingTo("120000");
    }
}
