package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.domain.Salary;
import com.jinbo.myerp.domain.SalarySetting;
import com.jinbo.myerp.mapper.SalaryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SalaryServiceTest {

    @Mock
    private SalaryMapper salaryMapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private PositionService positionService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private SalarySettingService salarySettingService;

    @InjectMocks
    private SalaryService salaryService;

    private Employee employee() {
        return Employee.builder().id(1L).positionId(1L).build();
    }

    @Test
    void calculate_newRecord_insertsWithCorrectAmounts() {
        given(employeeService.findAllActive()).willReturn(List.of(employee()));
        given(salarySettingService.get()).willReturn(SalarySetting.builder().dailyWage(new BigDecimal("100000")).build());
        given(positionService.findById(1L)).willReturn(Position.builder().id(1L).allowance(new BigDecimal("200000")).build());
        given(attendanceService.monthlySummary(1L, YearMonth.of(2026, 8)))
                .willReturn(new AttendanceSummary(1L, "2026-08", 20, 100, 50, 120));
        given(salaryMapper.findByEmployeeIdAndPayYearMonth(1L, "2026-08")).willReturn(Optional.empty());

        List<Salary> result = salaryService.calculate(YearMonth.of(2026, 8));

        assertThat(result).hasSize(1);
        Salary salary = result.get(0);
        assertThat(salary.getWorkDays()).isEqualTo(20);
        assertThat(salary.getBasePay()).isEqualByComparingTo("2000000");
        assertThat(salary.getPositionAllowance()).isEqualByComparingTo("200000");
        assertThat(salary.getOvertimePay()).isEqualByComparingTo("20000");
        assertThat(salary.getGrossPay()).isEqualByComparingTo("2220000");
        assertThat(salary.getIncomeTax()).isEqualByComparingTo("73260");
        assertThat(salary.getResidentTax()).isEqualByComparingTo("7326");
        assertThat(salary.getNationalPension()).isEqualByComparingTo("99900");
        assertThat(salary.getHealthInsurance()).isEqualByComparingTo("73260");
        assertThat(salary.getEmploymentInsurance()).isEqualByComparingTo("17760");
        assertThat(salary.getTotalDeduction()).isEqualByComparingTo("271506");
        assertThat(salary.getNetPay()).isEqualByComparingTo("1948494");
        verify(salaryMapper).insert(salary);
        verify(salaryMapper, never()).update(salary);
    }

    @Test
    void calculate_existingRecord_overwritesInsteadOfSkipping() {
        given(employeeService.findAllActive()).willReturn(List.of(employee()));
        given(salarySettingService.get()).willReturn(SalarySetting.builder().dailyWage(new BigDecimal("100000")).build());
        given(positionService.findById(1L)).willReturn(Position.builder().id(1L).allowance(new BigDecimal("200000")).build());
        given(attendanceService.monthlySummary(1L, YearMonth.of(2026, 8)))
                .willReturn(new AttendanceSummary(1L, "2026-08", 20, 0, 0, 0));
        Salary existing = Salary.builder().id(9L).employeeId(1L).payYearMonth("2026-08").build();
        given(salaryMapper.findByEmployeeIdAndPayYearMonth(1L, "2026-08")).willReturn(Optional.of(existing));

        List<Salary> result = salaryService.calculate(YearMonth.of(2026, 8));

        assertThat(result.get(0).getId()).isEqualTo(9L);
        verify(salaryMapper).update(existing);
        verify(salaryMapper, never()).insert(existing);
    }

    @Test
    void findByPayYearMonth_returnsPageResult() {
        Salary salary = Salary.builder().id(1L).employeeId(1L).payYearMonth("2026-08").build();
        given(salaryMapper.findByPayYearMonth("2026-08", 0, 10)).willReturn(List.of(salary));
        given(salaryMapper.countByPayYearMonth("2026-08")).willReturn(1);

        PageResult<Salary> result = salaryService.findByPayYearMonth("2026-08", 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }
}
