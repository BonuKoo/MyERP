package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.ItemSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemSpecResponse(
        Long id,
        Long itemId,
        String specName,
        String unit,
        BigDecimal costPrice,
        BigDecimal salePrice,
        @Schema(description = "매입/매출/수동조정으로 자동 계산되는 실재고. 이 필드는 직접 수정할 수 없다.")
        int currentStock,
        int safetyStock,
        boolean active,
        @Schema(description = "낙관적 락 버전. 매출 전표 재고 차감 시 동시성 제어에 쓰인다. 클라이언트가 신경 쓸 값은 아니다.")
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
