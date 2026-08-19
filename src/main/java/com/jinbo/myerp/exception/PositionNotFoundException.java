package com.jinbo.myerp.exception;

public class PositionNotFoundException extends RuntimeException {
    public PositionNotFoundException(Long id) {
        super("직책을 찾을 수 없습니다: id=" + id);
    }
}
