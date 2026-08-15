package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.service.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, long totalCount, int page, int size) {

    public static <T, R> PageResponse<R> of(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> content = pageResult.content().stream().map(mapper).toList();
        return new PageResponse<>(content, pageResult.totalCount(), pageResult.page(), pageResult.size());
    }
}
