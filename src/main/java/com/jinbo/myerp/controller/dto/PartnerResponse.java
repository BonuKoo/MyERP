package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartnerResponse(
        Long id,
        String name,
        String businessNumber,
        PartnerType partnerType,
        String contactName,
        String contactPhone,
        String address,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal receivableBalance,
        BigDecimal payableBalance
) {
    public static PartnerResponse from(Partner partner) {
        return new PartnerResponse(
                partner.getId(), partner.getName(), partner.getBusinessNumber(), partner.getPartnerType(),
                partner.getContactName(), partner.getContactPhone(), partner.getAddress(),
                partner.isActive(), partner.getCreatedAt(), partner.getUpdatedAt(),
                partner.getReceivableBalance(), partner.getPayableBalance());
    }
}
