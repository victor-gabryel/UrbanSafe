package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

public record CadastroResponse(
    Integer id,
    String nome,
    String email,
    boolean emailVerificado,
    OffsetDateTime dataCadastro
) {
}