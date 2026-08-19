package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.domain.Salary;
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
class SalaryMapperTest {

    @Autowired
    private SalaryMapper salaryMapper;

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

    private Salary newSalary(String payYearMonth) {
        return Salary.builder()
                .employeeId(employeeId).payYearMonth(payYearMonth).workDays(20)
                .basePay(new BigDecimal("2000000")).positionAllowance(new BigDecimal("200000"))
                .overtimePay(BigDecimal.ZERO).grossPay(new BigDecimal("2200000"))
                .incomeTax(new BigDecimal("72600")).residentTax(new BigDecimal("7260"))
                .nationalPension(new BigDecimal("99000")).healthInsurance(new BigDecimal("72600"))
                .employmentInsurance(new BigDecimal("17600")).totalDeduction(new BigDecimal("269060"))
                .netPay(new BigDecimal("1930940")).calculatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindByEmployeeIdAndPayYearMonth() {
        Salary salary = newSalary("2026-08");

        salaryMapper.insert(salary);

        assertThat(salary.getId()).isNotNull();
        Optional<Salary> found = salaryMapper.findByEmployeeIdAndPayYearMonth(employeeId, "2026-08");
        assertThat(found).isPresent();
        assertThat(found.get().getNetPay()).isEqualByComparingTo("1930940");
    }

    @Test
    void findByPayYearMonth_returnsPagedResult() {
        salaryMapper.insert(newSalary("2026-08"));

        List<Salary> found = salaryMapper.findByPayYearMonth("2026-08", 0, 10);
        int total = salaryMapper.countByPayYearMonth("2026-08");

        assertThat(found).hasSize(1);
        assertThat(total).isEqualTo(1);
    }

    @Test
    void update_overwritesExistingCalculation() {
        Salary salary = newSalary("2026-08");
        salaryMapper.insert(salary);

        salary.setNetPay(new BigDecimal("2000000"));
        salaryMapper.update(salary);

        Salary updated = salaryMapper.findByEmployeeIdAndPayYearMonth(employeeId, "2026-08").orElseThrow();
        assertThat(updated.getNetPay()).isEqualByComparingTo("2000000");
    }
}
