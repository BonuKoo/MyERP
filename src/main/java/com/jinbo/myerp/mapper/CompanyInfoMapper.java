package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CompanyInfoMapper {

    void insert(CompanyInfo companyInfo);

    Optional<CompanyInfo> findById(@Param("id") Long id);

    List<CompanyInfo> findAll();

    void update(CompanyInfo companyInfo);
}
