package br.com.urbansafe.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *
 * Campos que o próprio usuário pode alterar. Propositalmente NÃO há
 * perfil/papel, e-mail,
 *
 * ativo ou email_verificado: Jackson ignora campos desconhecidos por padrão,
 * então
 *
 * qualquer tentativa de enviá-los é descartada.
 *
 */
public record AtualizarPerfilRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome
) {
}
