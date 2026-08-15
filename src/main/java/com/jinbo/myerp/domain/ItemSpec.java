package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSpec {

    private Long id;
    private Long itemId;
    private String specName;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private int currentStock;
    private int safetyStock;
    private boolean active;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
