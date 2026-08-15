package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class CategorySubMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    private Long insertCategoryMain() {
        CategoryMain main = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        return main.getId();
    }

    @Test
    void insertAndFindById() {
        Long mainId = insertCategoryMain();
        CategorySub sub = CategorySub.builder().categoryMainId(mainId).name("내장타일 접착제").displayOrder(1).active(true).build();

        categorySubMapper.insert(sub);
        assertThat(sub.getId()).isNotNull();

        Optional<CategorySub> found = categorySubMapper.findById(sub.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("내장타일 접착제");
        assertThat(found.get().getCategoryMainId()).isEqualTo(mainId);
    }

    @Test
    void findByCategoryMainId_returnsOnlyMatching() {
        Long mainId = insertCategoryMain();
        categorySubMapper.insert(CategorySub.builder().categoryMainId(mainId).name("내장타일 접착제").displayOrder(1).active(true).build());
        categorySubMapper.insert(CategorySub.builder().categoryMainId(mainId).name("에폭시 접착제").displayOrder(2).active(true).build());

        List<CategorySub> subs = categorySubMapper.findByCategoryMainId(mainId);

        assertThat(subs).hasSize(2);
        assertThat(subs).extracting(CategorySub::getName).containsExactly("내장타일 접착제", "에폭시 접착제");
    }

    @Test
    void update_changesName() {
        Long mainId = insertCategoryMain();
        CategorySub sub = CategorySub.builder().categoryMainId(mainId).name("변경전").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);

        sub.setName("변경후");
        categorySubMapper.update(sub);

        CategorySub updated = categorySubMapper.findById(sub.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
    }
}
