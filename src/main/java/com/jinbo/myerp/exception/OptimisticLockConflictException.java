package com.jinbo.myerp.exception;

public class OptimisticLockConflictException extends RuntimeException {

    public OptimisticLockConflictException(Long itemSpecId) {
        super("재고 갱신 충돌이 반복되어 처리에 실패했습니다: itemSpecId=" + itemSpecId);
    }
}
