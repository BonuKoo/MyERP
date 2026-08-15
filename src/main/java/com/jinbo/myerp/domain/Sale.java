package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    private Long id;
    private String saleNo;
    private Long partnerId;
    private Long companyInfoId;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private SaleStatus status;
    private String memo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime canceledAt;
}
