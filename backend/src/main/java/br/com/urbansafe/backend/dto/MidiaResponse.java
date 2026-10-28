package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.MidiaOcorrencia;
import br.com.urbansafe.backend.entity.TipoMidia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadados da mídia. O arquivo é obtido pela rota autenticada 'arquivoUrl'; a chave interna do armazenamento nunca é exposta.")
public record MidiaResponse(
        Integer id,
        Integer ocorrenciaId,
        TipoMidia tipo,
        @Schema(example = "image/jpeg") String tipoConteudo,
        String nomeArquivo,
        String descricao,
        long tamanhoBytes,
        OffsetDateTime dataEnvio,
        @Schema(example = "/api/ocorrencias/42/midias/7/arquivo") String arquivoUrl) {

    public static MidiaResponse de(MidiaOcorrencia m) {
        return new MidiaResponse(m.getId(), m.getOcorrenciaId(), m.getTipo(), m.getTipoConteudo(),
                m.getNomeArquivo(), m.getDescricao(), m.getTamanhoBytes(), m.getDataEnvio(),
                "/api/ocorrencias/" + m.getOcorrenciaId() + "/midias/" + m.getId() + "/arquivo");
    }
}
