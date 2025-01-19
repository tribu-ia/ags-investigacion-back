package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaginatedResponse<T> {
    private List<T> data;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PaginatedResponse<T> of(List<T> data, long total, int page, int pageSize) {
        return PaginatedResponse.<T>builder()
            .data(data)
            .total(total)
            .page(page)
            .pageSize(pageSize)
            .build();
    }
} 