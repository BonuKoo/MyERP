package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CompanyUserMapper {

    void insert(CompanyUser companyUser);

    Optional<CompanyUser> findById(@Param("id") Long id);

    Optional<CompanyUser> findByEmail(@Param("email") String email);

    List<CompanyUser> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void update(CompanyUser companyUser);
}
