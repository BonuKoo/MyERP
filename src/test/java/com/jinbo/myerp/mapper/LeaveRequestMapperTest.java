package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.domain.LeaveStatus;
import com.jinbo.myerp.domain.LeaveType;
import com.jinbo.myerp.domain.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class LeaveRequestMapperTest {

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PositionMapper positionMapper;

    private Long employeeId;

    @BeforeEach
    void setUp() {
        Department department = Department.builder().name("인사팀").active(true).createdAt(LocalDateTime.now()).build();
        departmentMapper.insert(department);
        Position position = Position.builder().name("팀장").allowance(BigDecimal.ZERO).active(true).build();
        positionMapper.insert(position);

        Employee employee = Employee.builder()
                .departmentId(department.getId()).positionId(position.getId())
                .name("김철수").hireDate(LocalDate.of(2026, 1, 1)).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        employeeMapper.insert(employee);
        employeeId = employee.getId();
    }

    private LeaveRequest newLeaveRequest() {
        return LeaveRequest.builder()
                .employeeId(employeeId).leaveType(LeaveType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 10)).endDate(LocalDate.of(2026, 8, 10))
                .leaveDays(new BigDecimal("1.0")).status(LeaveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        LeaveRequest leaveRequest = newLeaveRequest();

        leaveRequestMapper.insert(leaveRequest);

        assertThat(leaveRequest.getId()).isNotNull();
        Optional<LeaveRequest> found = leaveRequestMapper.findById(leaveRequest.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(found.get().getLeaveType()).isEqualTo(LeaveType.FULL_DAY);
        assertThat(found.get().getLeaveDays()).isEqualByComparingTo("1.0");
    }

    @Test
    void findByEmployeeId_returnsAll() {
        leaveRequestMapper.insert(newLeaveRequest());
        leaveRequestMapper.insert(newLeaveRequest());

        List<LeaveRequest> found = leaveRequestMapper.findByEmployeeId(employeeId);

        assertThat(found).hasSize(2);
    }

    @Test
    void update_changesStatusAndApprover() {
        LeaveRequest leaveRequest = newLeaveRequest();
        leaveRequestMapper.insert(leaveRequest);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(1L);
        leaveRequest.setApprovedAt(LocalDateTime.of(2026, 8, 11, 10, 0));
        leaveRequestMapper.update(leaveRequest);

        LeaveRequest updated = leaveRequestMapper.findById(leaveRequest.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(updated.getApprovedBy()).isEqualTo(1L);
        assertThat(updated.getApprovedAt()).isEqualTo(LocalDateTime.of(2026, 8, 11, 10, 0));
    }
}
