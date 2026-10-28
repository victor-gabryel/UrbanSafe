package br.com.urbansafe.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ocorrências da área consultada")
public record MapaOcorrenciasResponse(
        List<MapaOcorrenciaResponse> itens,
        @Schema(description = "Máximo de itens retornados nesta consulta", example = "500") int limite,
        @Schema(description = "true se havia mais ocorrências na área do que o limite; reduza a área ou aplique filtros")
        boolean truncado) {
}
