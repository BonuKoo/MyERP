package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    private Long id;
    private Long categorySubId;
    private String name;
    private String description;
    private String ksStandard;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
