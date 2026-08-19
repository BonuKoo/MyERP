package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Position;

import java.math.BigDecimal;

public record PositionResponse(
        Long id,
        String name,
        BigDecimal allowance,
        boolean active
) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getId(), position.getName(), position.getAllowance(), position.isActive());
    }
}
