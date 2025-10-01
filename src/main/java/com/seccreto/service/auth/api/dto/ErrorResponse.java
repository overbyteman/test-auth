package com.seccreto.service.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Schema(description = "Modelo padrão de erro retornado pela API")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Schema(description = "Momento do erro em UTC", example = "2024-01-30T12:34:56.789Z")
    private final Instant timestamp = Instant.now();
    @Schema(description = "Código HTTP", example = "400")
    private int status;
    @Schema(description = "Motivo resumido", example = "Bad Request")
    private String error;
    @Schema(description = "Mensagem detalhada")
    private String message;
    @Schema(description = "Caminho da requisição", example = "/api/users")
    private String path;
    @Schema(description = "Lista de detalhes (ex: campos inválidos)")
    private List<String> details;
}

