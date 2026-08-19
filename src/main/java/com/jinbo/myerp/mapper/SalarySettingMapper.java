package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.SalarySetting;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface SalarySettingMapper {

    Optional<SalarySetting> find();

    void insert(SalarySetting salarySetting);

    void update(SalarySetting salarySetting);
}
