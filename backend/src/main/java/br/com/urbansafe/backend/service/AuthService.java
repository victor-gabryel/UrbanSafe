package br.com.urbansafe.backend.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.LoginRequest;
import br.com.urbansafe.backend.dto.LoginResponse;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.JwtService;

@Service
public class AuthService {

    static final String MSG_CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenVerificacaoEmailService tokenVerificacaoEmailService;
    private final VerificacaoEmailNotificador verificacaoNotificador;

    /** Hash descartável usado para gastar o mesmo tempo de BCrypt quando o e-mail não existe. */
    private final String hashFicticio;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TokenVerificacaoEmailService tokenVerificacaoEmailService,
            VerificacaoEmailNotificador verificacaoNotificador) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
        this.verificacaoNotificador = verificacaoNotificador;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-equalizar-tempo");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Optional<Usuario> encontrado = usuarioRepository.findComPerfilByEmailIgnoreCase(email);

        if (encontrado.isEmpty()) {
            passwordEncoder.matches(request.senha(), hashFicticio);
            throw ApiException.unauthorized(MSG_CREDENCIAIS_INVALIDAS);
        }

        Usuario usuario = encontrado.get();

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw ApiException.unauthorized(MSG_CREDENCIAIS_INVALIDAS);
        }

        // Só depois de provar que conhece a senha é que o estado da conta é revelado.
        if (!usuario.isAtivo()) {
            throw ApiException.forbidden("Esta conta está desativada.");
        }

        if (!usuario.isEmailVerificado()) {
            throw ApiException.forbidden(
                    "E-mail ainda não verificado. Verifique sua caixa de entrada ou solicite um novo link.");
        }

        return montarResposta(usuario, refreshTokenService.emitir(usuario));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse refresh(String refreshToken) {
        RefreshTokenService.RefreshEmitido emitido = refreshTokenService.rotacionar(refreshToken);

        return montarResposta(emitido.usuario(), emitido.tokenOriginal());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void logout(String refreshToken) {
        refreshTokenService.revogar(refreshToken);
    }

    /**
     * Reenvio do e-mail de verificação. Resposta sempre genérica para o chamador:
     * não revela se o e-mail existe, se já foi verificado nem se caiu no intervalo mínimo entre envios.
     * Emitir um novo token invalida o anterior; o e-mail só sai depois do commit.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void reenviarVerificacao(String emailInformado) {
        String email = emailInformado.trim().toLowerCase(Locale.ROOT);

        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.isAtivo() && !u.isEmailVerificado())
                .filter(u -> !tokenVerificacaoEmailService.emIntervaloDeReenvio(u.getId()))
                .ifPresent(usuario -> {
                    String token = tokenVerificacaoEmailService.gerarToken(usuario);
                    verificacaoNotificador.enviarAposCommit(usuario.getId(), usuario.getEmail(), token);
                });
    }

    private LoginResponse montarResposta(Usuario usuario, String refreshToken) {
        return new LoginResponse(
                jwtService.gerarAccessToken(usuario),
                "Bearer",
                jwtService.getExpiracaoEmSegundos(),
                refreshToken,
                UsuarioResponse.de(usuario));
    }
}
