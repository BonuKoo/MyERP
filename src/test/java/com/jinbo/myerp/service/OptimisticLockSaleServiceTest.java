package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.SaleStatus;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.exception.OptimisticLockConflictException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.SaleNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.SaleItemMapper;
import com.jinbo.myerp.mapper.SaleMapper;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OptimisticLockSaleServiceTest {

    @Mock
    private SaleMapper saleMapper;

    @Mock
    private SaleItemMapper saleItemMapper;

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
    private OptimisticLockSaleService saleService;

    private Sale newSaleRequest() {
        return Sale.builder()
                .partnerId(1L).companyInfoId(1L).saleDate(LocalDate.now()).memo("테스트").build();
    }

    @Test
    void register_success_decreasesStockAndRecordsHistory() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).partnerType(PartnerType.CUSTOMER).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(50).version(0).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));
        given(itemSpecMapper.updateStockOptimistic(eq(10L), eq(30), eq(0), any())).willReturn(1);
        given(ledgerService.adjustReceivableBalance(1L, new BigDecimal("400000"))).willReturn(new BigDecimal("900000"));

        SaleItem itemRequest = SaleItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("20000")).build();

        Sale result = saleService.register(newSaleRequest(), List.of(itemRequest), 99L);

        assertThat(result.getStatus()).isEqualTo(SaleStatus.CONFIRMED);
        assertThat(result.getSaleNo()).isNotBlank();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("400000");
        verify(saleMapper).insert(result);
        verify(saleItemMapper).insert(itemRequest);

        verify(itemSpecMapper, times(1)).updateStockOptimistic(eq(10L), eq(30), eq(0), any());

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        StockHistory history = captor.getValue();
        assertThat(history.getChangeType()).isEqualTo(StockChangeType.SALE_OUT);
        assertThat(history.getQuantity()).isEqualTo(-20);
        assertThat(history.getBeforeStock()).isEqualTo(50);
        assertThat(history.getAfterStock()).isEqualTo(30);
        assertThat(history.getCreatedBy()).isEqualTo(99L);

        // partner 잔액 조정이 sale insert보다 먼저 일어나야 한다 — sale.partner_id가
        // partner를 FK로 참조하므로, 순서가 바뀌면 sale insert가 먼저 잡는 공유 잠금 위에
        // 잔액 조정이 배타 잠금 승격을 요청하다가 동시 트랜잭션끼리 데드락이 난다.
        InOrder inOrder = inOrder(ledgerService, saleMapper);
        inOrder.verify(ledgerService).adjustReceivableBalance(1L, new BigDecimal("400000"));
        inOrder.verify(saleMapper).insert(result);

        verify(ledgerService).recordEntry(1L, LedgerType.RECEIVABLE, LedgerChangeType.SALE_CONFIRMED,
                new BigDecimal("400000"), new BigDecimal("900000"), "SALE", result.getId(), 99L);
    }

    @Test
    void register_retriesOnConflictThenSucceeds() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).partnerType(PartnerType.CUSTOMER).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));

        ItemSpec staleSpec = ItemSpec.builder().id(10L).currentStock(50).version(0).build();
        ItemSpec freshSpec = ItemSpec.builder().id(10L).currentStock(45).version(1).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(staleSpec), Optional.of(staleSpec), Optional.of(freshSpec));
        given(itemSpecMapper.updateStockOptimistic(eq(10L), eq(30), eq(0), any())).willReturn(0);
        given(itemSpecMapper.updateStockOptimistic(eq(10L), eq(25), eq(1), any())).willReturn(1);

        SaleItem itemRequest = SaleItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("20000")).build();

        saleService.register(newSaleRequest(), List.of(itemRequest), 99L);

        verify(itemSpecMapper, times(1)).updateStockOptimistic(eq(10L), eq(30), eq(0), any());
        verify(itemSpecMapper, times(1)).updateStockOptimistic(eq(10L), eq(25), eq(1), any());

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getBeforeStock()).isEqualTo(45);
        assertThat(captor.getValue().getAfterStock()).isEqualTo(25);
    }

    @Test
    void register_exceedsMaxRetries_throwsOptimisticLockConflictException() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).partnerType(PartnerType.CUSTOMER).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));

        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(50).version(0).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));
        given(itemSpecMapper.updateStockOptimistic(eq(10L), anyInt(), anyInt(), any())).willReturn(0);

        SaleItem itemRequest = SaleItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("20000")).build();

        assertThatThrownBy(() -> saleService.register(newSaleRequest(), List.of(itemRequest), 99L))
                .isInstanceOf(OptimisticLockConflictException.class);

        verify(stockHistoryMapper, never()).insert(any());
    }

    @Test
    void register_partnerNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.register(newSaleRequest(), List.of(), 99L))
                .isInstanceOf(PartnerNotFoundException.class);
    }

    @Test
    void register_companyInfoNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.register(newSaleRequest(), List.of(), 99L))
                .isInstanceOf(CompanyInfoNotFoundException.class);
    }

    @Test
    void register_itemSpecNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));
        given(itemSpecMapper.findById(10L)).willReturn(Optional.empty());

        SaleItem itemRequest = SaleItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("20000")).build();

        assertThatThrownBy(() -> saleService.register(newSaleRequest(), List.of(itemRequest), 99L))
                .isInstanceOf(ItemSpecNotFoundException.class);
    }

    @Test
    void register_insufficientStock_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(companyInfoMapper.findById(1L)).willReturn(Optional.of(CompanyInfo.builder().id(1L).build()));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(5).version(0).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));

        SaleItem itemRequest = SaleItem.builder().itemSpecId(10L).quantity(20).unitPrice(new BigDecimal("20000")).build();

        assertThatThrownBy(() -> saleService.register(newSaleRequest(), List.of(itemRequest), 99L))
                .isInstanceOf(InsufficientStockException.class);

        verify(saleMapper, never()).insert(any());
    }

    @Test
    void findById_notFound_throws() {
        given(saleMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.findById(1L))
                .isInstanceOf(SaleNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Sale> sales = List.of(Sale.builder().id(1L).build());
        given(saleMapper.findAll(0, 20)).willReturn(sales);
        given(saleMapper.countAll()).willReturn(1);

        PageResult<Sale> result = saleService.findAll(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void cancel_success_reversesStockAndRecordsHistory() {
        Sale sale = Sale.builder().id(1L).partnerId(1L).status(SaleStatus.CONFIRMED)
                .totalAmount(new BigDecimal("400000")).build();
        given(saleMapper.findById(1L)).willReturn(Optional.of(sale));
        SaleItem item = SaleItem.builder().id(1L).saleId(1L).itemSpecId(10L).quantity(20).build();
        given(saleItemMapper.findBySaleId(1L)).willReturn(List.of(item));
        ItemSpec spec = ItemSpec.builder().id(10L).currentStock(30).version(1).build();
        given(itemSpecMapper.findById(10L)).willReturn(Optional.of(spec));
        given(itemSpecMapper.updateStockOptimistic(eq(10L), eq(50), eq(1), any())).willReturn(1);

        Sale result = saleService.cancel(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(SaleStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
        verify(saleMapper).updateStatus(sale);

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(captor.getValue().getQuantity()).isEqualTo(20);
        assertThat(captor.getValue().getBeforeStock()).isEqualTo(30);
        assertThat(captor.getValue().getAfterStock()).isEqualTo(50);

        verify(ledgerService).recordReceivableChange(1L, LedgerChangeType.SALE_CANCELED,
                new BigDecimal("400000").negate(), "SALE", 1L, 99L);
    }

    @Test
    void cancel_alreadyCanceled_throwsAndDoesNotTouchStock() {
        Sale sale = Sale.builder().id(1L).status(SaleStatus.CANCELED).build();
        given(saleMapper.findById(1L)).willReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.cancel(1L, 99L))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(itemSpecMapper, never()).updateStockOptimistic(any(), anyInt(), anyInt(), any());
        verify(stockHistoryMapper, never()).insert(any());
        verify(ledgerService, never()).recordReceivableChange(any(), any(), any(), any(), any(), any());
    }
}
