package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.HistoricoOcorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento do histórico de uma ocorrência")
public record HistoricoResponse(
        Integer id,
        AcaoHistorico acao,
        StatusOcorrencia statusAnterior,
        StatusOcorrencia statusNovo,
        String descricao,
        OffsetDateTime dataEvento,
        @Schema(description = "Responsável pelo evento; nulo em eventos automáticos ou quando o solicitante é o autor e o responsável é outra pessoa",
                nullable = true)
        Integer usuarioId) {

    public static HistoricoResponse de(HistoricoOcorrencia h, boolean mostrarResponsavel) {
        return new HistoricoResponse(h.getId(), h.getAcao(), h.getStatusAnterior(), h.getStatusNovo(),
                h.getDescricao(), h.getDataEvento(), mostrarResponsavel ? h.getUsuarioId() : null);
    }
}
