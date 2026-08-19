package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.domain.LeaveStatus;
import com.jinbo.myerp.domain.LeaveType;
import com.jinbo.myerp.exception.InvalidLeaveRequestException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.LeaveRequestNotFoundException;
import com.jinbo.myerp.mapper.LeaveRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final EmployeeService employeeService;

    @Transactional
    public LeaveRequest apply(Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        verifySelf(employeeId);

        if (endDate.isBefore(startDate)) {
            throw new InvalidLeaveRequestException("종료일은 시작일 이후여야 합니다.");
        }

        BigDecimal leaveDays;
        if (leaveType == LeaveType.HALF_DAY) {
            if (!startDate.equals(endDate)) {
                throw new InvalidLeaveRequestException("반차는 시작일과 종료일이 같아야 합니다.");
            }
            leaveDays = new BigDecimal("0.5");
        } else {
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            leaveDays = BigDecimal.valueOf(days);
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employeeId(employeeId).leaveType(leaveType)
                .startDate(startDate).endDate(endDate).leaveDays(leaveDays)
                .reason(reason).status(LeaveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        leaveRequestMapper.insert(leaveRequest);
        return leaveRequest;
    }

    public List<LeaveRequest> findByEmployeeId(Long employeeId) {
        verifySelfOrOwner(employeeId);
        return leaveRequestMapper.findByEmployeeId(employeeId);
    }

    @Transactional
    public void approve(Long id) {
        LeaveRequest leaveRequest = findById(id);
        verifyPending(leaveRequest);
        Long approverId = currentUserId();
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approverId);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequestMapper.update(leaveRequest);
    }

    @Transactional
    public void reject(Long id) {
        LeaveRequest leaveRequest = findById(id);
        verifyPending(leaveRequest);
        Long approverId = currentUserId();
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(approverId);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequestMapper.update(leaveRequest);
    }

    private LeaveRequest findById(Long id) {
        return leaveRequestMapper.findById(id).orElseThrow(() -> new LeaveRequestNotFoundException(id));
    }

    private void verifyPending(LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStatusTransitionException("이미 처리된 휴가 신청입니다.");
        }
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void verifySelf(Long employeeId) {
        Employee me = employeeService.findByCompanyUserId(currentUserId());
        if (!me.getId().equals(employeeId)) {
            throw new AccessDeniedException("본인 명의로만 휴가를 신청할 수 있습니다.");
        }
    }

    private void verifySelfOrOwner(Long employeeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
        if (isOwner) {
            return;
        }

        Employee me = employeeService.findByCompanyUserId(currentUserId());
        if (!me.getId().equals(employeeId)) {
            throw new AccessDeniedException("본인의 휴가 신청만 조회할 수 있습니다.");
        }
    }
}
