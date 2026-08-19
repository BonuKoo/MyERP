package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salary {
    private Long id;
    private Long employeeId;
    private String payYearMonth;
    private int workDays;
    private BigDecimal basePay;
    private BigDecimal positionAllowance;
    private BigDecimal overtimePay;
    private BigDecimal grossPay;
    private BigDecimal incomeTax;
    private BigDecimal residentTax;
    private BigDecimal nationalPension;
    private BigDecimal healthInsurance;
    private BigDecimal employmentInsurance;
    private BigDecimal totalDeduction;
    private BigDecimal netPay;
    private LocalDateTime calculatedAt;
}
