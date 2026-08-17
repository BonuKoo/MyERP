package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.mapper.LedgerEntryMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryMapper ledgerEntryMapper;
    private final PartnerMapper partnerMapper;

    /**
     * partner 잔액만 원자적으로 조정하고 새 잔액을 반환한다. 이력(ledger_entry)은
     * 남기지 않는다 — 전표(sale/purchase/payment) insert보다 먼저 호출해서 partner
     * 행의 배타 잠금을 선점하기 위한 용도. 전표 insert가 partner를 FK로 참조해
     * 공유 잠금을 먼저 잡아버리면, 그 뒤에 이 메서드가 배타 잠금으로 승격을
     * 요청하다가 동시 트랜잭션끼리 데드락이 난다(4단계 sale_item/item_spec과
     * 같은 종류의 문제) — 그래서 반드시 전표 insert보다 먼저 호출해야 한다.
     */
    @Transactional
    public BigDecimal adjustReceivableBalance(Long partnerId, BigDecimal amount) {
        partnerMapper.adjustReceivableBalance(partnerId, amount);
        return partnerMapper.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId))
                .getReceivableBalance();
    }

    @Transactional
    public BigDecimal adjustPayableBalance(Long partnerId, BigDecimal amount) {
        partnerMapper.adjustPayableBalance(partnerId, amount);
        return partnerMapper.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId))
                .getPayableBalance();
    }

    /**
     * ledger_entry 이력만 기록한다(잔액은 건드리지 않음) — adjustReceivableBalance/
     * adjustPayableBalance로 이미 잔액을 조정한 뒤, 전표 ID(relatedDocumentId)가
     * 확보된 시점(전표 insert 이후)에 호출한다.
     */
    public void recordEntry(Long partnerId, LedgerType ledgerType, LedgerChangeType changeType, BigDecimal amount,
                             BigDecimal balanceAfter, String relatedDocumentType, Long relatedDocumentId, Long userId) {
        insertEntry(partnerId, ledgerType, changeType, amount, balanceAfter, relatedDocumentType, relatedDocumentId, userId);
    }

    /**
     * adjustReceivableBalance + recordEntry를 합친 편의 메서드. cancel 계열처럼
     * partner를 새로 FK 참조하는 자식 행 insert가 없는 경우에만 안전하다 —
     * register 계열에서는 절대 이 메서드를 직접 쓰지 말고 위 두 메서드로 쪼개서
     * 순서를 맞출 것(클래스 상단 주석 참고).
     */
    @Transactional
    public void recordReceivableChange(Long partnerId, LedgerChangeType changeType, BigDecimal amount,
                                        String relatedDocumentType, Long relatedDocumentId, Long userId) {
        BigDecimal balanceAfter = adjustReceivableBalance(partnerId, amount);
        recordEntry(partnerId, LedgerType.RECEIVABLE, changeType, amount, balanceAfter,
                relatedDocumentType, relatedDocumentId, userId);
    }

    @Transactional
    public void recordPayableChange(Long partnerId, LedgerChangeType changeType, BigDecimal amount,
                                     String relatedDocumentType, Long relatedDocumentId, Long userId) {
        BigDecimal balanceAfter = adjustPayableBalance(partnerId, amount);
        recordEntry(partnerId, LedgerType.PAYABLE, changeType, amount, balanceAfter,
                relatedDocumentType, relatedDocumentId, userId);
    }

    public PageResult<LedgerEntry> findByPartnerId(Long partnerId, int page, int size) {
        int offset = page * size;
        List<LedgerEntry> content = ledgerEntryMapper.findByPartnerId(partnerId, offset, size);
        long totalCount = ledgerEntryMapper.countByPartnerId(partnerId);
        return new PageResult<>(content, totalCount, page, size);
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
