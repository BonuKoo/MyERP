package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LeaveRequestMapper {

    void insert(LeaveRequest leaveRequest);

    Optional<LeaveRequest> findById(@Param("id") Long id);

    List<LeaveRequest> findByEmployeeId(@Param("employeeId") Long employeeId);

    void update(LeaveRequest leaveRequest);
}
