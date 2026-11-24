package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Bloqueia (ativo=false) ou reativa (ativo=true) uma conta")
public record AtualizarStatusUsuarioRequest(
        @Schema(example = "false") @NotNull(message = "O campo ativo é obrigatório") Boolean ativo) {
}
