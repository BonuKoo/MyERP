package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CategoryMain;

public record CategoryMainResponse(Long id, String name, int displayOrder, boolean active) {
    public static CategoryMainResponse from(CategoryMain categoryMain) {
        return new CategoryMainResponse(
                categoryMain.getId(), categoryMain.getName(), categoryMain.getDisplayOrder(), categoryMain.isActive());
    }
}
