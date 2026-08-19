package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PositionMapper {

    void insert(Position position);

    Optional<Position> findById(@Param("id") Long id);

    Optional<Position> findByName(@Param("name") String name);

    List<Position> findAll(@Param("offset") int offset, @Param("size") int size);

    int countAll();

    void update(Position position);
}
