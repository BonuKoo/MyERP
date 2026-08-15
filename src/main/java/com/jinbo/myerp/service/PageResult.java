package com.jinbo.myerp.service;

import java.util.List;

public record PageResult<T>(List<T> content, long totalCount, int page, int size) {
}
