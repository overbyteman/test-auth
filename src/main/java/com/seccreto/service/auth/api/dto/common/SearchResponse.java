package com.seccreto.service.auth.api.dto.common;

/**
 * Record para resposta de busca com informações de timing
 */
public record SearchResponse<T>(
    Pagination<T> pagination,
    long executionTimeMs,
    int itemsCount
) {
    
    public static <T> SearchResponse<T> of(Pagination<T> pagination, long executionTimeMs) {
        return new SearchResponse<>(
            pagination,
            executionTimeMs,
            pagination.items().size()
        );
    }
}

