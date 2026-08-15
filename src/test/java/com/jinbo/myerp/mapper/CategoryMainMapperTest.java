package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class CategoryMainMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    private CategoryMain newCategory(String name, int order) {
        return CategoryMain.builder().name(name).displayOrder(order).active(true).build();
    }

    @Test
    void insertAndFindById() {
        CategoryMain category = newCategory("타일/건축용접착제", 1);

        categoryMainMapper.insert(category);
        assertThat(category.getId()).isNotNull();

        Optional<CategoryMain> found = categoryMainMapper.findById(category.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("타일/건축용접착제");
        assertThat(found.get().getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void findAll_returnsOrderedByDisplayOrder() {
        categoryMainMapper.insert(newCategory("건축용품", 5));
        categoryMainMapper.insert(newCategory("목공/지물용접착제", 2));
        categoryMainMapper.insert(newCategory("타일/건축용접착제", 1));

        List<CategoryMain> all = categoryMainMapper.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(CategoryMain::getDisplayOrder).containsExactly(1, 2, 5);
    }

    @Test
    void update_changesNameAndActive() {
        CategoryMain category = newCategory("변경전", 1);
        categoryMainMapper.insert(category);

        category.setName("변경후");
        category.setActive(false);
        categoryMainMapper.update(category);

        CategoryMain updated = categoryMainMapper.findById(category.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
        assertThat(updated.isActive()).isFalse();
    }
}
