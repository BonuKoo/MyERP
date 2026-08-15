package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;

import java.time.LocalDateTime;
import java.util.List;

public record ItemResponse(
        Long id,
        Long categorySubId,
        String name,
        String description,
        String ksStandard,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CertificationResponse> certifications
) {
    public static ItemResponse from(Item item, List<Certification> certifications) {
        return new ItemResponse(
                item.getId(), item.getCategorySubId(), item.getName(), item.getDescription(), item.getKsStandard(),
                item.isActive(), item.getCreatedAt(), item.getUpdatedAt(),
                certifications.stream().map(CertificationResponse::from).toList());
    }
}
