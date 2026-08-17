package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseItem;
import com.jinbo.myerp.domain.PurchaseStatus;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.PurchaseNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.PurchaseItemMapper;
import com.jinbo.myerp.mapper.PurchaseMapper;
import com.jinbo.myerp.mapper.StockHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseMapper purchaseMapper;

    @Mock
    private PurchaseItemMapper purchaseItemMapper;

    @Mock
    private PartnerMapper partnerMapper;

    @Mock
    private CompanyInfoMapper companyInfoMapper;

    @Mock
    private ItemSpecMapper itemSpecMapper;

    @Mock
    private StockHistoryMapper stockHistoryMapper;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase newPurchaseRequest() {
        return Purchase.builder()
                .partnerId(1L).companyInfoId(1L).purchaseDate(LocalDate.now()).memo("테스트").build();
    }

    @Test
    void register_success_increasesStockAndRecordsHistory() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).partnerType(PartnerType.SUPPLIER).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(5).version(0).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));
        given(ledgerService.adjustPayableBalance(1L, new BigDecimal("300000"))).willReturn(new BigDecimal("700000"));

        PurchaseItem itemRequest = PurchaseItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("15000")).build();

        Purchase result = purchaseService.register(newPurchaseRequest(), List.of(itemRequest), 99L);

        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.CONFIRMED);
        assertThat(result.getPurchaseNo()).isNotBlank();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("300000");
        verify(purchaseMapper).insert(result);
        verify(purchaseItemMapper).insert(itemRequest);
        assertThat(itemRequest.getAmount()).isEqualByComparingTo("300000");

        assertThat(spec.getCurrentStock()).isEqualTo(25);
        assertThat(spec.getVersion()).isEqualTo(1);
        verify(itemSpecMapper).update(spec);

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        StockHistory history = captor.getValue();
        assertThat(history.getChangeType()).isEqualTo(StockChangeType.PURCHASE_IN);
        assertThat(history.getQuantity()).isEqualTo(20);
        assertThat(history.getBeforeStock()).isEqualTo(5);
        assertThat(history.getAfterStock()).isEqualTo(25);
        assertThat(history.getCreatedBy()).isEqualTo(99L);

        InOrder inOrder = inOrder(ledgerService, purchaseMapper);
        inOrder.verify(ledgerService).adjustPayableBalance(1L, new BigDecimal("300000"));
        inOrder.verify(purchaseMapper).insert(result);

        verify(ledgerService).recordEntry(1L, LedgerType.PAYABLE, LedgerChangeType.PURCHASE_CONFIRMED,
                new BigDecimal("300000"), new BigDecimal("700000"), "PURCHASE", result.getId(), 99L);
    }

    @Test
    void register_partnerNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.register(newPurchaseRequest(), List.of(), 99L))
                .isInstanceOf(PartnerNotFoundException.class);
    }

    @Test
    void register_companyInfoNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.register(newPurchaseRequest(), List.of(), 99L))
                .isInstanceOf(CompanyInfoNotFoundException.class);
    }

    @Test
    void register_itemSpecNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));
        given(itemSpecMapper.findById(10L)).willReturn(Optional.empty());

        PurchaseItem itemRequest = PurchaseItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("15000")).build();

        assertThatThrownBy(() -> purchaseService.register(newPurchaseRequest(), List.of(itemRequest), 99L))
                .isInstanceOf(ItemSpecNotFoundException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(purchaseMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.findById(1L))
                .isInstanceOf(PurchaseNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Purchase> purchases = List.of(Purchase.builder().id(1L).build());
        given(purchaseMapper.findAll(0, 20)).willReturn(purchases);
        given(purchaseMapper.countAll()).willReturn(1);

        PageResult<Purchase> result = purchaseService.findAll(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void cancel_success_reversesStockAndRecordsHistory() {
        Purchase purchase = Purchase.builder().id(1L).partnerId(1L).status(PurchaseStatus.CONFIRMED)
                .totalAmount(new BigDecimal("300000")).build();
        given(purchaseMapper.findById(1L)).willReturn(Optional.of(purchase));
        PurchaseItem item = PurchaseItem.builder().id(1L).purchaseId(1L).itemSpecId(10L).quantity(20).build();
        given(purchaseItemMapper.findByPurchaseId(1L)).willReturn(List.of(item));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(25).version(1).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));

        Purchase result = purchaseService.cancel(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
        verify(purchaseMapper).updateStatus(purchase);

        assertThat(spec.getCurrentStock()).isEqualTo(5);
        verify(itemSpecMapper).update(spec);

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(captor.getValue().getQuantity()).isEqualTo(-20);

        verify(ledgerService).recordPayableChange(1L, LedgerChangeType.PURCHASE_CANCELED,
                new BigDecimal("300000").negate(), "PURCHASE", 1L, 99L);
    }

    @Test
    void cancel_alreadyCanceled_throwsAndDoesNotTouchStock() {
        Purchase purchase = Purchase.builder().id(1L).status(PurchaseStatus.CANCELED).build();
        given(purchaseMapper.findById(1L)).willReturn(Optional.of(purchase));

        assertThatThrownBy(() -> purchaseService.cancel(1L, 99L))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(itemSpecMapper, never()).update(any());
        verify(stockHistoryMapper, never()).insert(any());
        verify(ledgerService, never()).recordPayableChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancel_wouldMakeStockNegative_throws() {
        Purchase purchase = Purchase.builder().id(1L).status(PurchaseStatus.CONFIRMED).build();
        given(purchaseMapper.findById(1L)).willReturn(Optional.of(purchase));
        PurchaseItem item = PurchaseItem.builder().id(1L).purchaseId(1L).itemSpecId(10L).quantity(20).build();
        given(purchaseItemMapper.findByPurchaseId(1L)).willReturn(List.of(item));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(5).version(1).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));

        assertThatThrownBy(() -> purchaseService.cancel(1L, 99L))
                .isInstanceOf(InsufficientStockException.class);
    }
}
