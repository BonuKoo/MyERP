package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseItem;
import com.jinbo.myerp.domain.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(
        Long id,
        String purchaseNo,
        Long partnerId,
        Long companyInfoId,
        LocalDate purchaseDate,
        BigDecimal totalAmount,
        PurchaseStatus status,
        String memo,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime canceledAt,
        List<PurchaseItemResponse> items
) {
    public static PurchaseResponse from(Purchase purchase, List<PurchaseItem> items) {
        return new PurchaseResponse(
                purchase.getId(), purchase.getPurchaseNo(), purchase.getPartnerId(), purchase.getCompanyInfoId(),
                purchase.getPurchaseDate(), purchase.getTotalAmount(), purchase.getStatus(), purchase.getMemo(),
                purchase.getCreatedBy(), purchase.getCreatedAt(), purchase.getCanceledAt(),
                items.stream().map(PurchaseItemResponse::from).toList());
    }
}
