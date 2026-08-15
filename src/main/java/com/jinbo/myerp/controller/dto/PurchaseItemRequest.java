package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.PurchaseItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PurchaseItemRequest(
        @NotNull Long itemSpecId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice
) {
    public PurchaseItem toDomain() {
        return PurchaseItem.builder()
                .itemSpecId(itemSpecId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }
}
