package com.jinbo.myerp.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long itemSpecId, int currentStock, int requestedChange) {
        super("재고가 부족합니다: itemSpecId=" + itemSpecId
                + ", 현재재고=" + currentStock + ", 요청변동량=" + requestedChange);
    }
}
