package br.com.urbansafe.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

    @NotBlank(message = "O refresh token é obrigatório")
    @Size(max = 200, message = "Refresh token inválido")
    String refreshToken

) {
}
