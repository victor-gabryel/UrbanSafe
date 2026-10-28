package br.com.urbansafe.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AtualizarPerfilRequest;
import br.com.urbansafe.backend.dto.CadastroRequest;
import br.com.urbansafe.backend.dto.CadastroResponse;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.entity.Perfil;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.util.SenhaValidator;

@Service
public class UsuarioService {

        private final UsuarioRepository usuarioRepository;
        private final PerfilRepository perfilRepository;
        private final PasswordEncoder passwordEncoder;
        private final VerificacaoEmailNotificador verificacaoNotificador;
        private final TokenVerificacaoEmailService tokenVerificacaoEmailService;
        private final RefreshTokenService refreshTokenService;

        public UsuarioService(
                        UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        PasswordEncoder passwordEncoder,
                        VerificacaoEmailNotificador verificacaoNotificador,
                        TokenVerificacaoEmailService tokenVerificacaoEmailService,
                        RefreshTokenService refreshTokenService) {

                this.usuarioRepository = usuarioRepository;
                this.perfilRepository = perfilRepository;
                this.passwordEncoder = passwordEncoder;
                this.verificacaoNotificador = verificacaoNotificador;
                this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
                this.refreshTokenService = refreshTokenService;
        }

        @Transactional
        public CadastroResponse cadastrar(CadastroRequest request) {
                String email = request.email()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                SenhaValidator.validarTamanhoBcrypt(request.senha());

                if (usuarioRepository.existsByEmailIgnoreCase(email)) {
                        throw ApiException.conflict("Este e-mail já está cadastrado.");
                }

                // Cadastro público sempre cria USUARIO; o request não possui campo de perfil.
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

                // O e-mail só sai depois do commit; falha de SMTP não desfaz o cadastro (o usuário usa
                // POST /api/auth/reenviar-verificacao, que não cria conta duplicada).
                verificacaoNotificador.enviarAposCommit(salvo.getId(), salvo.getEmail(), token);

                return new CadastroResponse(
                                salvo.getId(),
                                salvo.getNome(),
                                salvo.getEmail(),
                                salvo.isEmailVerificado(),
                                salvo.getDataCadastro());

        }

        @Transactional(readOnly = true)
        public UsuarioResponse buscarPerfil(Integer usuarioId) {
                return UsuarioResponse.de(carregar(usuarioId));
        }

        /** Consulta de outro usuário: apenas o próprio ou ADMINISTRADOR. */
        @Transactional(readOnly = true)
        public UsuarioResponse buscarPorId(UsuarioPrincipal solicitante, Integer id) {
                if (!solicitante.isAdministrador() && !solicitante.id().equals(id)) {
                        throw ApiException.forbidden("Você não tem permissão para consultar este usuário.");
                }

                return UsuarioResponse.de(carregar(id));
        }

        @Transactional
        public UsuarioResponse atualizarPerfil(Integer usuarioId, AtualizarPerfilRequest request) {
                Usuario usuario = carregar(usuarioId);

                usuario.setNome(request.nome().trim());

                return UsuarioResponse.de(usuarioRepository.save(usuario));
        }

        /**
         * Desativação lógica: a linha é preservada (auditoria e vínculos futuros com ocorrências/rotas)
         * e todas as sessões são revogadas. O e-mail continua reservado.
         */
        @Transactional
        public void desativarConta(Integer usuarioId) {
                Usuario usuario = carregar(usuarioId);

                usuario.setAtivo(false);
                usuarioRepository.save(usuario);

                refreshTokenService.revogarTodos(usuarioId);
        }

        private Usuario carregar(Integer id) {
                return usuarioRepository.findComPerfilById(id)
                                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        }
}
