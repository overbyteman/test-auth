package com.seccreto.service.auth.api.dto.common;

/**
 * Record para parâmetros de busca paginada
 */
public record SearchQuery(
    int page,
    int perPage,
    String terms,
    String sort,
    String direction
) {
}
