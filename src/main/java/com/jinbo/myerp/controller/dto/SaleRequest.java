package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Sale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SaleRequest(
        @NotNull Long partnerId,
        @NotNull Long companyInfoId,
        @NotNull LocalDate saleDate,
        @Size(max = 255) String memo,
        @NotEmpty @Valid List<SaleItemRequest> items
) {
    public Sale toDomain() {
        return Sale.builder()
                .partnerId(partnerId)
                .companyInfoId(companyInfoId)
                .saleDate(saleDate)
                .memo(memo)
                .build();
    }
}
