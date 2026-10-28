package br.com.urbansafe.backend.dto;

import br.com.urbansafe.backend.entity.AcaoModeracao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Ação de moderação (GESTOR ou ADMINISTRADOR). A justificativa é obrigatória, exceto em APROVAR.")
public record ModerarRequest(

                @Schema(example = "REJEITAR") @NotNull(message = "A ação é obrigatória") AcaoModeracao acao,

                @Schema(example = "Relato sem informações suficientes para localização.") @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres") String justificativa) {
}