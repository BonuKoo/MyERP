package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LedgerEntryMapper {

    void insert(LedgerEntry ledgerEntry);

    List<LedgerEntry> findByPartnerId(@Param("partnerId") Long partnerId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    int countByPartnerId(@Param("partnerId") Long partnerId);
}
