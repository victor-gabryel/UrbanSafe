package br.com.urbansafe.backend.util;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.dto.FiltroOcorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;

/** Montagem e validação dos filtros compartilhados por /api/ocorrencias e /api/mapa/**. */
public final class Filtros {

    private Filtros() {
    }

    public static FiltroOcorrencia montar(String bbox, double maxLadoBbox, boolean bboxObrigatorio, Integer tipoId,
            OffsetDateTime dataInicio, OffsetDateTime dataFim, StatusOcorrencia status, boolean apenasMinhas) {

        BoundingBox caixa = null;
        if (bbox != null && !bbox.isBlank()) {
            caixa = BoundingBox.parse(bbox, maxLadoBbox);
        } else if (bboxObrigatorio) {
            throw ApiException.badRequest("O parâmetro bbox é obrigatório (minLon,minLat,maxLon,maxLat).");
        }

        if (tipoId != null && tipoId <= 0) {
            throw ApiException.badRequest("O parâmetro tipo deve ser um id positivo.");
        }
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw ApiException.badRequest("dataInicio não pode ser posterior a dataFim.");
        }

        return new FiltroOcorrencia(caixa, tipoId, dataInicio, dataFim, status, apenasMinhas);
    }
}
