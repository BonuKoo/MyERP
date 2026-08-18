package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DepartmentMapper {

    void insert(Department department);

    Optional<Department> findById(@Param("id") Long id);

    Optional<Department> findByName(@Param("name") String name);

    List<Department> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void update(Department department);
}
