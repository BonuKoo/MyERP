package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.exception.CategorySubNotFoundException;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.mapper.CategorySubMapper;
import com.jinbo.myerp.mapper.ItemCertificationMapper;
import com.jinbo.myerp.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private CategorySubMapper categorySubMapper;

    @Mock
    private ItemCertificationMapper itemCertificationMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void register_whenCategorySubExists_insertsItemAndCertifications() {
        given(categorySubMapper.findById(1L)).willReturn(Optional.of(CategorySub.builder().id(1L).build()));
        Item item = Item.builder().categorySubId(1L).name("세라픽스 PC-7000D").build();

        Item result = itemService.register(item, List.of(10L, 20L));

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        verify(itemMapper).insert(item);
        verify(itemCertificationMapper).insert(item.getId(), 10L);
        verify(itemCertificationMapper).insert(item.getId(), 20L);
    }

    @Test
    void register_whenCategorySubNotFound_throws() {
        given(categorySubMapper.findById(99L)).willReturn(Optional.empty());
        Item item = Item.builder().categorySubId(99L).name("x").build();

        assertThatThrownBy(() -> itemService.register(item, List.of()))
                .isInstanceOf(CategorySubNotFoundException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(itemMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(1L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Item> items = List.of(Item.builder().id(1L).name("a").build());
        given(itemMapper.findAll(0, 20, null, null)).willReturn(items);
        given(itemMapper.countAll(null, null)).willReturn(1);

        PageResult<Item> result = itemService.findAll(0, 20, null, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void findAll_withCategoryFilter_passesFilterToMapper() {
        given(itemMapper.findAll(0, 20, 1L, 2L)).willReturn(List.of());
        given(itemMapper.countAll(1L, 2L)).willReturn(0);

        itemService.findAll(0, 20, 1L, 2L);

        verify(itemMapper).findAll(0, 20, 1L, 2L);
        verify(itemMapper).countAll(1L, 2L);
    }

    @Test
    void findCertifications_delegatesToMapper() {
        List<Certification> certs = List.of(Certification.builder().id(1L).name("KS인증").build());
        given(itemCertificationMapper.findCertificationsByItemId(1L)).willReturn(certs);

        List<Certification> result = itemService.findCertifications(1L);

        assertThat(result).isEqualTo(certs);
    }

    @Test
    void update_replacesFieldsAndCertifications() {
        Item existing = Item.builder().id(1L).categorySubId(1L).name("변경전").active(true).build();
        given(itemMapper.findById(1L)).willReturn(Optional.of(existing));
        Item changes = Item.builder().categorySubId(1L).name("변경후").description("설명").ksStandard("KS L 1592").build();

        Item result = itemService.update(1L, changes, List.of(30L));

        assertThat(result.getName()).isEqualTo("변경후");
        verify(itemCertificationMapper).deleteByItemId(1L);
        verify(itemCertificationMapper).insert(1L, 30L);
        verify(itemMapper).update(existing);
    }
}
