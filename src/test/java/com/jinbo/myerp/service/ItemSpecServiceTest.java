package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.mapper.ItemMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemSpecServiceTest {

    @Mock
    private ItemSpecMapper itemSpecMapper;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemSpecService itemSpecService;

    @Test
    void register_whenItemExists_insertsWithDefaults() {
        given(itemMapper.findById(1L)).willReturn(Optional.of(Item.builder().id(1L).build()));
        ItemSpec spec = ItemSpec.builder().itemId(1L).specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("15000")).salePrice(new BigDecimal("20000")).build();

        ItemSpec result = itemSpecService.register(spec);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCurrentStock()).isEqualTo(0);
        assertThat(result.getVersion()).isEqualTo(0);
        verify(itemSpecMapper).insert(spec);
    }

    @Test
    void register_whenItemNotFound_throws() {
        given(itemMapper.findById(99L)).willReturn(Optional.empty());
        ItemSpec spec = ItemSpec.builder().itemId(99L).specName("x").build();

        assertThatThrownBy(() -> itemSpecService.register(spec))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(itemSpecMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemSpecService.findById(1L))
                .isInstanceOf(ItemSpecNotFoundException.class);
    }

    @Test
    void findByItemId_delegatesToMapper() {
        List<ItemSpec> specs = List.of(ItemSpec.builder().id(1L).itemId(1L).build());
        given(itemSpecMapper.findByItemId(1L)).willReturn(specs);

        List<ItemSpec> result = itemSpecService.findByItemId(1L);

        assertThat(result).isEqualTo(specs);
    }

    @Test
    void update_changesPriceAndSafetyStock_butNotCurrentStockOrVersion() {
        ItemSpec existing = ItemSpec.builder().id(1L).itemId(1L).specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("15000")).salePrice(new BigDecimal("20000"))
                .currentStock(100).safetyStock(10).version(3).build();
        given(itemSpecMapper.findById(1L)).willReturn(Optional.of(existing));
        ItemSpec changes = ItemSpec.builder().specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("16000")).salePrice(new BigDecimal("21000")).safetyStock(15).build();

        ItemSpec result = itemSpecService.update(1L, changes);

        assertThat(result.getCostPrice()).isEqualByComparingTo("16000");
        assertThat(result.getSalePrice()).isEqualByComparingTo("21000");
        assertThat(result.getSafetyStock()).isEqualTo(15);
        assertThat(result.getCurrentStock()).isEqualTo(100);
        assertThat(result.getVersion()).isEqualTo(3);
        verify(itemSpecMapper).update(existing);
    }
}
