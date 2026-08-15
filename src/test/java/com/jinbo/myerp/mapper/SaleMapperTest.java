package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleStatus;
import com.jinbo.myerp.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class SaleMapperTest {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private SaleMapper saleMapper;

    private Long insertPartner() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("서울건재유통").partnerType(PartnerType.CUSTOMER)
                .active(true).createdAt(now).updatedAt(now).build();
        partnerMapper.insert(partner);
        return partner.getId();
    }

    private Long insertCompanyInfo() {
        CompanyInfo companyInfo = CompanyInfo.builder().companyName("진보상사").createdAt(LocalDateTime.now()).build();
        companyInfoMapper.insert(companyInfo);
        return companyInfo.getId();
    }

    private Long insertUser() {
        CompanyUser user = CompanyUser.builder().email("owner@myerp.com").password("x").name("Owner")
                .role(UserRole.OWNER).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        companyUserMapper.insert(user);
        return user.getId();
    }

    private Sale newSale(Long partnerId, Long companyInfoId, Long userId, String saleNo) {
        return Sale.builder()
                .saleNo(saleNo)
                .partnerId(partnerId)
                .companyInfoId(companyInfoId)
                .saleDate(LocalDate.now())
                .totalAmount(new BigDecimal("300000.00"))
                .status(SaleStatus.CONFIRMED)
                .memo("테스트 매출")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        Sale sale = newSale(partnerId, companyInfoId, userId, "SO-TEST-0001");

        saleMapper.insert(sale);
        assertThat(sale.getId()).isNotNull();

        Optional<Sale> found = saleMapper.findById(sale.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSaleNo()).isEqualTo("SO-TEST-0001");
        assertThat(found.get().getStatus()).isEqualTo(SaleStatus.CONFIRMED);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo("300000.00");
    }

    @Test
    void findAllAndCountAll_withPaging() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        for (int i = 1; i <= 3; i++) {
            saleMapper.insert(newSale(partnerId, companyInfoId, userId, "SO-TEST-000" + i));
        }

        List<Sale> page = saleMapper.findAll(0, 2);
        int total = saleMapper.countAll();

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void updateStatus_changesToCanceledWithTimestamp() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        Sale sale = newSale(partnerId, companyInfoId, userId, "SO-TEST-0001");
        saleMapper.insert(sale);

        sale.setStatus(SaleStatus.CANCELED);
        sale.setCanceledAt(LocalDateTime.now());
        saleMapper.updateStatus(sale);

        Sale updated = saleMapper.findById(sale.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SaleStatus.CANCELED);
        assertThat(updated.getCanceledAt()).isNotNull();
    }
}
