package com.jinbo.myerp.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("이미 등록된 이메일입니다: " + email);
    }
}
