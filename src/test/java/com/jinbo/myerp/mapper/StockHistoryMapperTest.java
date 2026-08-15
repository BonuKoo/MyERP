package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class StockHistoryMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemSpecMapper itemSpecMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private StockHistoryMapper stockHistoryMapper;

    private Long insertItemSpec() {
        CategoryMain main = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("내장타일 접착제").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        LocalDateTime now = LocalDateTime.now();
        Item item = Item.builder().categorySubId(sub.getId()).name("세라픽스 PC-7000D").active(true).createdAt(now).updatedAt(now).build();
        itemMapper.insert(item);
        ItemSpec spec = ItemSpec.builder().itemId(item.getId()).specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("15000")).salePrice(new BigDecimal("20000"))
                .currentStock(0).safetyStock(10).active(true).version(0).createdAt(now).updatedAt(now).build();
        itemSpecMapper.insert(spec);
        return spec.getId();
    }

    private Long insertUser() {
        CompanyUser user = CompanyUser.builder().email("owner@myerp.com").password("x").name("Owner")
                .role(UserRole.OWNER).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        companyUserMapper.insert(user);
        return user.getId();
    }

    @Test
    void insertAndFindByItemSpecId() {
        Long itemSpecId = insertItemSpec();
        Long userId = insertUser();

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.ADJUST)
                .quantity(50)
                .beforeStock(0)
                .afterStock(50)
                .relatedDocumentType("MANUAL")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        stockHistoryMapper.insert(history);
        assertThat(history.getId()).isNotNull();

        List<StockHistory> found = stockHistoryMapper.findByItemSpecId(itemSpecId, 0, 10);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(found.get(0).getAfterStock()).isEqualTo(50);
    }

    @Test
    void findByItemSpecId_ordersByCreatedAtDesc() {
        Long itemSpecId = insertItemSpec();
        Long userId = insertUser();

        for (int i = 1; i <= 3; i++) {
            StockHistory history = StockHistory.builder()
                    .itemSpecId(itemSpecId).changeType(StockChangeType.ADJUST)
                    .quantity(i).beforeStock(0).afterStock(i)
                    .createdBy(userId).createdAt(LocalDateTime.now().plusSeconds(i))
                    .build();
            stockHistoryMapper.insert(history);
        }

        List<StockHistory> page = stockHistoryMapper.findByItemSpecId(itemSpecId, 0, 2);
        int total = stockHistoryMapper.countByItemSpecId(itemSpecId);

        assertThat(page).hasSize(2);
        assertThat(page.get(0).getAfterStock()).isEqualTo(3);
        assertThat(total).isEqualTo(3);
    }
}
