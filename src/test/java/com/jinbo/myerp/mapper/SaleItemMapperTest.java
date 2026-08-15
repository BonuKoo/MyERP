package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.SaleStatus;
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
class SaleItemMapperTest {

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
    private SaleMapper saleMapper;

    @Autowired
    private SaleItemMapper saleItemMapper;

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
                .currentStock(50).safetyStock(10).active(true).version(0).createdAt(now).updatedAt(now).build();
        itemSpecMapper.insert(spec);
        return spec.getId();
    }

    private Long insertSale() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("서울건재유통").partnerType(PartnerType.CUSTOMER).active(true).createdAt(now).updatedAt(now).build();
        partnerMapper.insert(partner);
        CompanyInfo companyInfo = CompanyInfo.builder().companyName("진보상사").createdAt(now).build();
        companyInfoMapper.insert(companyInfo);
        CompanyUser user = CompanyUser.builder().email("owner@myerp.com").password("x").name("Owner")
                .role(UserRole.OWNER).active(true).createdAt(now).updatedAt(now).build();
        companyUserMapper.insert(user);

        Sale sale = Sale.builder()
                .saleNo("SO-TEST-0001").partnerId(partner.getId()).companyInfoId(companyInfo.getId())
                .saleDate(LocalDate.now()).totalAmount(new BigDecimal("300000.00"))
                .status(SaleStatus.CONFIRMED).createdBy(user.getId()).createdAt(now).build();
        saleMapper.insert(sale);
        return sale.getId();
    }

    @Test
    void insertAndFindBySaleId() {
        Long saleId = insertSale();
        Long itemSpecId = insertItemSpec();

        SaleItem item = SaleItem.builder()
                .saleId(saleId).itemSpecId(itemSpecId)
                .quantity(15).unitPrice(new BigDecimal("20000.00")).amount(new BigDecimal("300000.00"))
                .build();

        saleItemMapper.insert(item);
        assertThat(item.getId()).isNotNull();

        List<SaleItem> found = saleItemMapper.findBySaleId(saleId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getQuantity()).isEqualTo(15);
        assertThat(found.get(0).getAmount()).isEqualByComparingTo("300000.00");
    }
}
