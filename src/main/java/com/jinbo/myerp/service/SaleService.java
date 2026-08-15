package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;

import java.util.List;

public interface SaleService {

    Sale register(Sale sale, List<SaleItem> items, Long userId);

    Sale findById(Long id);

    List<SaleItem> findItemsBySaleId(Long saleId);

    PageResult<Sale> findAll(int page, int size);

    Sale cancel(Long id, Long userId);
}
