package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Purchase;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public record PurchaseRequest(
        @NotNull Long partnerId,
        @NotNull Long companyInfoId,
        @NotNull LocalDate purchaseDate,
        @Size(max = 255) String memo,
        @NotEmpty @Valid List<PurchaseItemRequest> items
) {
    public Purchase toDomain() {
        return Purchase.builder()
                .partnerId(partnerId)
                .companyInfoId(companyInfoId)
                .purchaseDate(purchaseDate)
                .memo(memo)
                .build();
    }
}
