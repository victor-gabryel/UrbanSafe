package br.com.urbansafe.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.HistoricoResponse;
import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.HistoricoOcorrencia;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.HistoricoOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

/**
 * Histórico somente de acréscimo. Não há operação de edição ou exclusão (e o banco também as bloqueia).
 * O registro é feito na MESMA transação da alteração que o originou.
 */
@Service
public class HistoricoService {

    private final HistoricoOcorrenciaRepository historicoRepository;
    private final OcorrenciaAcesso acesso;

    public HistoricoService(HistoricoOcorrenciaRepository historicoRepository, OcorrenciaAcesso acesso) {
        this.historicoRepository = historicoRepository;
        this.acesso = acesso;
    }

    @Transactional
    public void registrar(Integer ocorrenciaId, Integer usuarioId, AcaoHistorico acao,
            StatusOcorrencia statusAnterior, StatusOcorrencia statusNovo, String descricao) {
        historicoRepository.save(new HistoricoOcorrencia(
                ocorrenciaId, usuarioId, acao, statusAnterior, statusNovo, descricao));
    }

    /**
     * Consulta restrita ao autor, GESTOR e ADMINISTRADOR (o histórico pode conter justificativas de moderação).
     * O responsável pelo evento só é exposto à equipe ou quando é o próprio solicitante.
     */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> consultar(UsuarioPrincipal principal, Integer ocorrenciaId) {
        Ocorrencia ocorrencia = acesso.carregarVisivel(principal, ocorrenciaId);

        if (!principal.isEquipe() && !acesso.ehAutor(principal, ocorrencia)) {
            throw ApiException.forbidden(
                    "Somente o autor, GESTOR ou ADMINISTRADOR podem consultar o histórico desta ocorrência.");
        }

        return historicoRepository.findByOcorrenciaIdOrderByDataEventoAscIdAsc(ocorrenciaId).stream()
                .map(h -> HistoricoResponse.de(h,
                        principal.isEquipe() || principal.id().equals(h.getUsuarioId())))
                .toList();
    }
}
