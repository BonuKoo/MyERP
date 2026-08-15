package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CategoryMainMapper {

    void insert(CategoryMain categoryMain);

    Optional<CategoryMain> findById(@Param("id") Long id);

    List<CategoryMain> findAll();

    void update(CategoryMain categoryMain);
}
