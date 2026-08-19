package com.jinbo.myerp.exception;

public class LeaveRequestNotFoundException extends RuntimeException {
    public LeaveRequestNotFoundException(Long id) {
        super("휴가 신청을 찾을 수 없습니다: id=" + id);
    }
}
