package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;

import java.time.LocalDateTime;

public record StockHistoryResponse(
        Long id,
        Long itemSpecId,
        StockChangeType changeType,
        int quantity,
        int beforeStock,
        int afterStock,
        String relatedDocumentType,
        Long relatedDocumentId,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static StockHistoryResponse from(StockHistory history) {
        return new StockHistoryResponse(
                history.getId(), history.getItemSpecId(), history.getChangeType(), history.getQuantity(),
                history.getBeforeStock(), history.getAfterStock(), history.getRelatedDocumentType(),
                history.getRelatedDocumentId(), history.getCreatedBy(), history.getCreatedAt());
    }
}
