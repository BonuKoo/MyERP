package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.ItemSpec;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemSpecResponse(
        Long id,
        Long itemId,
        String specName,
        String unit,
        BigDecimal costPrice,
        BigDecimal salePrice,
        int currentStock,
        int safetyStock,
        boolean active,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ItemSpecResponse from(ItemSpec itemSpec) {
        return new ItemSpecResponse(
                itemSpec.getId(), itemSpec.getItemId(), itemSpec.getSpecName(), itemSpec.getUnit(),
                itemSpec.getCostPrice(), itemSpec.getSalePrice(), itemSpec.getCurrentStock(), itemSpec.getSafetyStock(),
                itemSpec.isActive(), itemSpec.getVersion(), itemSpec.getCreatedAt(), itemSpec.getUpdatedAt());
    }
}
