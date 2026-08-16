package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryResponse(
        Long id,
        Long partnerId,
        LedgerType ledgerType,
        LedgerChangeType changeType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String relatedDocumentType,
        Long relatedDocumentId,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(), entry.getPartnerId(), entry.getLedgerType(), entry.getChangeType(),
                entry.getAmount(), entry.getBalanceAfter(), entry.getRelatedDocumentType(),
                entry.getRelatedDocumentId(), entry.getCreatedBy(), entry.getCreatedAt());
    }
}
