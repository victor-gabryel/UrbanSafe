package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.entity.TokenVerificacaoEmail;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TokenVerificacaoEmailRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.util.TokenUtils;

/**
 * Tokens de verificação de e-mail: 256 bits aleatórios (SecureRandom), apenas o SHA-256 vai ao banco,
 * validade configurável (padrão 24 h) e uso único. Tabela própria ({@code tokens_verificacao_email}),
 * separada da de recuperação de senha: um token de um fluxo nunca é aceito no outro.
 */
@Service
public class TokenVerificacaoEmailService {

    static final String MSG_TOKEN_OBRIGATORIO = "O token de verificação é obrigatório.";
    static final String MSG_TOKEN_INVALIDO = "Token de verificação inválido.";
    static final String MSG_TOKEN_USADO = "Este token já foi utilizado.";
    static final String MSG_TOKEN_EXPIRADO = "O token de verificação expirou.";

    private final TokenVerificacaoEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final long expiracaoHoras;
    private final long intervaloReenvioSegundos;

    public TokenVerificacaoEmailService(
            TokenVerificacaoEmailRepository tokenRepository,
            UsuarioRepository usuarioRepository,
            @Value("${app.email-verification.expiration-hours:24}") long expiracaoHoras,
            @Value("${app.email-verification.resend-cooldown-seconds:60}") long intervaloReenvioSegundos) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.expiracaoHoras = expiracaoHoras;
        this.intervaloReenvioSegundos = intervaloReenvioSegundos;
    }

    /**
     * Emite um novo token e invalida os pendentes do usuário (no máximo um token ativo por usuário).
     * Retorna o valor original, que só deve ir para o e-mail do usuário — nunca para log ou resposta HTTP.
     */
    @Transactional
    public String gerarToken(Usuario usuario) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        String tokenOriginal = TokenUtils.gerarTokenAleatorio();

        TokenVerificacaoEmail token = new TokenVerificacaoEmail();
        token.setUsuario(usuario);
        token.setTokenHash(TokenUtils.sha256Hex(tokenOriginal));
        token.setExpiraEm(agora.plusHours(expiracaoHoras));
        tokenRepository.save(token);

        return tokenOriginal;
    }

    /** Já saiu um e-mail de verificação para esta conta dentro do intervalo mínimo entre reenvios? */
    @Transactional(readOnly = true)
    public boolean emIntervaloDeReenvio(Integer usuarioId) {
        OffsetDateTime limite = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(intervaloReenvioSegundos);

        return tokenRepository.existsByUsuario_IdAndCriadoEmAfter(usuarioId, limite);
    }

    /**
     * Valida o token, consome-o e ativa o e-mail na mesma transação. Qualquer exceção desfaz tudo:
     * o token só fica "usado" se o e-mail realmente foi marcado como verificado.
     */
    @Transactional
    public void verificarEmail(String tokenOriginal) {
        if (tokenOriginal == null || tokenOriginal.isBlank()) {
            throw ApiException.badRequest(MSG_TOKEN_OBRIGATORIO);
        }

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        String hash = TokenUtils.sha256Hex(tokenOriginal.trim());

        TokenVerificacaoEmail token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest(MSG_TOKEN_INVALIDO));

        if (token.getUtilizadoEm() != null) {
            throw ApiException.badRequest(MSG_TOKEN_USADO);
        }

        if (!token.getExpiraEm().isAfter(agora)) {
            throw ApiException.badRequest(MSG_TOKEN_EXPIRADO);
        }

        Usuario usuario = token.getUsuario();

        // Conta desativada não é reativada nem "verificada" por este fluxo.
        if (!usuario.isAtivo()) {
            throw ApiException.badRequest(MSG_TOKEN_INVALIDO);
        }

        // UPDATE condicional: se duas requisições chegarem juntas com o mesmo token, só uma recebe 1.
        if (tokenRepository.consumir(hash, agora) == 0) {
            throw ApiException.badRequest(MSG_TOKEN_USADO);
        }

        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        // Qualquer outro link pendente deixa de valer.
        tokenRepository.invalidarPendentes(usuario.getId(), agora);
    }
}
