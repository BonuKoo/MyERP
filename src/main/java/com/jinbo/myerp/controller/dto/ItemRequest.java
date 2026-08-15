package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemRequest(
        @NotNull Long categorySubId,
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 50) String ksStandard,
        List<Long> certificationIds
) {
    public Item toDomain() {
        return Item.builder()
                .categorySubId(categorySubId)
                .name(name)
                .description(description)
                .ksStandard(ksStandard)
                .build();
    }

    public List<Long> certificationIdsOrEmpty() {
        return certificationIds == null ? List.of() : certificationIds;
    }
}
