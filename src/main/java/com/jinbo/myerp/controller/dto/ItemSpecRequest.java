package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.ItemSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemSpecRequest(
        @NotBlank @Size(max = 50) @Schema(example = "20kg") String specName,
        @NotBlank @Size(max = 20) @Schema(example = "BOX") String unit,
        @NotNull @DecimalMin("0.01") @Schema(description = "매입 시 기준이 되는 원가") BigDecimal costPrice,
        @NotNull @DecimalMin("0.01") @Schema(description = "매출 시 기준이 되는 판매가") BigDecimal salePrice,
        @Min(0) @Schema(description = "재고가 이 값 이하로 내려가도 시스템이 막지는 않는다(현재는 표시용 참고값).")
        int safetyStock
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
