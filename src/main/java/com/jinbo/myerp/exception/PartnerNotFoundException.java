package com.jinbo.myerp.exception;

public class PartnerNotFoundException extends RuntimeException {

    public PartnerNotFoundException(Long id) {
        super("거래처를 찾을 수 없습니다: id=" + id);
    }
}
