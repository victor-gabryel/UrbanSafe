package com.urbansafe.usuario;

import com.urbansafe.perfil.Perfil;
import com.urbansafe.perfil.PerfilRepository;
import com.urbansafe.verificacao.VerificacaoEmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificacaoEmailService verificacaoEmailService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            VerificacaoEmailService verificacaoEmailService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificacaoEmailService = verificacaoEmailService;
    }

    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome é obrigatório.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "O e-mail é obrigatório.");
        }

        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos 8 caracteres.");
        }

        String nomeNormalizado = nome.trim();
        String emailNormalizado =
                email.trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(
                emailNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe um usuário cadastrado com esse e-mail.");
        }

        Perfil perfilUsuario = perfilRepository
                .findByNomeIgnoreCase("USUARIO")
                .orElseThrow(() -> new IllegalStateException(
                        "O perfil USUARIO não está cadastrado no banco."));

        Usuario usuario = new Usuario();
        usuario.setNome(nomeNormalizado);
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(perfilUsuario);
        usuario.setStatus("PENDENTE");

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        verificacaoEmailService.enviarCodigo(usuarioSalvo);

        return usuarioSalvo;
    }
}