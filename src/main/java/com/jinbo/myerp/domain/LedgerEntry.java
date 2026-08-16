package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    private Long id;
    private Long partnerId;
    private LedgerType ledgerType;
    private LedgerChangeType changeType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String relatedDocumentType;
    private Long relatedDocumentId;
    private Long createdBy;
    private LocalDateTime createdAt;
}
