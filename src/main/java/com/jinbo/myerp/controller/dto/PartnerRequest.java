package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String businessNumber,
        @NotNull PartnerType partnerType,
        @Size(max = 50) String contactName,
        @Size(max = 20) String contactPhone,
        @Size(max = 255) String address
) {
    public Partner toDomain() {
        return Partner.builder()
                .name(name)
                .businessNumber(businessNumber)
                .partnerType(partnerType)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .address(address)
                .build();
    }
}
