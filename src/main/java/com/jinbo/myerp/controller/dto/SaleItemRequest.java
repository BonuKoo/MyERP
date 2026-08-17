package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.SaleItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull @Schema(description = "매출할 규격의 id. 해당 규격의 현재재고가 이 수량만큼 감소한다(부족하면 409).")
        Long itemSpecId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.01") @Schema(description = "건별 매출단가. 규격의 salePrice와 다를 수 있다(할인 등 반영).")
        BigDecimal unitPrice
) {
    public SaleItem toDomain() {
        return SaleItem.builder()
                .itemSpecId(itemSpecId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }
}
