package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.service.AttendanceSummary;

public record AttendanceSummaryResponse(
        Long employeeId,
        String yearMonth,
        int workDays,
        long totalLateMinutes,
        long totalEarlyLeaveMinutes,
        long totalOvertimeMinutes
) {
    public static AttendanceSummaryResponse from(AttendanceSummary summary) {
        return new AttendanceSummaryResponse(
                summary.employeeId(), summary.yearMonth(), summary.workDays(),
                summary.totalLateMinutes(), summary.totalEarlyLeaveMinutes(), summary.totalOvertimeMinutes());
    }
}
