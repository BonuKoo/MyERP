package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemImage;
import io.swagger.v3.oas.annotations.media.Schema;

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
        List<CertificationResponse> certifications,

        @Schema(description = "첨부된 사진. 단건 조회는 전체 사진을, 목록 조회는 카드에 쓸 대표 사진 1장만 담는다" +
                "(목록에서 품목마다 전체 사진을 싣는 건 낭비라서). 사진이 없으면 빈 배열")
        List<ItemImageResponse> images
) {
    public static ItemResponse from(Item item, List<Certification> certifications, List<ItemImage> images) {
        return new ItemResponse(
                item.getId(), item.getCategorySubId(), item.getName(), item.getDescription(), item.getKsStandard(),
                item.isActive(), item.getCreatedAt(), item.getUpdatedAt(),
                certifications.stream().map(CertificationResponse::from).toList(),
                images.stream().map(ItemImageResponse::from).toList());
    }
}
