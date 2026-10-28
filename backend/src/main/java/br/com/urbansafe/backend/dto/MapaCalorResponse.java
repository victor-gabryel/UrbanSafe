package br.com.urbansafe.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados agregados para o mapa de calor")
public record MapaCalorResponse(
        @Schema(description = "Tamanho da célula da grade, em graus", example = "0.005") double celulaGraus,
        long totalOcorrencias,
        List<PontoCalorResponse> pontos,
        @Schema(description = "true se havia mais células do que o limite; apenas as mais densas foram retornadas") boolean truncado,
        String aviso) {

    public static final String AVISO_PADRAO = "A densidade reflete apenas os relatos registrados no sistema; "
            + "não representa a probabilidade real de crime nem garante a segurança de uma região.";
}
