package org.kshrd.hrdroomservice.api.dto.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content, long totalElements, int page, int size, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageResponse<>(content, totalElements, page, size, totalPages);
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages());
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return of(page.map(mapper));
    }
}
