package com.jinbo.myerp.service;

public record AttendanceSummary(
        Long employeeId,
        String yearMonth,
        int workDays,
        long totalLateMinutes,
        long totalEarlyLeaveMinutes,
        long totalOvertimeMinutes
) {
}
