package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.PurchaseItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PurchaseItemMapper {

    void insert(PurchaseItem purchaseItem);

    List<PurchaseItem> findByPurchaseId(@Param("purchaseId") Long purchaseId);
}
