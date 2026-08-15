package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.SaleItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SaleItemMapper {

    void insert(SaleItem saleItem);

    List<SaleItem> findBySaleId(@Param("saleId") Long saleId);
}
