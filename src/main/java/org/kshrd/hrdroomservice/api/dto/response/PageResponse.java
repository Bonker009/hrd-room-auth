package org.kshrd.hrdroomservice.api.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return PageResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }
}
