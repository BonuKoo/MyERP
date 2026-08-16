package com.jinbo.myerp.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("결제 전표를 찾을 수 없습니다: id=" + id);
    }
}
