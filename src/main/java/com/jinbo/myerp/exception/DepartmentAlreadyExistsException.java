package com.jinbo.myerp.exception;

public class DepartmentAlreadyExistsException extends RuntimeException {

    public DepartmentAlreadyExistsException(String name) {
        super("이미 등록된 부서명입니다: " + name);
    }
}
