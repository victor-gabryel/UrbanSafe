package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.entity.TokenRecuperacaoSenha;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TokenRecuperacaoSenhaRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.util.SenhaValidator;
import br.com.urbansafe.backend.util.TokenUtils;

@Service
public class RecuperacaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    static final String MSG_TOKEN_INVALIDO = "Token de redefinição inválido, expirado ou já utilizado.";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final long expiracaoMinutos;

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenRecuperacaoSenhaRepository tokenRepository,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.password-reset.expiration-minutes}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /**
     * Gera e envia o token de recuperação. Não informa ao chamador se o e-mail existe
     * (evita enumeração de contas); o controller sempre responde com a mesma mensagem.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void solicitar(String emailInformado) {
        String email = emailInformado.trim().toLowerCase(Locale.ROOT);

        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo)
                .ifPresent(usuario -> {
                    OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

                    // Um novo pedido invalida os links anteriores ainda pendentes.
                    tokenRepository.invalidarPendentes(usuario.getId(), agora);

                    String tokenOriginal = TokenUtils.gerarTokenAleatorio();

                    TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
                    token.setUsuario(usuario);
                    token.setTokenHash(TokenUtils.sha256Hex(tokenOriginal));
                    token.setExpiraEm(agora.plusMinutes(expiracaoMinutos));
                    tokenRepository.save(token);

                    try {
                        emailService.enviarRecuperacaoSenha(usuario.getEmail(), tokenOriginal);
                    } catch (MailException e) {
                        log.error("Falha ao enviar e-mail de recuperação (usuário id={}): {}",
                                usuario.getId(), e.getMessage());
                    }
                });
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void redefinir(String tokenOriginal, String novaSenha) {
        SenhaValidator.validarTamanhoBcrypt(novaSenha);

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        TokenRecuperacaoSenha token = tokenRepository
                .findByTokenHash(TokenUtils.sha256Hex(tokenOriginal))
                .orElseThrow(() -> ApiException.badRequest(MSG_TOKEN_INVALIDO));

        // Mensagem única para token usado/expirado/inexistente: não ajuda quem está adivinhando tokens.
        if (token.getUtilizadoEm() != null || !token.getExpiraEm().isAfter(agora)) {
            throw ApiException.badRequest(MSG_TOKEN_INVALIDO);
        }

        Usuario usuario = token.getUsuario();

        if (!usuario.isAtivo()) {
            throw ApiException.badRequest(MSG_TOKEN_INVALIDO);
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        token.setUtilizadoEm(agora);
        tokenRepository.save(token);
        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        // Trocar a senha encerra todas as sessões existentes.
        refreshTokenService.revogarTodos(usuario.getId());
    }
}
