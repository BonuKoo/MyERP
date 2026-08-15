package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CompanyInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyInfoRequest(
        @NotBlank @Size(max = 100) String companyName,
        @Size(max = 20) String businessNumber,
        @Size(max = 50) String ceoName,
        @Size(max = 255) String address,
        @Size(max = 20) String phone
) {
    public CompanyInfo toDomain() {
        return CompanyInfo.builder()
                .companyName(companyName)
                .businessNumber(businessNumber)
                .ceoName(ceoName)
                .address(address)
                .phone(phone)
                .build();
    }
}
