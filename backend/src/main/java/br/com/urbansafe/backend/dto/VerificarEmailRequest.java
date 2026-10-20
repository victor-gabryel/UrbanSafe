package br.com.urbansafe.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarEmailRequest(
        @NotBlank(message = "O token é obrigatório.") String token) {
}