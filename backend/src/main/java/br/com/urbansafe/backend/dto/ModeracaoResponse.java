package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.AcaoModeracao;
import br.com.urbansafe.backend.entity.Moderacao;
import br.com.urbansafe.backend.entity.StatusOcorrencia;

public record ModeracaoResponse(
        Integer id,
        Integer ocorrenciaId,
        Integer moderadorId,
        AcaoModeracao acao,
        StatusOcorrencia statusAnterior,
        StatusOcorrencia statusNovo,
        String justificativa,
        OffsetDateTime dataModeracao) {

    public static ModeracaoResponse de(Moderacao m) {
        return new ModeracaoResponse(m.getId(), m.getOcorrenciaId(), m.getModeradorId(), m.getAcao(),
                m.getStatusAnterior(), m.getStatusNovo(), m.getJustificativa(), m.getDataModeracao());
    }
}
