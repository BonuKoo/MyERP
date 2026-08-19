package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Attendance;
import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.domain.Employee;
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
class AttendanceMapperTest {

    @Autowired
    private AttendanceMapper attendanceMapper;

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

    private Attendance newAttendance(LocalDate workDate) {
        return Attendance.builder().employeeId(employeeId).workDate(workDate)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void insertAndFindById() {
        Attendance attendance = newAttendance(LocalDate.of(2026, 8, 1));
        attendance.setClockIn(LocalDateTime.of(2026, 8, 1, 9, 5));

        attendanceMapper.insert(attendance);

        assertThat(attendance.getId()).isNotNull();
        Optional<Attendance> found = attendanceMapper.findById(attendance.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getClockIn()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 5));
        assertThat(found.get().getClockOut()).isNull();
    }

    @Test
    void findByEmployeeIdAndWorkDate_whenExists_returnsIt() {
        attendanceMapper.insert(newAttendance(LocalDate.of(2026, 8, 1)));

        Optional<Attendance> found = attendanceMapper.findByEmployeeIdAndWorkDate(employeeId, LocalDate.of(2026, 8, 1));

        assertThat(found).isPresent();
    }

    @Test
    void findByEmployeeIdAndWorkDate_whenNotExists_returnsEmpty() {
        assertThat(attendanceMapper.findByEmployeeIdAndWorkDate(employeeId, LocalDate.of(2026, 8, 1))).isEmpty();
    }

    @Test
    void findByEmployeeIdAndMonth_returnsOnlyThatMonth() {
        attendanceMapper.insert(newAttendance(LocalDate.of(2026, 8, 1)));
        attendanceMapper.insert(newAttendance(LocalDate.of(2026, 8, 15)));
        attendanceMapper.insert(newAttendance(LocalDate.of(2026, 9, 1)));

        List<Attendance> augustRecords = attendanceMapper.findByEmployeeIdAndMonth(
                employeeId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));

        assertThat(augustRecords).hasSize(2);
    }

    @Test
    void update_setsClockOut() {
        Attendance attendance = newAttendance(LocalDate.of(2026, 8, 1));
        attendance.setClockIn(LocalDateTime.of(2026, 8, 1, 9, 0));
        attendanceMapper.insert(attendance);

        attendance.setClockOut(LocalDateTime.of(2026, 8, 1, 18, 30));
        attendanceMapper.update(attendance);

        Attendance updated = attendanceMapper.findById(attendance.getId()).orElseThrow();
        assertThat(updated.getClockOut()).isEqualTo(LocalDateTime.of(2026, 8, 1, 18, 30));
    }
}
