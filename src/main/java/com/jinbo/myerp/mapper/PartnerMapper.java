package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Partner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface PartnerMapper {

    void insert(Partner partner);

    Optional<Partner> findById(@Param("id") Long id);

    List<Partner> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void update(Partner partner);

    void adjustReceivableBalance(@Param("partnerId") Long partnerId, @Param("delta") BigDecimal delta);

    void adjustPayableBalance(@Param("partnerId") Long partnerId, @Param("delta") BigDecimal delta);
}
