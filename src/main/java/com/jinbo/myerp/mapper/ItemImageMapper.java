package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.ItemImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemImageMapper {

    void insert(ItemImage itemImage);

    Optional<ItemImage> findById(@Param("id") Long id);

    List<ItemImage> findByItemId(@Param("itemId") Long itemId);

    /**
     * 여러 품목의 대표 사진을 한 번에 가져온다. 목록 화면이 품목마다 따로 조회하면
     * N+1이 되기 때문에 필요하다(20건 목록 = 1쿼리).
     */
    List<ItemImage> findPrimaryByItemIds(@Param("itemIds") List<Long> itemIds);

    Integer findMaxDisplayOrder(@Param("itemId") Long itemId);

    void clearPrimary(@Param("itemId") Long itemId);

    void markPrimary(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    void deleteByItemId(@Param("itemId") Long itemId);
}
