package br.com.urbansafe.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(

        @NotBlank(message = "O nome é obrigatório")

        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")

        String nome,

        @NotBlank(message = "O e-mail é obrigatório")

        @Email(message = "Informe um e-mail válido")

        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")

        String email,

        @NotBlank(message = "A senha é obrigatória")

        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")

        String senha

) {

}