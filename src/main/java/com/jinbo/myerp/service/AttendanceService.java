package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Attendance;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.mapper.AttendanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);

    private final AttendanceMapper attendanceMapper;
    private final EmployeeService employeeService;

    @Transactional
    public Attendance clockIn() {
        Employee me = currentEmployee();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceMapper.findByEmployeeIdAndWorkDate(me.getId(), today).orElse(null);
        if (attendance != null && attendance.getClockIn() != null) {
            throw new InvalidStatusTransitionException("이미 출근 처리되었습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (attendance == null) {
            attendance = Attendance.builder().employeeId(me.getId()).workDate(today)
                    .clockIn(now).createdAt(now).updatedAt(now).build();
            attendanceMapper.insert(attendance);
        } else {
            attendance.setClockIn(now);
            attendance.setUpdatedAt(now);
            attendanceMapper.update(attendance);
        }
        return attendance;
    }

    @Transactional
    public Attendance clockOut() {
        Employee me = currentEmployee();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceMapper.findByEmployeeIdAndWorkDate(me.getId(), today)
                .orElseThrow(() -> new InvalidStatusTransitionException("출근 기록이 없습니다."));
        if (attendance.getClockIn() == null) {
            throw new InvalidStatusTransitionException("출근 기록이 없습니다.");
        }
        if (attendance.getClockOut() != null) {
            throw new InvalidStatusTransitionException("이미 퇴근 처리되었습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setClockOut(now);
        attendance.setUpdatedAt(now);
        attendanceMapper.update(attendance);
        return attendance;
    }

    public AttendanceSummary monthlySummary(Long employeeId, YearMonth yearMonth) {
        verifySelfOrOwner(employeeId);

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEndExclusive = monthStart.plusMonths(1);
        List<Attendance> records = attendanceMapper.findByEmployeeIdAndMonth(employeeId, monthStart, monthEndExclusive);

        int workDays = 0;
        long totalLate = 0;
        long totalEarly = 0;
        long totalOvertime = 0;
        for (Attendance record : records) {
            if (record.getClockIn() == null) {
                continue;
            }
            workDays++;

            LocalTime clockInTime = record.getClockIn().toLocalTime();
            if (clockInTime.isAfter(START_TIME)) {
                totalLate += Duration.between(START_TIME, clockInTime).toMinutes();
            }

            if (record.getClockOut() != null) {
                LocalTime clockOutTime = record.getClockOut().toLocalTime();
                if (clockOutTime.isBefore(END_TIME)) {
                    totalEarly += Duration.between(clockOutTime, END_TIME).toMinutes();
                } else if (clockOutTime.isAfter(END_TIME)) {
                    totalOvertime += Duration.between(END_TIME, clockOutTime).toMinutes();
                }
            }
        }

        return new AttendanceSummary(employeeId, yearMonth.toString(), workDays, totalLate, totalEarly, totalOvertime);
    }

    private Employee currentEmployee() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return employeeService.findByCompanyUserId(userId);
    }

    private void verifySelfOrOwner(Long employeeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
        if (isOwner) {
            return;
        }

        Employee me = currentEmployee();
        if (!me.getId().equals(employeeId)) {
            throw new AccessDeniedException("본인의 근태만 조회할 수 있습니다.");
        }
    }
}
