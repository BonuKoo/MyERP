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

    private Long insertCategoryMain(String name) {
        CategoryMain main = CategoryMain.builder().name(name).displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        return main.getId();
    }

    private Long insertCategorySub(Long categoryMainId, String name) {
        CategorySub sub = CategorySub.builder().categoryMainId(categoryMainId).name(name).displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        return sub.getId();
    }

    private Long insertCategorySub() {
        return insertCategorySub(insertCategoryMain("타일/건축용접착제"), "내장타일 접착제");
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
    void findAllAndCountAll_withPaging() {
        Long categorySubId = insertCategorySub();
        for (int i = 1; i <= 3; i++) {
            itemMapper.insert(newItem(categorySubId, "품목" + i));
        }

        List<Item> page = itemMapper.findAll(0, 2, null, null);
        int total = itemMapper.countAll(null, null);

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void findAll_filterByCategorySubId_returnsOnlyThatSub() {
        Long mainId = insertCategoryMain("타일/건축용접착제");
        Long subA = insertCategorySub(mainId, "내장타일 접착제");
        Long subB = insertCategorySub(mainId, "고성능타일 접착제");
        itemMapper.insert(newItem(subA, "세라픽스 PC-7000D"));
        itemMapper.insert(newItem(subA, "세라픽스 PC-7000L"));
        itemMapper.insert(newItem(subB, "세라픽스 PC-9000P"));

        List<Item> items = itemMapper.findAll(0, 20, null, subA);
        int total = itemMapper.countAll(null, subA);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("세라픽스 PC-7000D", "세라픽스 PC-7000L");
        assertThat(total).isEqualTo(2);
    }

    @Test
    void findAll_filterByCategoryMainId_returnsAllSubsUnderThatMain() {
        Long mainA = insertCategoryMain("타일/건축용접착제");
        Long subA1 = insertCategorySub(mainA, "내장타일 접착제");
        Long subA2 = insertCategorySub(mainA, "고성능타일 접착제");
        Long mainB = insertCategoryMain("목공/지물용접착제");
        Long subB1 = insertCategorySub(mainB, "목공용 접착제");
        itemMapper.insert(newItem(subA1, "세라픽스 PC-7000D"));
        itemMapper.insert(newItem(subA2, "세라픽스 PC-9000P"));
        itemMapper.insert(newItem(subB1, "우드본드 705"));

        List<Item> items = itemMapper.findAll(0, 20, mainA, null);
        int total = itemMapper.countAll(mainA, null);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("세라픽스 PC-7000D", "세라픽스 PC-9000P");
        assertThat(total).isEqualTo(2);
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
