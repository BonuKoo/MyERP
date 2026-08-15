package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SaleMapper {

    void insert(Sale sale);

    Optional<Sale> findById(@Param("id") Long id);

    List<Sale> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void updateStatus(Sale sale);
}
