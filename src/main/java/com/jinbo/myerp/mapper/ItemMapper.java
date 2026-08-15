package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemMapper {

    void insert(Item item);

    Optional<Item> findById(@Param("id") Long id);

    List<Item> findAll(@Param("offset") int offset, @Param("size") int size);

    List<Item> findByCategorySubId(@Param("categorySubId") Long categorySubId);

    int countAll();

    void update(Item item);
}
