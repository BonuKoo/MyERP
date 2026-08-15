package com.jinbo.myerp.controller.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull Integer quantityDelta
) {
}
