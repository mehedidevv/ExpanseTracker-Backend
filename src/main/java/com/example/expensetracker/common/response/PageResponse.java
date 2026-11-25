package com.example.expensetracker.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Stable paged response shape, independent of Spring's Page JSON. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
