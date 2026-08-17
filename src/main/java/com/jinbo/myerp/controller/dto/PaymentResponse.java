package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentStatus;
import com.jinbo.myerp.domain.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentNo,
        Long partnerId,
        PaymentType paymentType,
        BigDecimal amount,
        LocalDate paymentDate,
        String method,
        String memo,
        PaymentStatus status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getPaymentNo(), payment.getPartnerId(), payment.getPaymentType(),
                payment.getAmount(), payment.getPaymentDate(), payment.getMethod(), payment.getMemo(),
                payment.getStatus(), payment.getCreatedBy(), payment.getCreatedAt(), payment.getCanceledAt());
    }
}
