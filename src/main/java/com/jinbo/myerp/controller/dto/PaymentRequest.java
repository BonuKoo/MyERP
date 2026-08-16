package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull Long partnerId,
        @NotNull PaymentType paymentType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @Size(max = 20) String method,
        @Size(max = 255) String memo
) {
    public Payment toDomain() {
        return Payment.builder()
                .partnerId(partnerId)
                .paymentType(paymentType)
                .amount(amount)
                .paymentDate(paymentDate)
                .method(method)
                .memo(memo)
                .build();
    }
}
