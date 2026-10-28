package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.entity.RefreshToken;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.RefreshTokenRepository;
import br.com.urbansafe.backend.util.TokenUtils;

/**
 * Refresh tokens opacos: persistidos apenas como SHA-256, com rotação a cada uso,
 * expiração e revogação. Reuso de um token já rotacionado é tratado como possível roubo
 * e revoga todas as sessões do usuário.
 *
 * noRollbackFor: a revogação precisa ser confirmada no banco mesmo quando a requisição termina em 401.
 */
@Service
public class RefreshTokenService {

    public record RefreshEmitido(String tokenOriginal, Usuario usuario) {
    }

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expiracaoDias;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-days}") long expiracaoDias) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expiracaoDias = expiracaoDias;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public String emitir(Usuario usuario) {
        String tokenOriginal = TokenUtils.gerarTokenAleatorio();

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(TokenUtils.sha256Hex(tokenOriginal));
        token.setExpiraEm(OffsetDateTime.now(ZoneOffset.UTC).plusDays(expiracaoDias));

        refreshTokenRepository.save(token);

        return tokenOriginal;
    }

    /** Valida o refresh token, revoga-o e emite um novo (rotação). */
    @Transactional(noRollbackFor = ApiException.class)
    public RefreshEmitido rotacionar(String tokenOriginal) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshToken atual = refreshTokenRepository
                .findByTokenHash(TokenUtils.sha256Hex(tokenOriginal))
                .orElseThrow(this::tokenInvalido);

        Usuario usuario = atual.getUsuario();

        if (atual.getRevogadoEm() != null) {
            // Token já usado/revogado sendo apresentado de novo: derruba todas as sessões do usuário.
            refreshTokenRepository.revogarTodosDoUsuario(usuario.getId(), agora);
            throw tokenInvalido();
        }

        if (!atual.getExpiraEm().isAfter(agora)) {
            throw tokenInvalido();
        }

        if (!usuario.isAtivo() || !usuario.isEmailVerificado()) {
            refreshTokenRepository.revogarTodosDoUsuario(usuario.getId(), agora);
            throw tokenInvalido();
        }

        // Revogação atômica: se outra requisição concorrente já consumiu este token, 0 linhas são afetadas.
        if (refreshTokenRepository.revogarSeAtivo(atual.getId(), agora) == 0) {
            refreshTokenRepository.revogarTodosDoUsuario(usuario.getId(), agora);
            throw tokenInvalido();
        }

        return new RefreshEmitido(emitir(usuario), usuario);
    }

    /** Idempotente: token desconhecido ou já revogado não gera erro nem revela nada. */
    @Transactional(noRollbackFor = ApiException.class)
    public void revogar(String tokenOriginal) {
        Optional<RefreshToken> token = refreshTokenRepository
                .findByTokenHash(TokenUtils.sha256Hex(tokenOriginal));

        token.ifPresent(t -> refreshTokenRepository.revogarSeAtivo(
                t.getId(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void revogarTodos(Integer usuarioId) {
        refreshTokenRepository.revogarTodosDoUsuario(usuarioId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private ApiException tokenInvalido() {
        return ApiException.unauthorized("Refresh token inválido, expirado ou revogado.");
    }
}
