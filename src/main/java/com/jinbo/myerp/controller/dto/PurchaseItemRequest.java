package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.PurchaseItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PurchaseItemRequest(
        @NotNull @Schema(description = "매입할 규격의 id. 해당 규격의 현재재고가 이 수량만큼 증가한다.")
        Long itemSpecId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0") @Schema(description = "건별 매입단가. 규격의 costPrice와 다를 수 있다(변동 원가 반영).")
        BigDecimal unitPrice
) {
    public PurchaseItem toDomain() {
        return PurchaseItem.builder()
                .itemSpecId(itemSpecId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
    }
}
