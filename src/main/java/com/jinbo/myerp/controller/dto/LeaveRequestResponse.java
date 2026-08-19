package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.domain.LeaveStatus;
import com.jinbo.myerp.domain.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal leaveDays,
        String reason,
        LeaveStatus status
) {
    public static LeaveRequestResponse from(LeaveRequest leaveRequest) {
        return new LeaveRequestResponse(
                leaveRequest.getId(), leaveRequest.getEmployeeId(), leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(), leaveRequest.getEndDate(), leaveRequest.getLeaveDays(),
                leaveRequest.getReason(), leaveRequest.getStatus());
    }
}
