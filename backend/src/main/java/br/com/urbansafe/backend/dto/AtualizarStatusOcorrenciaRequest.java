package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.urbansafe.backend.entity.StatusOcorrencia;

@Schema(description = "Novo status de uma ocorrência (GESTOR ou ADMINISTRADOR). Passa sempre pela moderação: "
        + "PUBLICADA = APROVAR, REJEITADA = REJEITAR, OCULTA = OCULTAR_TEMPORARIAMENTE, "
        + "PENDENTE = SOLICITAR_CORRECAO. A justificativa é obrigatória, exceto ao publicar.")
public record AtualizarStatusOcorrenciaRequest(
        @Schema(example = "REJEITADA") @NotNull(message = "O status é obrigatório") StatusOcorrencia status,
        @Schema(example = "Relato sem informações suficientes para localização.") @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres") String justificativa) {
}
