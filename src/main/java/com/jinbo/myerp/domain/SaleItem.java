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
public class SaleItem {

    private Long id;
    private Long saleId;
    private Long itemSpecId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
