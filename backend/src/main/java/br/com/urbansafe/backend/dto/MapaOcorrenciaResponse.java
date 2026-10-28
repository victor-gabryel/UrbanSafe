package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO enxuto do marcador: sem descrição, endereço, autor ou qualquer dado
 * pessoal.
 */
@Schema(description = "Marcador de ocorrência no mapa (sem dados pessoais)")
public record MapaOcorrenciaResponse(
                Integer id,
                Integer tipoId,
                String tipoNome,
                String titulo,
                BigDecimal latitude,
                BigDecimal longitude,
                StatusOcorrencia status,
                Relevancia relevancia,
                OffsetDateTime dataOcorrencia) {
}