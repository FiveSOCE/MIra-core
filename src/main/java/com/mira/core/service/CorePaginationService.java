package com.mira.core.service;

import com.mira.core.api.PaginationService;

import java.util.List;

public final class CorePaginationService implements PaginationService {
    @Override
    public <T> Page<T> page(List<T> values, int requestedPage, int pageSize) {
        values = values == null ? List.of() : values;
        pageSize = Math.max(1, pageSize);
        int pages = Math.max(1, (values.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(requestedPage, pages));
        int start = Math.min(values.size(), (page - 1) * pageSize);
        int end = Math.min(values.size(), start + pageSize);
        return new Page<>(List.copyOf(values.subList(start, end)), page, pages, values.size(), pageSize);
    }
}
