package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.PurchaseItem;

import java.math.BigDecimal;

public record PurchaseItemResponse(Long id, Long itemSpecId, int quantity, BigDecimal unitPrice, BigDecimal amount) {
    public static PurchaseItemResponse from(PurchaseItem item) {
        return new PurchaseItemResponse(item.getId(), item.getItemSpecId(), item.getQuantity(), item.getUnitPrice(), item.getAmount());
    }
}
