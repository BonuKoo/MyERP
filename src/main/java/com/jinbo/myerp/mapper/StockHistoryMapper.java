package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.StockHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockHistoryMapper {

    void insert(StockHistory stockHistory);

    List<StockHistory> findByItemSpecId(@Param("itemSpecId") Long itemSpecId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    int countByItemSpecId(@Param("itemSpecId") Long itemSpecId);
}
