package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.SaleItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull Long itemSpecId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice
) {
    public SaleItem toDomain() {
        return SaleItem.builder()
                .itemSpecId(itemSpecId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }
}
