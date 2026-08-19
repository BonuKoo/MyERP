package com.jinbo.myerp.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("사원을 찾을 수 없습니다: id=" + id);
    }

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
