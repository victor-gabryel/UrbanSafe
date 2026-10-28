package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ocorrência. 'autorId' só é preenchido para o próprio autor, GESTOR e ADMINISTRADOR.")
public record OcorrenciaResponse(
        @Schema(example = "42") Integer id,
        TipoResumo tipo,
        @Schema(example = "Celular furtado no ponto de ônibus") String titulo,
        String descricao,
        @Schema(description = "Latitude (-90 a 90)", example = "-8.0476") BigDecimal latitude,
        @Schema(description = "Longitude (-180 a 180)", example = "-34.8770") BigDecimal longitude,
        String endereco,
        OffsetDateTime dataOcorrencia,
        OffsetDateTime dataCadastro,
        OffsetDateTime dataAtualizacao,
        StatusOcorrencia status,
        Relevancia relevancia,
        boolean anonimato,
        @Schema(description = "Id do autor; nulo para quem não é o autor nem GESTOR/ADMINISTRADOR", nullable = true)
        Integer autorId) {

    public record TipoResumo(Integer id, String nome, String categoria) {
    }

    public static OcorrenciaResponse de(Ocorrencia o, boolean mostrarAutor) {
        return new OcorrenciaResponse(
                o.getId(),
                new TipoResumo(o.getTipo().getId(), o.getTipo().getNome(), o.getTipo().getCategoria()),
                o.getTitulo(),
                o.getDescricao(),
                o.getLatitude(),
                o.getLongitude(),
                o.getEndereco(),
                o.getDataOcorrencia(),
                o.getDataCadastro(),
                o.getDataAtualizacao(),
                o.getStatus(),
                o.getRelevancia(),
                o.isAnonimato(),
                mostrarAutor ? o.getUsuarioId() : null);
    }
}
