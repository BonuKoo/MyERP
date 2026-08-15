package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseItem;
import com.jinbo.myerp.domain.PurchaseStatus;
import com.jinbo.myerp.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class PurchaseItemMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemSpecMapper itemSpecMapper;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private PurchaseMapper purchaseMapper;

    @Autowired
    private PurchaseItemMapper purchaseItemMapper;

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

    private Long insertPurchase() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("동양건재상사").partnerType(PartnerType.SUPPLIER).active(true).createdAt(now).updatedAt(now).build();
        partnerMapper.insert(partner);
        CompanyInfo companyInfo = CompanyInfo.builder().companyName("진보상사").createdAt(now).build();
        companyInfoMapper.insert(companyInfo);
        CompanyUser user = CompanyUser.builder().email("owner@myerp.com").password("x").name("Owner")
                .role(UserRole.OWNER).active(true).createdAt(now).updatedAt(now).build();
        companyUserMapper.insert(user);

        Purchase purchase = Purchase.builder()
                .purchaseNo("PO-TEST-0001").partnerId(partner.getId()).companyInfoId(companyInfo.getId())
                .purchaseDate(LocalDate.now()).totalAmount(new BigDecimal("300000.00"))
                .status(PurchaseStatus.CONFIRMED).createdBy(user.getId()).createdAt(now).build();
        purchaseMapper.insert(purchase);
        return purchase.getId();
    }

    @Test
    void insertAndFindByPurchaseId() {
        Long purchaseId = insertPurchase();
        Long itemSpecId = insertItemSpec();

        PurchaseItem item = PurchaseItem.builder()
                .purchaseId(purchaseId).itemSpecId(itemSpecId)
                .quantity(15).unitPrice(new BigDecimal("15000.00")).amount(new BigDecimal("225000.00"))
                .build();

        purchaseItemMapper.insert(item);
        assertThat(item.getId()).isNotNull();

        List<PurchaseItem> found = purchaseItemMapper.findByPurchaseId(purchaseId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getQuantity()).isEqualTo(15);
        assertThat(found.get(0).getAmount()).isEqualByComparingTo("225000.00");
    }
}
