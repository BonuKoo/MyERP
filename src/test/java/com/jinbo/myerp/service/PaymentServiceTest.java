package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentStatus;
import com.jinbo.myerp.domain.PaymentType;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.PaymentNotFoundException;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PartnerMapper partnerMapper;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment newPaymentRequest(PaymentType type) {
        return Payment.builder()
                .partnerId(1L)
                .paymentType(type)
                .amount(new BigDecimal("100000.00"))
                .paymentDate(LocalDate.now())
                .method("BANK_TRANSFER")
                .memo("테스트")
                .build();
    }

    @Test
    void register_receipt_decreasesReceivableBalance() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(ledgerService.adjustReceivableBalance(1L, new BigDecimal("100000.00").negate()))
                .willReturn(new BigDecimal("200000.00"));

        Payment result = paymentService.register(newPaymentRequest(PaymentType.RECEIPT), 99L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(result.getPaymentNo()).isNotBlank();
        verify(paymentMapper).insert(result);

        InOrder inOrder = inOrder(ledgerService, paymentMapper);
        inOrder.verify(ledgerService).adjustReceivableBalance(1L, new BigDecimal("100000.00").negate());
        inOrder.verify(paymentMapper).insert(result);

        verify(ledgerService).recordEntry(1L, LedgerType.RECEIVABLE, LedgerChangeType.PAYMENT_RECEIVED,
                new BigDecimal("100000.00").negate(), new BigDecimal("200000.00"), "PAYMENT", result.getId(), 99L);
    }

    @Test
    void register_disbursement_decreasesPayableBalance() {
        given(partnerMapper.findById(1L)).willReturn(Optional.of(Partner.builder().id(1L).build()));
        given(ledgerService.adjustPayableBalance(1L, new BigDecimal("100000.00").negate()))
                .willReturn(new BigDecimal("50000.00"));

        Payment result = paymentService.register(newPaymentRequest(PaymentType.DISBURSEMENT), 99L);

        InOrder inOrder = inOrder(ledgerService, paymentMapper);
        inOrder.verify(ledgerService).adjustPayableBalance(1L, new BigDecimal("100000.00").negate());
        inOrder.verify(paymentMapper).insert(result);

        verify(ledgerService).recordEntry(1L, LedgerType.PAYABLE, LedgerChangeType.PAYMENT_PAID,
                new BigDecimal("100000.00").negate(), new BigDecimal("50000.00"), "PAYMENT", result.getId(), 99L);
    }

    @Test
    void register_partnerNotFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.register(newPaymentRequest(PaymentType.RECEIPT), 99L))
                .isInstanceOf(PartnerNotFoundException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(paymentMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(1L))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Payment> payments = List.of(Payment.builder().id(1L).build());
        given(paymentMapper.findAll(0, 20)).willReturn(payments);
        given(paymentMapper.countAll()).willReturn(1);

        PageResult<Payment> result = paymentService.findAll(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void cancel_receipt_reversesReceivableBalance() {
        Payment payment = Payment.builder().id(1L).partnerId(1L).paymentType(PaymentType.RECEIPT)
                .amount(new BigDecimal("100000.00")).status(PaymentStatus.CONFIRMED).build();
        given(paymentMapper.findById(1L)).willReturn(Optional.of(payment));

        Payment result = paymentService.cancel(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
        verify(paymentMapper).updateStatus(payment);
        verify(ledgerService).recordReceivableChange(1L, LedgerChangeType.PAYMENT_RECEIVED_CANCELED,
                new BigDecimal("100000.00"), "PAYMENT", 1L, 99L);
    }

    @Test
    void cancel_disbursement_reversesPayableBalance() {
        Payment payment = Payment.builder().id(1L).partnerId(1L).paymentType(PaymentType.DISBURSEMENT)
                .amount(new BigDecimal("100000.00")).status(PaymentStatus.CONFIRMED).build();
        given(paymentMapper.findById(1L)).willReturn(Optional.of(payment));

        paymentService.cancel(1L, 99L);

        verify(ledgerService).recordPayableChange(1L, LedgerChangeType.PAYMENT_PAID_CANCELED,
                new BigDecimal("100000.00"), "PAYMENT", 1L, 99L);
    }

    @Test
    void cancel_alreadyCanceled_throws() {
        Payment payment = Payment.builder().id(1L).status(PaymentStatus.CANCELED).build();
        given(paymentMapper.findById(1L)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancel(1L, 99L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
