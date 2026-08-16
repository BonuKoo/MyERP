package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentStatus;
import com.jinbo.myerp.domain.PaymentType;
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
class PaymentMapperTest {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private PaymentMapper paymentMapper;

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

    private Payment newPayment(Long partnerId, Long userId, String paymentNo) {
        return Payment.builder()
                .paymentNo(paymentNo)
                .partnerId(partnerId)
                .paymentType(PaymentType.RECEIPT)
                .amount(new BigDecimal("100000.00"))
                .paymentDate(LocalDate.now())
                .method("BANK_TRANSFER")
                .memo("테스트 수금")
                .status(PaymentStatus.CONFIRMED)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        Long partnerId = insertPartner();
        Long userId = insertUser();
        Payment payment = newPayment(partnerId, userId, "PM-TEST-0001");

        paymentMapper.insert(payment);
        assertThat(payment.getId()).isNotNull();

        Optional<Payment> found = paymentMapper.findById(payment.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentNo()).isEqualTo("PM-TEST-0001");
        assertThat(found.get().getPaymentType()).isEqualTo(PaymentType.RECEIPT);
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(found.get().getAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void findAllAndCountAll_withPaging() {
        Long partnerId = insertPartner();
        Long userId = insertUser();
        for (int i = 1; i <= 3; i++) {
            paymentMapper.insert(newPayment(partnerId, userId, "PM-TEST-000" + i));
        }

        List<Payment> page = paymentMapper.findAll(0, 2);
        int total = paymentMapper.countAll();

        assertThat(page).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void updateStatus_changesToCanceledWithTimestamp() {
        Long partnerId = insertPartner();
        Long userId = insertUser();
        Payment payment = newPayment(partnerId, userId, "PM-TEST-0001");
        paymentMapper.insert(payment);

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCanceledAt(LocalDateTime.now());
        paymentMapper.updateStatus(payment);

        Payment updated = paymentMapper.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(updated.getCanceledAt()).isNotNull();
    }
}
