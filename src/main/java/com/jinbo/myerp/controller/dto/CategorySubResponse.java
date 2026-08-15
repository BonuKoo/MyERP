package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CategorySub;

public record CategorySubResponse(Long id, Long categoryMainId, String name, int displayOrder, boolean active) {
    public static CategorySubResponse from(CategorySub categorySub) {
        return new CategorySubResponse(
                categorySub.getId(), categorySub.getCategoryMainId(), categorySub.getName(),
                categorySub.getDisplayOrder(), categorySub.isActive());
    }
}
