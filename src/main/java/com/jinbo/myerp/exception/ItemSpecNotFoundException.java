package com.jinbo.myerp.exception;

public class ItemSpecNotFoundException extends RuntimeException {

    public ItemSpecNotFoundException(Long id) {
        super("규격을 찾을 수 없습니다: id=" + id);
    }
}
