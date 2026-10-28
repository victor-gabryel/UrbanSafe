package br.com.urbansafe.backend.dto;

public record LoginResponse(

        String accessToken,

        String tokenType,

        long expiresIn,

        String refreshToken,

        UsuarioResponse usuario

) {

}