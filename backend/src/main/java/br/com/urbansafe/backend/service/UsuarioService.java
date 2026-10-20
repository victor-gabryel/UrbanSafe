package br.com.urbansafe.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.CadastroRequest;
import br.com.urbansafe.backend.dto.CadastroResponse;
import br.com.urbansafe.backend.entity.Perfil;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

        private final UsuarioRepository usuarioRepository;
        private final PerfilRepository perfilRepository;
        private final PasswordEncoder passwordEncoder;
        private final EmailService emailService;
        private final TokenVerificacaoEmailService tokenVerificacaoEmailService;

        public UsuarioService(
                        UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        TokenVerificacaoEmailService tokenVerificacaoEmailService) {

                this.usuarioRepository = usuarioRepository;
                this.perfilRepository = perfilRepository;
                this.passwordEncoder = passwordEncoder;
                this.emailService = emailService;
                this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
        }

        @Transactional
        public CadastroResponse cadastrar(CadastroRequest request) {
                String email = request.email()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                if (usuarioRepository.existsByEmailIgnoreCase(email)) {
                        throw new IllegalArgumentException(
                                        "Este e-mail já está cadastrado.");
                }

                Perfil perfilUsuario = perfilRepository.findByNome("USUARIO")
                                .orElseThrow(() -> new IllegalStateException(
                                                "Perfil USUARIO não encontrado no banco de dados."));

                Usuario usuario = new Usuario();
                usuario.setNome(request.nome().trim());
                usuario.setEmail(email);
                usuario.setSenhaHash(
                                passwordEncoder.encode(request.senha()));
                usuario.setPerfil(perfilUsuario);
                usuario.setAtivo(true);
                usuario.setEmailVerificado(false);

                Usuario salvo = usuarioRepository.save(usuario);

                String token = tokenVerificacaoEmailService.gerarToken(salvo);

                emailService.enviarVerificacao(
                                salvo.getEmail(),
                                token);

                return new CadastroResponse(
                                salvo.getId(),
                                salvo.getNome(),
                                salvo.getEmail(),
                                salvo.isEmailVerificado(),
                                salvo.getDataCadastro());

        }
}