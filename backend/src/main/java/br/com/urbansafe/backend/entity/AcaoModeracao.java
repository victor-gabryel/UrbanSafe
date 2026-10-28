package br.com.urbansafe.backend.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ações de moderação (tipo PostgreSQL acao_moderacao) com o fluxo mínimo adotado pelo projeto.
 *
 * <p>O documento define apenas os valores das ações e dos status. As transições abaixo são a solução
 * mínima e consistente adotada (decisão registrada no README):
 * <pre>
 * APROVAR                 PENDENTE | OCULTA | REJEITADA  -> PUBLICADA   (justificativa opcional)
 * REJEITAR                PENDENTE | PUBLICADA | OCULTA  -> REJEITADA   (justificativa obrigatória)
 * SOLICITAR_CORRECAO      PENDENTE | PUBLICADA           -> PENDENTE    (justificativa obrigatória)
 * OCULTAR_TEMPORARIAMENTE PENDENTE | PUBLICADA           -> OCULTA      (justificativa obrigatória)
 * </pre>
 */
public enum AcaoModeracao {

    APROVAR(StatusOcorrencia.PUBLICADA, false,
            EnumSet.of(StatusOcorrencia.PENDENTE, StatusOcorrencia.OCULTA, StatusOcorrencia.REJEITADA)),
    REJEITAR(StatusOcorrencia.REJEITADA, true,
            EnumSet.of(StatusOcorrencia.PENDENTE, StatusOcorrencia.PUBLICADA, StatusOcorrencia.OCULTA)),
    SOLICITAR_CORRECAO(StatusOcorrencia.PENDENTE, true,
            EnumSet.of(StatusOcorrencia.PENDENTE, StatusOcorrencia.PUBLICADA)),
    OCULTAR_TEMPORARIAMENTE(StatusOcorrencia.OCULTA, true,
            EnumSet.of(StatusOcorrencia.PENDENTE, StatusOcorrencia.PUBLICADA));

    private final StatusOcorrencia statusResultante;
    private final boolean exigeJustificativa;
    private final Set<StatusOcorrencia> statusPermitidos;

    AcaoModeracao(StatusOcorrencia statusResultante, boolean exigeJustificativa,
            Set<StatusOcorrencia> statusPermitidos) {
        this.statusResultante = statusResultante;
        this.exigeJustificativa = exigeJustificativa;
        this.statusPermitidos = statusPermitidos;
    }

    public StatusOcorrencia getStatusResultante() {
        return statusResultante;
    }

    public boolean exigeJustificativa() {
        return exigeJustificativa;
    }

    public boolean permitidaPara(StatusOcorrencia atual) {
        return statusPermitidos.contains(atual);
    }
}
