package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.StockHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ItemSpecMapper itemSpecMapper;

    @Mock
    private StockHistoryMapper stockHistoryMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    void adjustStock_increasesStockAndRecordsHistory() {
        ItemSpec spec = ItemSpec.builder().id(1L).currentStock(10).version(0).build();
        given(itemSpecMapper.findById(1L)).willReturn(Optional.of(spec));

        ItemSpec result = stockService.adjustStock(1L, 20, 99L);

        assertThat(result.getCurrentStock()).isEqualTo(30);
        assertThat(result.getVersion()).isEqualTo(1);
        verify(itemSpecMapper).update(spec);

        ArgumentCaptor<StockHistory> captor = ArgumentCaptor.forClass(StockHistory.class);
        verify(stockHistoryMapper).insert(captor.capture());
        StockHistory history = captor.getValue();
        assertThat(history.getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(history.getQuantity()).isEqualTo(20);
        assertThat(history.getBeforeStock()).isEqualTo(10);
        assertThat(history.getAfterStock()).isEqualTo(30);
        assertThat(history.getCreatedBy()).isEqualTo(99L);
    }

    @Test
    void adjustStock_decreasesStock() {
        ItemSpec spec = ItemSpec.builder().id(1L).currentStock(10).version(0).build();
        given(itemSpecMapper.findById(1L)).willReturn(Optional.of(spec));

        ItemSpec result = stockService.adjustStock(1L, -4, 99L);

        assertThat(result.getCurrentStock()).isEqualTo(6);
    }

    @Test
    void adjustStock_wouldGoNegative_throwsAndDoesNotWrite() {
        ItemSpec spec = ItemSpec.builder().id(1L).currentStock(5).version(0).build();
        given(itemSpecMapper.findById(1L)).willReturn(Optional.of(spec));

        assertThatThrownBy(() -> stockService.adjustStock(1L, -10, 99L))
                .isInstanceOf(InsufficientStockException.class);

        verify(itemSpecMapper, never()).update(any());
        verify(stockHistoryMapper, never()).insert(any());
    }

    @Test
    void adjustStock_itemSpecNotFound_throws() {
        given(itemSpecMapper.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.adjustStock(99L, 10, 1L))
                .isInstanceOf(ItemSpecNotFoundException.class);
    }

    @Test
    void findHistoryByItemSpecId_returnsPageResult() {
        List<StockHistory> histories = List.of(StockHistory.builder().id(1L).itemSpecId(1L).build());
        given(stockHistoryMapper.findByItemSpecId(1L, 0, 20)).willReturn(histories);
        given(stockHistoryMapper.countByItemSpecId(1L)).willReturn(1);

        PageResult<StockHistory> result = stockService.findHistoryByItemSpecId(1L, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }
}
