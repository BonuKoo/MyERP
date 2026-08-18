package com.jinbo.myerp.exception;

public class ItemImageNotFoundException extends RuntimeException {

    public ItemImageNotFoundException(Long id) {
        super("품목 사진을 찾을 수 없습니다: id=" + id);
    }
}
