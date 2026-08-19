package com.jinbo.myerp.exception;

public class EmployeeAlreadyLinkedException extends RuntimeException {
    public EmployeeAlreadyLinkedException(Long companyUserId) {
        super("이미 다른 사원과 연결된 계정입니다: companyUserId=" + companyUserId);
    }
}
