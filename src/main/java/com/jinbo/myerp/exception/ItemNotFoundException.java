package com.jinbo.myerp.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(Long id) {
        super("품목을 찾을 수 없습니다: id=" + id);
    }
}
