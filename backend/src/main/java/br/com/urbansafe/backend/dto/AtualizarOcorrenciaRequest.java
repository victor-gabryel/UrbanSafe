package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.Relevancia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial: campo ausente (null) NÃO é alterado (nada é apagado por
 * omissão).
 * "endereco" aceita string vazia para limpar o valor. Não existe campo de
 * status: mudanças de status
 * só ocorrem pelo fluxo de moderação.
 */
@Schema(description = "Atualização parcial. Autor: campos do relato enquanto PENDENTE. "
                + "GESTOR/ADMINISTRADOR (em relato de outra pessoa): somente tipoOcorrenciaId e relevancia.")
public record AtualizarOcorrenciaRequest(

                Integer tipoOcorrenciaId,

                @Pattern(regexp = ".*\\S.*", message = "O título não pode ser vazio") @Size(max = 150, message = "O título deve ter no máximo 150 caracteres") String titulo,

                @Pattern(regexp = "(?s).*\\S.*", message = "A descrição não pode ser vazia") @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres") String descricao,

                @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90") @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90") BigDecimal latitude,

                @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180") @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180") BigDecimal longitude,

                @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres") String endereco,

                OffsetDateTime dataOcorrencia,

                Boolean anonimato,

                @Schema(description = "Somente GESTOR/ADMINISTRADOR") Relevancia relevancia) {

        public boolean vazio() {
                return tipoOcorrenciaId == null && titulo == null && descricao == null && latitude == null
                                && longitude == null && endereco == null && dataOcorrencia == null && anonimato == null
                                && relevancia == null;
        }

        /**
         * true se algum campo de conteúdo do relato (além de tipo e relevância) foi
         * enviado.
         */
        public boolean temCamposDoRelato() {
                return titulo != null || descricao != null || latitude != null || longitude != null
                                || endereco != null || dataOcorrencia != null || anonimato != null;
        }
}