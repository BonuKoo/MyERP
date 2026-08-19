package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.domain.Salary;
import com.jinbo.myerp.domain.SalarySetting;
import com.jinbo.myerp.mapper.SalaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.033");
    private static final BigDecimal RESIDENT_TAX_RATE_OF_INCOME_TAX = new BigDecimal("0.1");
    private static final BigDecimal NATIONAL_PENSION_RATE = new BigDecimal("0.045");
    private static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.033");
    private static final BigDecimal EMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.008");
    private static final BigDecimal OVERTIME_HOURLY_RATE = new BigDecimal("10000");
    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    private final SalaryMapper salaryMapper;
    private final EmployeeService employeeService;
    private final PositionService positionService;
    private final AttendanceService attendanceService;
    private final SalarySettingService salarySettingService;

    @Transactional
    public List<Salary> calculate(YearMonth yearMonth) {
        SalarySetting setting = salarySettingService.get();
        String payYearMonth = yearMonth.toString();

        return employeeService.findAllActive().stream()
                .map(employee -> calculateOne(employee, yearMonth, payYearMonth, setting.getDailyWage()))
                .toList();
    }

    public PageResult<Salary> findByPayYearMonth(String payYearMonth, int page, int size) {
        int offset = page * size;
        return new PageResult<>(
                salaryMapper.findByPayYearMonth(payYearMonth, offset, size),
                salaryMapper.countByPayYearMonth(payYearMonth),
                page, size);
    }

    private Salary calculateOne(Employee employee, YearMonth yearMonth, String payYearMonth, BigDecimal dailyWage) {
        Position position = positionService.findById(employee.getPositionId());
        AttendanceSummary summary = attendanceService.monthlySummary(employee.getId(), yearMonth);

        BigDecimal basePay = dailyWage.multiply(BigDecimal.valueOf(summary.workDays()));
        BigDecimal positionAllowance = position.getAllowance();
        BigDecimal overtimeHours = BigDecimal.valueOf(summary.totalOvertimeMinutes())
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
        BigDecimal overtimePay = OVERTIME_HOURLY_RATE.multiply(overtimeHours);
        BigDecimal grossPay = basePay.add(positionAllowance).add(overtimePay);

        BigDecimal incomeTax = round(grossPay.multiply(INCOME_TAX_RATE));
        BigDecimal residentTax = round(incomeTax.multiply(RESIDENT_TAX_RATE_OF_INCOME_TAX));
        BigDecimal nationalPension = round(grossPay.multiply(NATIONAL_PENSION_RATE));
        BigDecimal healthInsurance = round(grossPay.multiply(HEALTH_INSURANCE_RATE));
        BigDecimal employmentInsurance = round(grossPay.multiply(EMPLOYMENT_INSURANCE_RATE));
        BigDecimal totalDeduction = incomeTax.add(residentTax).add(nationalPension)
                .add(healthInsurance).add(employmentInsurance);
        BigDecimal netPay = grossPay.subtract(totalDeduction);

        Salary salary = salaryMapper.findByEmployeeIdAndPayYearMonth(employee.getId(), payYearMonth).orElse(null);
        boolean isNew = salary == null;
        if (isNew) {
            salary = Salary.builder().employeeId(employee.getId()).payYearMonth(payYearMonth).build();
        }
        salary.setWorkDays(summary.workDays());
        salary.setBasePay(basePay);
        salary.setPositionAllowance(positionAllowance);
        salary.setOvertimePay(overtimePay);
        salary.setGrossPay(grossPay);
        salary.setIncomeTax(incomeTax);
        salary.setResidentTax(residentTax);
        salary.setNationalPension(nationalPension);
        salary.setHealthInsurance(healthInsurance);
        salary.setEmploymentInsurance(employmentInsurance);
        salary.setTotalDeduction(totalDeduction);
        salary.setNetPay(netPay);
        salary.setCalculatedAt(LocalDateTime.now());

        if (isNew) {
            salaryMapper.insert(salary);
        } else {
            salaryMapper.update(salary);
        }
        return salary;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
