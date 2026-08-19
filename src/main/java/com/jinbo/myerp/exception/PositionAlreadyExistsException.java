package com.jinbo.myerp.exception;

public class PositionAlreadyExistsException extends RuntimeException {
    public PositionAlreadyExistsException(String name) {
        super("이미 등록된 직책명입니다: " + name);
    }
}
