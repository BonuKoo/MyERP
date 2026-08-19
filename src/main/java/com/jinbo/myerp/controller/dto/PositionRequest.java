package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Position;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PositionRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @DecimalMin("0.00") BigDecimal allowance
) {
    public Position toDomain() {
        return Position.builder().name(name).allowance(allowance).build();
    }
}
