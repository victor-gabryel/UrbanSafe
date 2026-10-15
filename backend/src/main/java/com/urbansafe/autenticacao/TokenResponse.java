package com.urbansafe.autenticacao;

public record TokenResponse(
        String token,
        String tipo,
        long expiraEm,
        String nome,
        String email,
        String perfil) {
}