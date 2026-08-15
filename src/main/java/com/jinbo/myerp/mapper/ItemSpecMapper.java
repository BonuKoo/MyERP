package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.ItemSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemSpecMapper {

    void insert(ItemSpec itemSpec);

    Optional<ItemSpec> findById(@Param("id") Long id);

    List<ItemSpec> findByItemId(@Param("itemId") Long itemId);

    void update(ItemSpec itemSpec);
}
