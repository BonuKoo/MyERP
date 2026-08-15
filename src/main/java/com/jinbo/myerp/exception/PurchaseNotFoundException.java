package com.jinbo.myerp.exception;

public class PurchaseNotFoundException extends RuntimeException {

    public PurchaseNotFoundException(Long id) {
        super("매입 전표를 찾을 수 없습니다: id=" + id);
    }
}
