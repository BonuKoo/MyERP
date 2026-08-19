package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        LocalDate workDate,
        LocalDateTime clockIn,
        LocalDateTime clockOut
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(), attendance.getEmployeeId(), attendance.getWorkDate(),
                attendance.getClockIn(), attendance.getClockOut());
    }
}
