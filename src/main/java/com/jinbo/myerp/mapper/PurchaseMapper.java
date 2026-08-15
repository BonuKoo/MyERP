package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Purchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PurchaseMapper {

    void insert(Purchase purchase);

    Optional<Purchase> findById(@Param("id") Long id);

    List<Purchase> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void updateStatus(Purchase purchase);
}
