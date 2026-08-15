package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CategoryMain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryMainRequest(
        @NotBlank @Size(max = 50) String name,
        int displayOrder
) {
    public CategoryMain toDomain() {
        return CategoryMain.builder().name(name).displayOrder(displayOrder).build();
    }
}
