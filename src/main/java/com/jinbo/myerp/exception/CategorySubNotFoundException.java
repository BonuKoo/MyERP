package com.jinbo.myerp.exception;

public class CategorySubNotFoundException extends RuntimeException {

    public CategorySubNotFoundException(Long id) {
        super("중분류 카테고리를 찾을 수 없습니다: id=" + id);
    }
}
