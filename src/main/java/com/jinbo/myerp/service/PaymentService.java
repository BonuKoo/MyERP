package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentStatus;
import com.jinbo.myerp.domain.PaymentType;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.PaymentNotFoundException;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PartnerMapper partnerMapper;
    private final LedgerService ledgerService;

    @Transactional
    public Payment register(Payment payment, Long userId) {
        partnerMapper.findById(payment.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException(payment.getPartnerId()));

        payment.setPaymentNo("PM" + System.currentTimeMillis());
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setCreatedBy(userId);
        payment.setCreatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        recordLedger(payment, payment.getAmount().negate(),
                payment.getPaymentType() == PaymentType.RECEIPT
                        ? LedgerChangeType.PAYMENT_RECEIVED
                        : LedgerChangeType.PAYMENT_PAID,
                userId);

        return payment;
    }

    public Payment findById(Long id) {
        return paymentMapper.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    public PageResult<Payment> findAll(int page, int size) {
        int offset = page * size;
        return new PageResult<>(paymentMapper.findAll(offset, size), paymentMapper.countAll(), page, size);
    }

    @Transactional
    public Payment cancel(Long id, Long userId) {
        Payment payment = findById(id);
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new InvalidStatusTransitionException("이미 취소된 결제 전표입니다: id=" + id);
        }

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCanceledAt(LocalDateTime.now());
        paymentMapper.updateStatus(payment);

        recordLedger(payment, payment.getAmount(),
                payment.getPaymentType() == PaymentType.RECEIPT
                        ? LedgerChangeType.PAYMENT_RECEIVED_CANCELED
                        : LedgerChangeType.PAYMENT_PAID_CANCELED,
                userId);

        return payment;
    }

    private void recordLedger(Payment payment, BigDecimal amount, LedgerChangeType changeType, Long userId) {
        if (payment.getPaymentType() == PaymentType.RECEIPT) {
            ledgerService.recordReceivableChange(payment.getPartnerId(), changeType, amount,
                    "PAYMENT", payment.getId(), userId);
        } else {
            ledgerService.recordPayableChange(payment.getPartnerId(), changeType, amount,
                    "PAYMENT", payment.getId(), userId);
        }
    }
}
