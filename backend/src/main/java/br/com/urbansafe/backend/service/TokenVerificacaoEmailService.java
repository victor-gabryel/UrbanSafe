package br.com.urbansafe.backend.service;

import br.com.urbansafe.backend.entity.TokenVerificacaoEmail;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TokenVerificacaoEmailRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenVerificacaoEmailService {

    private final TokenVerificacaoEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenVerificacaoEmailService(
            TokenVerificacaoEmailRepository tokenRepository,
            UsuarioRepository usuarioRepository) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public String gerarToken(Usuario usuario) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String tokenOriginal = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        TokenVerificacaoEmail token = new TokenVerificacaoEmail();
        token.setUsuario(usuario);
        token.setTokenHash(gerarHash(tokenOriginal));
        token.setExpiraEm(
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));

        tokenRepository.save(token);

        return tokenOriginal;
    }

    @Transactional
    public void verificarEmail(String tokenOriginal) {
        if (tokenOriginal == null || tokenOriginal.isBlank()) {
            throw ApiException.badRequest("O token de verificação é obrigatório.");
        }

        String hash = gerarHash(tokenOriginal);

        TokenVerificacaoEmail token = tokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest("Token de verificação inválido."));

        if (token.getUtilizadoEm() != null) {
            throw ApiException.badRequest("Este token já foi utilizado.");
        }

        if (!token.getExpiraEm().isAfter(
                OffsetDateTime.now(ZoneOffset.UTC))) {
            throw ApiException.badRequest("O token de verificação expirou.");
        }

        Usuario usuario = token.getUsuario();
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        token.setUtilizadoEm(OffsetDateTime.now(ZoneOffset.UTC));
        tokenRepository.save(token);
    }

    public String gerarHash(String tokenOriginal) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    tokenOriginal.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Não foi possível gerar o hash do token.", e);
        }
    }
}