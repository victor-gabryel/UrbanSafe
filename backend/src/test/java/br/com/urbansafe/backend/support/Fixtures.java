package br.com.urbansafe.backend.support;

import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.entity.Perfil;
import br.com.urbansafe.backend.entity.Usuario;

public final class Fixtures {

    public static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-0123456789";

    private Fixtures() {
    }

    public static Perfil perfil(String nome) {
        Perfil perfil = new Perfil();
        perfil.setNome(nome);
        return perfil;
    }

    public static Usuario usuario(Integer id, String email, String perfil) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Fulano");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil(perfil));
        usuario.setAtivo(true);
        usuario.setEmailVerificado(true);
        return usuario;
    }
}
