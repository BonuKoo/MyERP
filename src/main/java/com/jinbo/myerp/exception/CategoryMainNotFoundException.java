package com.jinbo.myerp.exception;

public class CategoryMainNotFoundException extends RuntimeException {

    public CategoryMainNotFoundException(Long id) {
        super("대분류 카테고리를 찾을 수 없습니다: id=" + id);
    }
}
