package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Attendance;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.mapper.AttendanceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AttendanceService attendanceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Employee employee(Long id) {
        return Employee.builder().id(id).companyUserId(9L).build();
    }

    @Test
    void clockIn_firstTimeToday_insertsNewRecord() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        given(attendanceMapper.findByEmployeeIdAndWorkDate(1L, LocalDate.now())).willReturn(Optional.empty());

        Attendance result = attendanceService.clockIn();

        assertThat(result.getClockIn()).isNotNull();
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        verify(attendanceMapper).insert(result);
    }

    @Test
    void clockIn_alreadyClockedIn_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        Attendance existing = Attendance.builder().id(1L).employeeId(1L).workDate(LocalDate.now())
                .clockIn(LocalDateTime.now()).build();
        given(attendanceMapper.findByEmployeeIdAndWorkDate(1L, LocalDate.now())).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> attendanceService.clockIn())
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void clockOut_afterClockIn_setsClockOut() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        Attendance existing = Attendance.builder().id(1L).employeeId(1L).workDate(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(9)).build();
        given(attendanceMapper.findByEmployeeIdAndWorkDate(1L, LocalDate.now())).willReturn(Optional.of(existing));

        Attendance result = attendanceService.clockOut();

        assertThat(result.getClockOut()).isNotNull();
        verify(attendanceMapper).update(existing);
    }

    @Test
    void clockOut_withoutClockIn_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        given(attendanceMapper.findByEmployeeIdAndWorkDate(1L, LocalDate.now())).willReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOut())
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void clockOut_alreadyClockedOut_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        Attendance existing = Attendance.builder().id(1L).employeeId(1L).workDate(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(9)).clockOut(LocalDateTime.now()).build();
        given(attendanceMapper.findByEmployeeIdAndWorkDate(1L, LocalDate.now())).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> attendanceService.clockOut())
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void monthlySummary_ownerCanViewAnyone() {
        authenticateAs(99L, "OWNER");
        List<Attendance> records = List.of(
                Attendance.builder().employeeId(1L).workDate(LocalDate.of(2026, 8, 1))
                        .clockIn(LocalDateTime.of(2026, 8, 1, 9, 15)).clockOut(LocalDateTime.of(2026, 8, 1, 18, 30)).build(),
                Attendance.builder().employeeId(1L).workDate(LocalDate.of(2026, 8, 2))
                        .clockIn(LocalDateTime.of(2026, 8, 2, 8, 50)).clockOut(LocalDateTime.of(2026, 8, 2, 17, 40)).build()
        );
        given(attendanceMapper.findByEmployeeIdAndMonth(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)))
                .willReturn(records);

        AttendanceSummary summary = attendanceService.monthlySummary(1L, YearMonth.of(2026, 8));

        assertThat(summary.workDays()).isEqualTo(2);
        assertThat(summary.totalLateMinutes()).isEqualTo(15);
        assertThat(summary.totalEarlyLeaveMinutes()).isEqualTo(20);
        assertThat(summary.totalOvertimeMinutes()).isEqualTo(30);
    }

    @Test
    void monthlySummary_staffViewingSelf_allowed() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));
        given(attendanceMapper.findByEmployeeIdAndMonth(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)))
                .willReturn(List.of());

        AttendanceSummary summary = attendanceService.monthlySummary(1L, YearMonth.of(2026, 8));

        assertThat(summary.workDays()).isEqualTo(0);
    }

    @Test
    void monthlySummary_staffViewingOthers_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(employee(1L));

        assertThatThrownBy(() -> attendanceService.monthlySummary(2L, YearMonth.of(2026, 8)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
