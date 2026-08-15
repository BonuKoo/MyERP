package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String businessNumber,
        @NotNull
        @Schema(description = "SUPPLIER=매입처, CUSTOMER=매출처, BOTH=양쪽 다. " +
                "매출 전표 등록 화면에서는 CUSTOMER/BOTH만 선택 대상으로 노출한다.")
        PartnerType partnerType,
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
