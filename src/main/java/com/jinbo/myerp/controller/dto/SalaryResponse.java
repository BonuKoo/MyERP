package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Salary;

import java.math.BigDecimal;

public record SalaryResponse(
        Long id,
        Long employeeId,
        String payYearMonth,
        int workDays,
        BigDecimal basePay,
        BigDecimal positionAllowance,
        BigDecimal overtimePay,
        BigDecimal grossPay,
        BigDecimal incomeTax,
        BigDecimal residentTax,
        BigDecimal nationalPension,
        BigDecimal healthInsurance,
        BigDecimal employmentInsurance,
        BigDecimal totalDeduction,
        BigDecimal netPay
) {
    public static SalaryResponse from(Salary salary) {
        return new SalaryResponse(
                salary.getId(), salary.getEmployeeId(), salary.getPayYearMonth(), salary.getWorkDays(),
                salary.getBasePay(), salary.getPositionAllowance(), salary.getOvertimePay(), salary.getGrossPay(),
                salary.getIncomeTax(), salary.getResidentTax(), salary.getNationalPension(),
                salary.getHealthInsurance(), salary.getEmploymentInsurance(), salary.getTotalDeduction(),
                salary.getNetPay());
    }
}
