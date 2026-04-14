package com.aigrs.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {
    private List<T> content;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private Boolean isLast;
    private Boolean isFirst;
    
    public static <T> PaginatedResponse<T> of(List<T> content, Integer pageNumber, 
                                               Integer pageSize, Long totalElements) {
        Integer totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return PaginatedResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(pageNumber >= totalPages - 1)
                .isFirst(pageNumber == 0)
                .build();
    }
}
