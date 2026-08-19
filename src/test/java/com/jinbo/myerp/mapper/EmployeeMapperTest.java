package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class EmployeeMapperTest {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PositionMapper positionMapper;

    private Long departmentId;
    private Long positionId;

    @BeforeEach
    void setUp() {
        Department department = Department.builder().name("인사팀").active(true).createdAt(java.time.LocalDateTime.now()).build();
        departmentMapper.insert(department);
        departmentId = department.getId();

        Position position = Position.builder().name("팀장").allowance(new BigDecimal("200000")).active(true).build();
        positionMapper.insert(position);
        positionId = position.getId();
    }

    private Employee newEmployee(String name) {
        return Employee.builder()
                .departmentId(departmentId)
                .positionId(positionId)
                .name(name)
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        Employee employee = newEmployee("김철수");

        employeeMapper.insert(employee);

        assertThat(employee.getId()).isNotNull();
        Optional<Employee> found = employeeMapper.findById(employee.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().getCompanyUserId()).isNull();
    }

    @Test
    void findAll_filtersByDepartmentAndName() {
        employeeMapper.insert(newEmployee("김철수"));
        employeeMapper.insert(newEmployee("이영희"));

        List<Employee> filtered = employeeMapper.findAll(0, 10, departmentId, null, "철수");
        int total = employeeMapper.countAll(departmentId, null, "철수");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getName()).isEqualTo("김철수");
        assertThat(total).isEqualTo(1);
    }

    @Test
    void findAll_noFilters_returnsAll() {
        employeeMapper.insert(newEmployee("김철수"));
        employeeMapper.insert(newEmployee("이영희"));

        List<Employee> all = employeeMapper.findAll(0, 10, null, null, null);

        assertThat(all).hasSize(2);
    }

    @Test
    void update_changesFieldsAndResignation() {
        Employee employee = newEmployee("김철수");
        employeeMapper.insert(employee);

        employee.setName("김철수(개명)");
        employee.setResignationDate(LocalDate.of(2026, 6, 30));
        employee.setActive(false);
        employeeMapper.update(employee);

        Employee updated = employeeMapper.findById(employee.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("김철수(개명)");
        assertThat(updated.getResignationDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void findByCompanyUserId_whenLinked_returnsIt() {
        Employee employee = newEmployee("김철수");
        employee.setCompanyUserId(42L);
        employeeMapper.insert(employee);

        Optional<Employee> found = employeeMapper.findByCompanyUserId(42L);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
    }

    @Test
    void findByCompanyUserId_whenNotLinked_returnsEmpty() {
        assertThat(employeeMapper.findByCompanyUserId(999L)).isEmpty();
    }
}
