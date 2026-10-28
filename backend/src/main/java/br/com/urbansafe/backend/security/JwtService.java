package br.com.urbansafe.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.urbansafe.backend.entity.Usuario;

/**
 * Emite e valida o JWT de acesso (HS256, curta duração).
 * Claims: sub (id do usuário), perfil, iss, iat, exp, jti. Nada de e-mail, senha ou dados pessoais.
 */
@Service
public class JwtService {

    static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final String issuer;
    private final Duration expiracao;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-expiration-minutes}") long expiracaoMinutos) {

        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET (app.jwt.secret) deve ter no mínimo 32 bytes. "
                            + "Gere um com: openssl rand -base64 48");
        }
        if (expiracaoMinutos <= 0) {
            throw new IllegalStateException("app.jwt.access-expiration-minutes deve ser maior que zero.");
        }

        SecretKey chave = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Sem tolerância de relógio: token expirado é rejeitado imediatamente.
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(issuer)));
        this.decoder = nimbus;

        this.issuer = issuer;
        this.expiracao = Duration.ofMinutes(expiracaoMinutos);
    }

    public String gerarAccessToken(Usuario usuario) {
        return gerarAccessToken(usuario, Instant.now());
    }

    /** Permite informar o instante de emissão (usado nos testes para gerar tokens já expirados). */
    public String gerarAccessToken(Usuario usuario, Instant emitidoEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plus(expiracao))
                .id(UUID.randomUUID().toString())
                .claim("perfil", usuario.getPerfil().getNome())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiracaoEmSegundos() {
        return expiracao.toSeconds();
    }

    /**
     * Valida assinatura, emissor e validade e devolve o id do usuário (claim sub).
     *
     * @throws TokenJwtException com motivo EXPIRADO ou INVALIDO
     */
    public Integer validarEExtrairUsuarioId(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return Integer.valueOf(jwt.getSubject());
        } catch (JwtValidationException e) {
            boolean expirado = e.getErrors().stream()
                    .anyMatch(erro -> erro.getDescription() != null
                            && erro.getDescription().contains("expired"));
            throw new TokenJwtException(
                    expirado ? TokenJwtException.Motivo.EXPIRADO : TokenJwtException.Motivo.INVALIDO, e);
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException cobre token malformado/assinatura inválida; IllegalArgumentException cobre sub ausente/não numérico.
            throw new TokenJwtException(TokenJwtException.Motivo.INVALIDO, e);
        }
    }
}
