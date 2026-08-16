package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class PartnerMapperTest {

    @Autowired
    private PartnerMapper partnerMapper;

    private Partner newPartner(String name) {
        LocalDateTime now = LocalDateTime.now();
        return Partner.builder()
                .name(name)
                .partnerType(PartnerType.CUSTOMER)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAndFindById() {
        Partner partner = newPartner("동양건재상사");

        partnerMapper.insert(partner);
        assertThat(partner.getId()).isNotNull();

        Optional<Partner> found = partnerMapper.findById(partner.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("동양건재상사");
        assertThat(found.get().getPartnerType()).isEqualTo(PartnerType.CUSTOMER);
    }

    @Test
    void findAllAndCountAll_withPaging() {
        for (int i = 1; i <= 3; i++) {
            partnerMapper.insert(newPartner("거래처" + i));
        }

        List<Partner> page = partnerMapper.findAll(0, 2);
        int total = partnerMapper.countAll();

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void update_changesFields() {
        Partner partner = newPartner("변경전");
        partnerMapper.insert(partner);

        partner.setName("변경후");
        partner.setActive(false);
        partnerMapper.update(partner);

        Partner updated = partnerMapper.findById(partner.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void adjustReceivableBalance_accumulatesAtomically() {
        Partner partner = newPartner("외상거래처");
        partnerMapper.insert(partner);

        partnerMapper.adjustReceivableBalance(partner.getId(), new BigDecimal("300000.00"));
        partnerMapper.adjustReceivableBalance(partner.getId(), new BigDecimal("-100000.00"));

        Partner updated = partnerMapper.findById(partner.getId()).orElseThrow();
        assertThat(updated.getReceivableBalance()).isEqualByComparingTo("200000.00");
    }

    @Test
    void adjustPayableBalance_accumulatesAtomically() {
        Partner partner = newPartner("매입거래처");
        partnerMapper.insert(partner);

        partnerMapper.adjustPayableBalance(partner.getId(), new BigDecimal("500000.00"));
        partnerMapper.adjustPayableBalance(partner.getId(), new BigDecimal("-200000.00"));

        Partner updated = partnerMapper.findById(partner.getId()).orElseThrow();
        assertThat(updated.getPayableBalance()).isEqualByComparingTo("300000.00");
    }
}
