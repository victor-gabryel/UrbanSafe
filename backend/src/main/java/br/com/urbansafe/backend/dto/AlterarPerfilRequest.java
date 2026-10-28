package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Novo perfil de um usuário (somente ADMINISTRADOR)")
public record AlterarPerfilRequest(

        @Schema(description = "USUARIO (usuário comum; 'USUARIO_COMUM' também é aceito), GESTOR ou ADMINISTRADOR",
                example = "GESTOR")
        @NotBlank(message = "O perfil é obrigatório")
        String perfil) {
}
