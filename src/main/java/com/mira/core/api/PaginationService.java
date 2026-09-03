package com.mira.core.api;

import java.util.List;

public interface PaginationService {
    <T> Page<T> page(List<T> values, int requestedPage, int pageSize);

    record Page<T>(List<T> values, int page, int pages, int total, int pageSize) {
        public boolean hasPrevious() { return page > 1; }
        public boolean hasNext() { return page < pages; }
    }
}
