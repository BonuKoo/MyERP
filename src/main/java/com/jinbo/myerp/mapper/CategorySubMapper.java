package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategorySub;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CategorySubMapper {

    void insert(CategorySub categorySub);

    Optional<CategorySub> findById(@Param("id") Long id);

    List<CategorySub> findByCategoryMainId(@Param("categoryMainId") Long categoryMainId);

    void update(CategorySub categorySub);
}
