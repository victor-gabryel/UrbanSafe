package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Célula agregada do mapa de calor")
public record PontoCalorResponse(
                @Schema(description = "Latitude do centro da célula", example = "-8.045") double latitude,
                @Schema(description = "Longitude do centro da célula", example = "-34.875") double longitude,
                @Schema(description = "Quantidade de ocorrências na célula", example = "12") long quantidade,
                @Schema(description = "quantidade / maior quantidade entre as células retornadas (0 a 1)", example = "0.4") double intensidade) {
}