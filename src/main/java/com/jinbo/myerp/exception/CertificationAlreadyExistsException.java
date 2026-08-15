package com.jinbo.myerp.exception;

public class CertificationAlreadyExistsException extends RuntimeException {

    public CertificationAlreadyExistsException(String name) {
        super("이미 등록된 인증정보입니다: " + name);
    }
}
