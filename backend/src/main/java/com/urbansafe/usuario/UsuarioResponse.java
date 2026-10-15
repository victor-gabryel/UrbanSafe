package com.urbansafe.usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String perfil,
        String status,
        OffsetDateTime dataCadastro
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().getNome(),
                usuario.getStatus(),
                usuario.getDataCadastro()
        );
    }
}