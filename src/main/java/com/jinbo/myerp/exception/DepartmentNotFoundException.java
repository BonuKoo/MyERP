package com.jinbo.myerp.exception;

public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(Long id) {
        super("부서를 찾을 수 없습니다: id=" + id);
    }
}
