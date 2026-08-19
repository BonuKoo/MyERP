package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AttendanceMapper {

    void insert(Attendance attendance);

    Optional<Attendance> findById(@Param("id") Long id);

    Optional<Attendance> findByEmployeeIdAndWorkDate(
            @Param("employeeId") Long employeeId, @Param("workDate") LocalDate workDate);

    List<Attendance> findByEmployeeIdAndMonth(
            @Param("employeeId") Long employeeId,
            @Param("monthStart") LocalDate monthStart, @Param("monthEndExclusive") LocalDate monthEndExclusive);

    void update(Attendance attendance);
}
