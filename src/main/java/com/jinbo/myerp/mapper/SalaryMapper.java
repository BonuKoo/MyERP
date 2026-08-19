package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Salary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SalaryMapper {

    void insert(Salary salary);

    Optional<Salary> findByEmployeeIdAndPayYearMonth(
            @Param("employeeId") Long employeeId, @Param("payYearMonth") String payYearMonth);

    List<Salary> findByPayYearMonth(
            @Param("payYearMonth") String payYearMonth, @Param("offset") int offset, @Param("size") int size);

    int countByPayYearMonth(@Param("payYearMonth") String payYearMonth);

    void update(Salary salary);
}
