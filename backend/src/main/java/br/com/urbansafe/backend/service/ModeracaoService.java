package br.com.urbansafe.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.ModeracaoResponse;
import br.com.urbansafe.backend.dto.ModerarRequest;
import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.AcaoModeracao;
import br.com.urbansafe.backend.entity.Moderacao;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.ModeracaoRepository;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

/**
 * Fluxo de moderação. É o ÚNICO caminho para alterar o status de uma ocorrência. Cada decisão gera um
 * registro imutável em "moderacoes" e um evento em "historico_ocorrencia", na mesma transação.
 */
@Service
public class ModeracaoService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ModeracaoRepository moderacaoRepository;
    private final OcorrenciaAcesso acesso;
    private final HistoricoService historicoService;

    public ModeracaoService(OcorrenciaRepository ocorrenciaRepository, ModeracaoRepository moderacaoRepository,
            OcorrenciaAcesso acesso, HistoricoService historicoService) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.moderacaoRepository = moderacaoRepository;
        this.acesso = acesso;
        this.historicoService = historicoService;
    }

    @Transactional
    public ModeracaoResponse moderar(UsuarioPrincipal principal, Integer ocorrenciaId, ModerarRequest request) {
        exigirEquipe(principal);

        Ocorrencia ocorrencia = acesso.carregar(ocorrenciaId);

        // Evita que o moderador aprove/rejeite o próprio relato (conflito de interesse).
        if (acesso.ehAutor(principal, ocorrencia)) {
            throw ApiException.forbidden("Você não pode moderar uma ocorrência registrada por você.");
        }

        AcaoModeracao acao = request.acao();
        String justificativa = request.justificativa() == null || request.justificativa().isBlank()
                ? null
                : request.justificativa().trim();

        if (acao.exigeJustificativa() && justificativa == null) {
            throw ApiException.badRequest("A justificativa é obrigatória para a ação " + acao + ".");
        }

        StatusOcorrencia atual = ocorrencia.getStatus();
        if (!acao.permitidaPara(atual)) {
            throw ApiException.conflict(
                    "A ação " + acao + " não é permitida para uma ocorrência com status " + atual + ".");
        }

        StatusOcorrencia novo = acao.getStatusResultante();
        ocorrencia.setStatus(novo);
        ocorrenciaRepository.saveAndFlush(ocorrencia);

        Moderacao moderacao = moderacaoRepository.save(
                new Moderacao(ocorrencia.getId(), principal.id(), acao, atual, novo, justificativa));

        historicoService.registrar(ocorrencia.getId(), principal.id(), AcaoHistorico.MODERACAO, atual, novo,
                "Moderação: " + acao + (justificativa == null ? "." : " — " + justificativa));

        return ModeracaoResponse.de(moderacao);
    }

    @Transactional(readOnly = true)
    public List<ModeracaoResponse> listar(UsuarioPrincipal principal, Integer ocorrenciaId) {
        exigirEquipe(principal);
        acesso.carregar(ocorrenciaId);

        return moderacaoRepository.findByOcorrenciaIdOrderByDataModeracaoAscIdAsc(ocorrenciaId).stream()
                .map(ModeracaoResponse::de)
                .toList();
    }

    // Defesa em profundidade: a SecurityConfig já restringe a rota, mas o serviço não confia só nela.
    private void exigirEquipe(UsuarioPrincipal principal) {
        if (principal == null || !principal.isEquipe()) {
            throw ApiException.forbidden("Somente GESTOR ou ADMINISTRADOR podem acessar a moderação.");
        }
    }
}
