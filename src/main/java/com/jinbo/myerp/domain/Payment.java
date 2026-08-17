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
public class Payment {

    private Long id;
    private String paymentNo;
    private Long partnerId;
    private PaymentType paymentType;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String method;
    private String memo;
    private PaymentStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime canceledAt;
}
