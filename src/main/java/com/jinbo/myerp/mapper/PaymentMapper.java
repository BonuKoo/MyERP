package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PaymentMapper {

    void insert(Payment payment);

    Optional<Payment> findById(@Param("id") Long id);

    List<Payment> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void updateStatus(Payment payment);
}
