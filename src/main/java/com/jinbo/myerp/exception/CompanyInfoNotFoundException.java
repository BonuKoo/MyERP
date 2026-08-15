package com.jinbo.myerp.exception;

public class CompanyInfoNotFoundException extends RuntimeException {

    public CompanyInfoNotFoundException(Long id) {
        super("회사 정보를 찾을 수 없습니다: id=" + id);
    }
}
