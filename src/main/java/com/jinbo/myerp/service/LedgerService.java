package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.mapper.LedgerEntryMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryMapper ledgerEntryMapper;
    private final PartnerMapper partnerMapper;

    @Transactional
    public void recordReceivableChange(Long partnerId, LedgerChangeType changeType, BigDecimal amount,
                                        String relatedDocumentType, Long relatedDocumentId, Long userId) {
        partnerMapper.adjustReceivableBalance(partnerId, amount);
        Partner partner = partnerMapper.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        insertEntry(partnerId, LedgerType.RECEIVABLE, changeType, amount, partner.getReceivableBalance(),
                relatedDocumentType, relatedDocumentId, userId);
    }

    @Transactional
    public void recordPayableChange(Long partnerId, LedgerChangeType changeType, BigDecimal amount,
                                     String relatedDocumentType, Long relatedDocumentId, Long userId) {
        partnerMapper.adjustPayableBalance(partnerId, amount);
        Partner partner = partnerMapper.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        insertEntry(partnerId, LedgerType.PAYABLE, changeType, amount, partner.getPayableBalance(),
                relatedDocumentType, relatedDocumentId, userId);
    }

    private void insertEntry(Long partnerId, LedgerType ledgerType, LedgerChangeType changeType, BigDecimal amount,
                              BigDecimal balanceAfter, String relatedDocumentType, Long relatedDocumentId, Long userId) {
        LedgerEntry entry = LedgerEntry.builder()
                .partnerId(partnerId)
                .ledgerType(ledgerType)
                .changeType(changeType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .relatedDocumentType(relatedDocumentType)
                .relatedDocumentId(relatedDocumentId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryMapper.insert(entry);
    }
}
