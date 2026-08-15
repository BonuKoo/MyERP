package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Certification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CertificationMapper {

    void insert(Certification certification);

    Optional<Certification> findById(@Param("id") Long id);

    Optional<Certification> findByName(@Param("name") String name);

    List<Certification> findAll();
}
