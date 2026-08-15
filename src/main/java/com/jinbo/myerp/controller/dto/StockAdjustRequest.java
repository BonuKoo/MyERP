package com.jinbo.myerp.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull
        @Schema(description = "재고 증감량. 양수면 증가, 음수면 감소. 조정 결과 재고가 음수가 되면 실패한다.",
                example = "10")
        Integer quantityDelta
) {
}
