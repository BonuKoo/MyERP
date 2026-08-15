package com.jinbo.myerp.exception;

public class SaleNotFoundException extends RuntimeException {

    public SaleNotFoundException(Long id) {
        super("매출 전표를 찾을 수 없습니다: id=" + id);
    }
}
