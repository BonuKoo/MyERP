package com.jinbo.myerp.exception;

public class CompanyUserNotFoundException extends RuntimeException {

    public CompanyUserNotFoundException(Long id) {
        super("사용자를 찾을 수 없습니다: id=" + id);
    }
}
