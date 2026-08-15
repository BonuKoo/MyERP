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
public class StockHistory {

    private Long id;
    private Long itemSpecId;
    private StockChangeType changeType;
    private int quantity;
    private int beforeStock;
    private int afterStock;
    private String relatedDocumentType;
    private Long relatedDocumentId;
    private Long createdBy;
    private LocalDateTime createdAt;
}
