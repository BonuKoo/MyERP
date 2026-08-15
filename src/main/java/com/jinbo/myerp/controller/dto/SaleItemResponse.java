package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.SaleItem;

import java.math.BigDecimal;

public record SaleItemResponse(Long id, Long itemSpecId, int quantity, BigDecimal unitPrice, BigDecimal amount) {
    public static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(item.getId(), item.getItemSpecId(), item.getQuantity(), item.getUnitPrice(), item.getAmount());
    }
}
