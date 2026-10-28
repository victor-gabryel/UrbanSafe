package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar um tipo de ocorrência (somente ADMINISTRADOR)")
public record CriarTipoOcorrenciaRequest(
        @Schema(example = "Vandalismo") @NotBlank(message = "O nome é obrigatório") @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres") String nome,
        @Schema(example = "Pichação ou depredação de espaço público.") @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres") String descricao,
        @Schema(example = "Patrimônio") @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres") String categoria,
        @Schema(description = "Padrão: true", example = "true") Boolean exigeRevisao) {
}
