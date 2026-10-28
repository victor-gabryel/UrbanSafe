package br.com.urbansafe.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "O token é obrigatório")
        @Size(max = 200, message = "Token inválido")
        String token,
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {
}
