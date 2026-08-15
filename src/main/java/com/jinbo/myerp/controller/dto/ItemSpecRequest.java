package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.ItemSpec;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemSpecRequest(
        @NotBlank @Size(max = 50) String specName,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @DecimalMin("0") BigDecimal costPrice,
        @NotNull @DecimalMin("0") BigDecimal salePrice,
        @Min(0) int safetyStock
) {
    public ItemSpec toDomain() {
        return ItemSpec.builder()
                .specName(specName)
                .unit(unit)
                .costPrice(costPrice)
                .salePrice(salePrice)
                .safetyStock(safetyStock)
                .build();
    }
}
