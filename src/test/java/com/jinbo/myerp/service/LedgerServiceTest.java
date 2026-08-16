package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.mapper.LedgerEntryMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void recordReceivableChange_adjustsBalanceAndInsertsEntry() {
        given(partnerMapper.findById(1L)).willReturn(
                Optional.of(Partner.builder().id(1L).receivableBalance(new BigDecimal("300000.00")).build()));

        ledgerService.recordReceivableChange(1L, LedgerChangeType.SALE_CONFIRMED, new BigDecimal("300000.00"),
                "SALE", 5L, 99L);

        verify(partnerMapper).adjustReceivableBalance(1L, new BigDecimal("300000.00"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper).insert(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertThat(entry.getPartnerId()).isEqualTo(1L);
        assertThat(entry.getLedgerType()).isEqualTo(LedgerType.RECEIVABLE);
        assertThat(entry.getChangeType()).isEqualTo(LedgerChangeType.SALE_CONFIRMED);
        assertThat(entry.getAmount()).isEqualByComparingTo("300000.00");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("300000.00");
        assertThat(entry.getRelatedDocumentType()).isEqualTo("SALE");
        assertThat(entry.getRelatedDocumentId()).isEqualTo(5L);
        assertThat(entry.getCreatedBy()).isEqualTo(99L);
    }

    @Test
    void recordPayableChange_adjustsBalanceAndInsertsEntry() {
        given(partnerMapper.findById(2L)).willReturn(
                Optional.of(Partner.builder().id(2L).payableBalance(new BigDecimal("150000.00")).build()));

        ledgerService.recordPayableChange(2L, LedgerChangeType.PURCHASE_CONFIRMED, new BigDecimal("150000.00"),
                "PURCHASE", 7L, 99L);

        verify(partnerMapper).adjustPayableBalance(2L, new BigDecimal("150000.00"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper).insert(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertThat(entry.getLedgerType()).isEqualTo(LedgerType.PAYABLE);
        assertThat(entry.getChangeType()).isEqualTo(LedgerChangeType.PURCHASE_CONFIRMED);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("150000.00");
    }

    @Test
    void adjustReceivableBalance_returnsNewBalanceWithoutInsertingEntry() {
        given(partnerMapper.findById(1L)).willReturn(
                Optional.of(Partner.builder().id(1L).receivableBalance(new BigDecimal("300000.00")).build()));

        BigDecimal balanceAfter = ledgerService.adjustReceivableBalance(1L, new BigDecimal("300000.00"));

        verify(partnerMapper).adjustReceivableBalance(1L, new BigDecimal("300000.00"));
        assertThat(balanceAfter).isEqualByComparingTo("300000.00");
        verify(ledgerEntryMapper, never()).insert(any());
    }

    @Test
    void adjustPayableBalance_returnsNewBalanceWithoutInsertingEntry() {
        given(partnerMapper.findById(2L)).willReturn(
                Optional.of(Partner.builder().id(2L).payableBalance(new BigDecimal("150000.00")).build()));

        BigDecimal balanceAfter = ledgerService.adjustPayableBalance(2L, new BigDecimal("150000.00"));

        verify(partnerMapper).adjustPayableBalance(2L, new BigDecimal("150000.00"));
        assertThat(balanceAfter).isEqualByComparingTo("150000.00");
        verify(ledgerEntryMapper, never()).insert(any());
    }

    @Test
    void recordEntry_insertsEntryWithoutAdjustingBalance() {
        ledgerService.recordEntry(1L, LedgerType.RECEIVABLE, LedgerChangeType.SALE_CONFIRMED,
                new BigDecimal("300000.00"), new BigDecimal("300000.00"), "SALE", 5L, 99L);

        verify(partnerMapper, never()).adjustReceivableBalance(any(), any());
        verify(partnerMapper, never()).adjustPayableBalance(any(), any());

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper).insert(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertThat(entry.getPartnerId()).isEqualTo(1L);
        assertThat(entry.getLedgerType()).isEqualTo(LedgerType.RECEIVABLE);
        assertThat(entry.getRelatedDocumentId()).isEqualTo(5L);
    }

    @Test
    void findByPartnerId_returnsPageResult() {
        List<LedgerEntry> entries = List.of(LedgerEntry.builder().id(1L).partnerId(1L).build());
        given(ledgerEntryMapper.findByPartnerId(1L, 0, 20)).willReturn(entries);
        given(ledgerEntryMapper.countByPartnerId(1L)).willReturn(1);

        PageResult<LedgerEntry> result = ledgerService.findByPartnerId(1L, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }
}
