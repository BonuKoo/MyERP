package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.ItemImage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ItemImageResponse(
        @Schema(description = "사진 id. 실제 이미지는 GET /api/items/{itemId}/images/{id} 로 가져온다", example = "10")
        Long id,

        @Schema(description = "사용자가 업로드한 원본 파일명", example = "세라픽스-정면.png")
        String uploadFileName,

        @Schema(description = "MIME type", example = "image/png")
        String fileType,

        @Schema(description = "파일 크기(byte)", example = "482913")
        long fileSize,

        @Schema(description = "표시 순서. 작을수록 앞", example = "0")
        int displayOrder,

        @Schema(description = "목록 카드에 쓰이는 대표 사진 여부. 품목당 최대 1장", example = "true")
        boolean primary,

        LocalDateTime createdAt
) {
    public static ItemImageResponse from(ItemImage image) {
        return new ItemImageResponse(
                image.getId(),
                image.getUploadFileName(),
                image.getFileType(),
                image.getFileSize(),
                image.getDisplayOrder(),
                image.isPrimary(),
                image.getCreatedAt());
    }
}
