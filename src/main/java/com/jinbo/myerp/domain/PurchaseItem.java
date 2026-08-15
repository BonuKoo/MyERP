package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItem {

    private Long id;
    private Long purchaseId;
    private Long itemSpecId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
