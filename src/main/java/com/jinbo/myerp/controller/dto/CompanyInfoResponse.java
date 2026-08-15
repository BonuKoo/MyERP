package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CompanyInfo;

import java.time.LocalDateTime;

public record CompanyInfoResponse(
        Long id,
        String companyName,
        String businessNumber,
        String ceoName,
        String address,
        String phone,
        LocalDateTime createdAt
) {
    public static CompanyInfoResponse from(CompanyInfo companyInfo) {
        return new CompanyInfoResponse(
                companyInfo.getId(), companyInfo.getCompanyName(), companyInfo.getBusinessNumber(),
                companyInfo.getCeoName(), companyInfo.getAddress(), companyInfo.getPhone(), companyInfo.getCreatedAt());
    }
}
