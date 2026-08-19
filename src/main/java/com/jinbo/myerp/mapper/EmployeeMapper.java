package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmployeeMapper {

    void insert(Employee employee);

    Optional<Employee> findById(@Param("id") Long id);

    Optional<Employee> findByCompanyUserId(@Param("companyUserId") Long companyUserId);

    List<Employee> findAll(@Param("offset") int offset, @Param("size") int size,
                            @Param("departmentId") Long departmentId, @Param("positionId") Long positionId,
                            @Param("name") String name);

    int countAll(@Param("departmentId") Long departmentId, @Param("positionId") Long positionId,
                 @Param("name") String name);

    List<Employee> findAllActive();

    void update(Employee employee);
}
