package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.exception.CategoryMainNotFoundException;
import com.jinbo.myerp.mapper.CategoryMainMapper;
import com.jinbo.myerp.mapper.CategorySubMapper;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMainMapper categoryMainMapper;

    @Mock
    private CategorySubMapper categorySubMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void registerMain_setsActiveTrue() {
        CategoryMain category = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).build();

        CategoryMain result = categoryService.registerMain(category);

        assertThat(result.isActive()).isTrue();
        verify(categoryMainMapper).insert(category);
    }

    @Test
    void findAllMain_delegatesToMapper() {
        List<CategoryMain> mains = List.of(CategoryMain.builder().id(1L).name("a").build());
        given(categoryMainMapper.findAll()).willReturn(mains);

        List<CategoryMain> result = categoryService.findAllMain();

        assertThat(result).isEqualTo(mains);
    }

    @Test
    void registerSub_whenMainExists_setsActiveTrue() {
        given(categoryMainMapper.findById(1L)).willReturn(Optional.of(CategoryMain.builder().id(1L).name("a").build()));
        CategorySub sub = CategorySub.builder().categoryMainId(1L).name("내장타일 접착제").displayOrder(1).build();

        CategorySub result = categoryService.registerSub(sub);

        assertThat(result.isActive()).isTrue();
        verify(categorySubMapper).insert(sub);
    }

    @Test
    void registerSub_whenMainNotFound_throws() {
        given(categoryMainMapper.findById(99L)).willReturn(Optional.empty());
        CategorySub sub = CategorySub.builder().categoryMainId(99L).name("내장타일 접착제").build();

        assertThatThrownBy(() -> categoryService.registerSub(sub))
                .isInstanceOf(CategoryMainNotFoundException.class);
    }

    @Test
    void findSubsByMainId_delegatesToMapper() {
        List<CategorySub> subs = List.of(CategorySub.builder().id(1L).categoryMainId(1L).name("a").build());
        given(categorySubMapper.findByCategoryMainId(1L)).willReturn(subs);

        List<CategorySub> result = categoryService.findSubsByMainId(1L);

        assertThat(result).isEqualTo(subs);
    }

    @Test
    void findAllSub_delegatesToMapper() {
        List<CategorySub> subs = List.of(
                CategorySub.builder().id(1L).categoryMainId(1L).name("a").build(),
                CategorySub.builder().id(2L).categoryMainId(2L).name("b").build());
        given(categorySubMapper.findAll()).willReturn(subs);

        List<CategorySub> result = categoryService.findAllSub();

        assertThat(result).isEqualTo(subs);
    }
}
