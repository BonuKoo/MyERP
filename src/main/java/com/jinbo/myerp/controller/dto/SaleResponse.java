package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String saleNo,
        Long partnerId,
        Long companyInfoId,
        LocalDate saleDate,
        BigDecimal totalAmount,
        SaleStatus status,
        String memo,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime canceledAt,
        List<SaleItemResponse> items
) {
    public static SaleResponse from(Sale sale, List<SaleItem> items) {
        return new SaleResponse(
                sale.getId(), sale.getSaleNo(), sale.getPartnerId(), sale.getCompanyInfoId(),
                sale.getSaleDate(), sale.getTotalAmount(), sale.getStatus(), sale.getMemo(),
                sale.getCreatedBy(), sale.getCreatedAt(), sale.getCanceledAt(),
                items.stream().map(SaleItemResponse::from).toList());
    }
}
