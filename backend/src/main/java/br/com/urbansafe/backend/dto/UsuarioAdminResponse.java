package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.Usuario;

/** Visão administrativa de uma conta. Nunca inclui senha/hash. */
public record UsuarioAdminResponse(
        Integer id,
        String nome,
        String email,
        String perfil,
        boolean ativo,
        boolean emailVerificado,
        OffsetDateTime dataCadastro) {

    /** Exige que o perfil já esteja carregado. */
    public static UsuarioAdminResponse de(Usuario u) {
        return new UsuarioAdminResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().getNome(),
                u.isAtivo(), u.isEmailVerificado(), u.getDataCadastro());
    }
}
