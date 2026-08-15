package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class ItemSpecMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemSpecMapper itemSpecMapper;

    private Long insertItem() {
        CategoryMain main = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("내장타일 접착제").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        LocalDateTime now = LocalDateTime.now();
        Item item = Item.builder().categorySubId(sub.getId()).name("세라픽스 PC-7000D").active(true).createdAt(now).updatedAt(now).build();
        itemMapper.insert(item);
        return item.getId();
    }

    private ItemSpec newSpec(Long itemId, String specName) {
        LocalDateTime now = LocalDateTime.now();
        return ItemSpec.builder()
                .itemId(itemId)
                .specName(specName)
                .unit("BOX")
                .costPrice(new BigDecimal("15000.00"))
                .salePrice(new BigDecimal("20000.00"))
                .currentStock(0)
                .safetyStock(10)
                .active(true)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAndFindById() {
        Long itemId = insertItem();
        ItemSpec spec = newSpec(itemId, "20kg");

        itemSpecMapper.insert(spec);
        assertThat(spec.getId()).isNotNull();

        Optional<ItemSpec> found = itemSpecMapper.findById(spec.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSpecName()).isEqualTo("20kg");
        assertThat(found.get().getSalePrice()).isEqualByComparingTo("20000.00");
        assertThat(found.get().getVersion()).isEqualTo(0);
    }

    @Test
    void findByItemId_returnsOnlyMatching() {
        Long itemId = insertItem();
        itemSpecMapper.insert(newSpec(itemId, "20kg"));
        itemSpecMapper.insert(newSpec(itemId, "10kg"));

        List<ItemSpec> specs = itemSpecMapper.findByItemId(itemId);

        assertThat(specs).hasSize(2);
    }

    @Test
    void update_changesStockAndVersion() {
        Long itemId = insertItem();
        ItemSpec spec = newSpec(itemId, "20kg");
        itemSpecMapper.insert(spec);

        spec.setCurrentStock(50);
        spec.setVersion(spec.getVersion() + 1);
        itemSpecMapper.update(spec);

        ItemSpec updated = itemSpecMapper.findById(spec.getId()).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualTo(50);
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void findByIdForUpdate_returnsRow() {
        Long itemId = insertItem();
        ItemSpec spec = newSpec(itemId, "20kg");
        spec.setCurrentStock(30);
        itemSpecMapper.insert(spec);

        Optional<ItemSpec> found = itemSpecMapper.findByIdForUpdate(spec.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCurrentStock()).isEqualTo(30);
    }

    @Test
    void updateStockOptimistic_succeedsWhenVersionMatches() {
        Long itemId = insertItem();
        ItemSpec spec = newSpec(itemId, "20kg");
        spec.setCurrentStock(30);
        itemSpecMapper.insert(spec);

        int affected = itemSpecMapper.updateStockOptimistic(spec.getId(), 10, 0, LocalDateTime.now());

        assertThat(affected).isEqualTo(1);
        ItemSpec updated = itemSpecMapper.findById(spec.getId()).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualTo(10);
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void updateStockOptimistic_returnsZeroWhenVersionStale() {
        Long itemId = insertItem();
        ItemSpec spec = newSpec(itemId, "20kg");
        spec.setCurrentStock(30);
        itemSpecMapper.insert(spec);

        int affected = itemSpecMapper.updateStockOptimistic(spec.getId(), 10, 99, LocalDateTime.now());

        assertThat(affected).isEqualTo(0);
        ItemSpec unchanged = itemSpecMapper.findById(spec.getId()).orElseThrow();
        assertThat(unchanged.getCurrentStock()).isEqualTo(30);
        assertThat(unchanged.getVersion()).isEqualTo(0);
    }
}
