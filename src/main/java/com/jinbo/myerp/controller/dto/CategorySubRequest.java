package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.CategorySub;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategorySubRequest(
        @NotNull Long categoryMainId,
        @NotBlank @Size(max = 50) String name,
        int displayOrder
) {
    public CategorySub toDomain() {
        return CategorySub.builder().categoryMainId(categoryMainId).name(name).displayOrder(displayOrder).build();
    }
}
