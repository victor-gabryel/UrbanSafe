package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial: campo ausente (null) NÃO é alterado. Para limpar descrição/categoria envie
 * string vazia. "ativo" = true reativa um tipo desativado.
 */
@Schema(description = "Atualização parcial de tipo de ocorrência (somente ADMINISTRADOR). Campos ausentes não são alterados.")
public record AtualizarTipoOcorrenciaRequest(

        @Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres")
        String categoria,

        Boolean ativo,

        Boolean exigeRevisao) {

    public boolean vazio() {
        return nome == null && descricao == null && categoria == null && ativo == null && exigeRevisao == null;
    }
}
