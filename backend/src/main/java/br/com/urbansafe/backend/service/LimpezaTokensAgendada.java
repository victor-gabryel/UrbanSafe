package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.repository.RefreshTokenRepository;
import br.com.urbansafe.backend.repository.TokenRecuperacaoSenhaRepository;
import br.com.urbansafe.backend.repository.TokenVerificacaoEmailRepository;

/**
 * Rotina diária de retenção: remove tokens (verificação de e-mail, recuperação de senha e refresh) expirados,
 * utilizados ou revogados há mais de {@code app.tokens.retencao-dias} dias (padrão 30, seção 18.2).
 */
@Component
public class LimpezaTokensAgendada {

    private static final Logger log = LoggerFactory.getLogger(LimpezaTokensAgendada.class);

    private final TokenVerificacaoEmailRepository verificacaoRepository;
    private final TokenRecuperacaoSenhaRepository recuperacaoRepository;
    private final RefreshTokenRepository refreshRepository;
    private final long retencaoDias;

    public LimpezaTokensAgendada(
            TokenVerificacaoEmailRepository verificacaoRepository,
            TokenRecuperacaoSenhaRepository recuperacaoRepository,
            RefreshTokenRepository refreshRepository,
            @Value("${app.tokens.retencao-dias:30}") long retencaoDias) {
        this.verificacaoRepository = verificacaoRepository;
        this.recuperacaoRepository = recuperacaoRepository;
        this.refreshRepository = refreshRepository;
        this.retencaoDias = retencaoDias;
    }

    @Scheduled(cron = "${app.tokens.limpeza-cron:0 30 3 * * *}")
    @Transactional
    public void limpar() {
        OffsetDateTime limite = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retencaoDias);
        int verificacao = verificacaoRepository.removerAnterioresA(limite);
        int recuperacao = recuperacaoRepository.removerAnterioresA(limite);
        int refresh = refreshRepository.removerAnterioresA(limite);
        log.info("Limpeza de tokens: {} de verificação, {} de recuperação, {} de sessão removidos.",
                verificacao, recuperacao, refresh);
    }
}
