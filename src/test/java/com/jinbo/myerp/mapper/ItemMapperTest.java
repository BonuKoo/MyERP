package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.Item;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class ItemMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    private Long insertCategorySub() {
        CategoryMain main = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("내장타일 접착제").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        return sub.getId();
    }

    private Item newItem(Long categorySubId, String name) {
        LocalDateTime now = LocalDateTime.now();
        return Item.builder()
                .categorySubId(categorySubId)
                .name(name)
                .ksStandard("KS L 1592")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAndFindById() {
        Long categorySubId = insertCategorySub();
        Item item = newItem(categorySubId, "세라픽스 PC-7000D");

        itemMapper.insert(item);
        assertThat(item.getId()).isNotNull();

        Optional<Item> found = itemMapper.findById(item.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("세라픽스 PC-7000D");
        assertThat(found.get().getCategorySubId()).isEqualTo(categorySubId);
    }

    @Test
    void findByCategorySubId_returnsOnlyMatching() {
        Long categorySubId = insertCategorySub();
        itemMapper.insert(newItem(categorySubId, "세라픽스 PC-7000D"));
        itemMapper.insert(newItem(categorySubId, "세라픽스 PC-7000L"));

        List<Item> items = itemMapper.findByCategorySubId(categorySubId);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("세라픽스 PC-7000D", "세라픽스 PC-7000L");
    }

    @Test
    void findAllAndCountAll_withPaging() {
        Long categorySubId = insertCategorySub();
        for (int i = 1; i <= 3; i++) {
            itemMapper.insert(newItem(categorySubId, "품목" + i));
        }

        List<Item> page = itemMapper.findAll(0, 2);
        int total = itemMapper.countAll();

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void update_changesNameAndActive() {
        Long categorySubId = insertCategorySub();
        Item item = newItem(categorySubId, "변경전");
        itemMapper.insert(item);

        item.setName("변경후");
        item.setActive(false);
        itemMapper.update(item);

        Item updated = itemMapper.findById(item.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
        assertThat(updated.isActive()).isFalse();
    }
}
