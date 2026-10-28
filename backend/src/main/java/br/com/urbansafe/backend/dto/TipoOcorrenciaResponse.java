package br.com.urbansafe.backend.dto;

import br.com.urbansafe.backend.entity.TipoOcorrencia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de ocorrência do catálogo")
public record TipoOcorrenciaResponse(
        @Schema(example = "1") Integer id,
        @Schema(example = "Furto") String nome,
        @Schema(example = "Subtração de bem móvel sem violência ou grave ameaça.") String descricao,
        @Schema(example = "Patrimônio") String categoria,
        @Schema(example = "true") boolean ativo,
        @Schema(description = "Se true, ocorrências desse tipo nascem PENDENTES e passam por moderação",
                example = "true") boolean exigeRevisao) {

    public static TipoOcorrenciaResponse de(TipoOcorrencia tipo) {
        return new TipoOcorrenciaResponse(tipo.getId(), tipo.getNome(), tipo.getDescricao(),
                tipo.getCategoria(), tipo.isAtivo(), tipo.isExigeRevisao());
    }
}
