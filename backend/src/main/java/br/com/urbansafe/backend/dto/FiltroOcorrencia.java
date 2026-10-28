package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.util.BoundingBox;

/**
 * Filtros já parseados e validados pelo serviço (bbox, intervalo de datas).
 * Todos opcionais.
 */
public record FiltroOcorrencia(
                BoundingBox bbox,
                Integer tipoId,
                OffsetDateTime dataInicio,
                OffsetDateTime dataFim,
                StatusOcorrencia status,
                boolean apenasMinhas) {
}