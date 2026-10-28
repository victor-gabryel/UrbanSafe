package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;
import br.com.urbansafe.backend.entity.Usuario;

/** Dados públicos do usuário. Nunca inclui senha/hash. */

public record UsuarioResponse(

        Integer id,

        String nome,

        String email,

        String perfil,

        boolean emailVerificado,

        OffsetDateTime dataCadastro

) {

    /** Exige que o perfil já esteja carregado (use findComPerfil...). */

    public static UsuarioResponse de(Usuario usuario) {

        return new UsuarioResponse(

                usuario.getId(),

                usuario.getNome(),

                usuario.getEmail(),

                usuario.getPerfil().getNome(),

                usuario.isEmailVerificado(),

                usuario.getDataCadastro());

    }

}