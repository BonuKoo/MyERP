package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.domain.LeaveStatus;
import com.jinbo.myerp.domain.LeaveType;
import com.jinbo.myerp.exception.InvalidLeaveRequestException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.LeaveRequestNotFoundException;
import com.jinbo.myerp.mapper.LeaveRequestMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void apply_fullDay_calculatesDaysInclusive() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        LeaveRequest result = leaveRequestService.apply(
                1L, LeaveType.FULL_DAY, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), "여행");

        assertThat(result.getLeaveDays()).isEqualByComparingTo("3");
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verify(leaveRequestMapper).insert(result);
    }

    @Test
    void apply_halfDay_setsHalfDay() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        LeaveRequest result = leaveRequestService.apply(
                1L, LeaveType.HALF_DAY, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), "병원");

        assertThat(result.getLeaveDays()).isEqualByComparingTo("0.5");
    }

    @Test
    void apply_halfDayWithDifferentDates_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        assertThatThrownBy(() -> leaveRequestService.apply(
                1L, LeaveType.HALF_DAY, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), "병원"))
                .isInstanceOf(InvalidLeaveRequestException.class);
    }

    @Test
    void apply_endBeforeStart_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        assertThatThrownBy(() -> leaveRequestService.apply(
                1L, LeaveType.FULL_DAY, LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 10), "여행"))
                .isInstanceOf(InvalidLeaveRequestException.class);
    }

    @Test
    void apply_forOtherEmployee_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        assertThatThrownBy(() -> leaveRequestService.apply(
                2L, LeaveType.FULL_DAY, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), "여행"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approve_pendingRequest_setsApproved() {
        authenticateAs(1L, "OWNER");
        LeaveRequest pending = LeaveRequest.builder().id(1L).status(LeaveStatus.PENDING).build();
        given(leaveRequestMapper.findById(1L)).willReturn(Optional.of(pending));

        leaveRequestService.approve(1L);

        assertThat(pending.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(pending.getApprovedBy()).isEqualTo(1L);
        assertThat(pending.getApprovedAt()).isNotNull();
        verify(leaveRequestMapper).update(pending);
    }

    @Test
    void approve_notFound_throws() {
        given(leaveRequestMapper.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.approve(99L))
                .isInstanceOf(LeaveRequestNotFoundException.class);
    }

    @Test
    void approve_alreadyDecided_throws() {
        LeaveRequest approved = LeaveRequest.builder().id(1L).status(LeaveStatus.APPROVED).build();
        given(leaveRequestMapper.findById(1L)).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> leaveRequestService.approve(1L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void reject_pendingRequest_setsRejected() {
        authenticateAs(1L, "OWNER");
        LeaveRequest pending = LeaveRequest.builder().id(1L).status(LeaveStatus.PENDING).build();
        given(leaveRequestMapper.findById(1L)).willReturn(Optional.of(pending));

        leaveRequestService.reject(1L);

        assertThat(pending.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        verify(leaveRequestMapper).update(pending);
    }

    @Test
    void findByEmployeeId_selfAllowed() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());
        given(leaveRequestMapper.findByEmployeeId(1L)).willReturn(List.of());

        List<LeaveRequest> result = leaveRequestService.findByEmployeeId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmployeeId_otherStaff_throws() {
        authenticateAs(9L, "STAFF");
        given(employeeService.findByCompanyUserId(9L)).willReturn(Employee.builder().id(1L).build());

        assertThatThrownBy(() -> leaveRequestService.findByEmployeeId(2L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
