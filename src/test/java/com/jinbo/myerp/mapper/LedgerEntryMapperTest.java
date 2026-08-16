package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class LedgerEntryMapperTest {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    private Long insertPartner() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("동양건재상사").partnerType(PartnerType.CUSTOMER)
                .active(true).createdAt(now).updatedAt(now).build();
        partnerMapper.insert(partner);
        return partner.getId();
    }

    private Long insertUser() {
        CompanyUser user = CompanyUser.builder().email("owner@myerp.com").password("x").name("Owner")
                .role(UserRole.OWNER).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        companyUserMapper.insert(user);
        return user.getId();
    }

    @Test
    void insertAndFindByPartnerId() {
        Long partnerId = insertPartner();
        Long userId = insertUser();

        LedgerEntry entry = LedgerEntry.builder()
                .partnerId(partnerId)
                .ledgerType(LedgerType.RECEIVABLE)
                .changeType(LedgerChangeType.SALE_CONFIRMED)
                .amount(new BigDecimal("300000.00"))
                .balanceAfter(new BigDecimal("300000.00"))
                .relatedDocumentType("SALE")
                .relatedDocumentId(1L)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        ledgerEntryMapper.insert(entry);
        assertThat(entry.getId()).isNotNull();

        List<LedgerEntry> found = ledgerEntryMapper.findByPartnerId(partnerId, 0, 10);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getLedgerType()).isEqualTo(LedgerType.RECEIVABLE);
        assertThat(found.get(0).getChangeType()).isEqualTo(LedgerChangeType.SALE_CONFIRMED);
        assertThat(found.get(0).getBalanceAfter()).isEqualByComparingTo("300000.00");
    }

    @Test
    void findByPartnerId_ordersByCreatedAtDescWithPaging() {
        Long partnerId = insertPartner();
        Long userId = insertUser();

        for (int i = 1; i <= 3; i++) {
            LedgerEntry entry = LedgerEntry.builder()
                    .partnerId(partnerId)
                    .ledgerType(LedgerType.RECEIVABLE)
                    .changeType(LedgerChangeType.SALE_CONFIRMED)
                    .amount(BigDecimal.valueOf(i))
                    .balanceAfter(BigDecimal.valueOf(i))
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now().plusSeconds(i))
                    .build();
            ledgerEntryMapper.insert(entry);
        }

        List<LedgerEntry> page = ledgerEntryMapper.findByPartnerId(partnerId, 0, 2);
        int total = ledgerEntryMapper.countByPartnerId(partnerId);

        assertThat(page).hasSize(2);
        assertThat(page.get(0).getBalanceAfter()).isEqualByComparingTo("3");
        assertThat(total).isEqualTo(3);
    }
}
