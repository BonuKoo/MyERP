package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseStatus;
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
class PurchaseMapperTest {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private PurchaseMapper purchaseMapper;

    private Long insertPartner() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("동양건재상사").partnerType(PartnerType.SUPPLIER)
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

    private Purchase newPurchase(Long partnerId, Long companyInfoId, Long userId, String purchaseNo) {
        return Purchase.builder()
                .purchaseNo(purchaseNo)
                .partnerId(partnerId)
                .companyInfoId(companyInfoId)
                .purchaseDate(LocalDate.now())
                .totalAmount(new BigDecimal("300000.00"))
                .status(PurchaseStatus.CONFIRMED)
                .memo("테스트 매입")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        Purchase purchase = newPurchase(partnerId, companyInfoId, userId, "PO-TEST-0001");

        purchaseMapper.insert(purchase);
        assertThat(purchase.getId()).isNotNull();

        Optional<Purchase> found = purchaseMapper.findById(purchase.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPurchaseNo()).isEqualTo("PO-TEST-0001");
        assertThat(found.get().getStatus()).isEqualTo(PurchaseStatus.CONFIRMED);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo("300000.00");
    }

    @Test
    void findAllAndCountAll_withPaging() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        for (int i = 1; i <= 3; i++) {
            purchaseMapper.insert(newPurchase(partnerId, companyInfoId, userId, "PO-TEST-000" + i));
        }

        List<Purchase> page = purchaseMapper.findAll(0, 2);
        int total = purchaseMapper.countAll();

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void updateStatus_changesToCanceledWithTimestamp() {
        Long partnerId = insertPartner();
        Long companyInfoId = insertCompanyInfo();
        Long userId = insertUser();
        Purchase purchase = newPurchase(partnerId, companyInfoId, userId, "PO-TEST-0001");
        purchaseMapper.insert(purchase);

        purchase.setStatus(PurchaseStatus.CANCELED);
        purchase.setCanceledAt(LocalDateTime.now());
        purchaseMapper.updateStatus(purchase);

        Purchase updated = purchaseMapper.findById(purchase.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PurchaseStatus.CANCELED);
        assertThat(updated.getCanceledAt()).isNotNull();
    }
}
