package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Certification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemCertificationMapper {

    void insert(@Param("itemId") Long itemId, @Param("certificationId") Long certificationId);

    void deleteByItemId(@Param("itemId") Long itemId);

    List<Certification> findCertificationsByItemId(@Param("itemId") Long itemId);
}
