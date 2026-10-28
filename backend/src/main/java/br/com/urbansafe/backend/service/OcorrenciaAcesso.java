package br.com.urbansafe.backend.service;

import org.springframework.stereotype.Component;

import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

/**
 * Regras de visibilidade e propriedade compartilhadas pelos serviços de ocorrência, mídia, histórico e moderação.
 * Visível = GESTOR/ADMINISTRADOR (qualquer status), o autor (qualquer status) ou qualquer usuário autenticado
 * quando PUBLICADA. Ocorrência invisível responde 404 (não revela que existe).
 */
@Component
public class OcorrenciaAcesso {

    private final OcorrenciaRepository ocorrenciaRepository;

    public OcorrenciaAcesso(OcorrenciaRepository ocorrenciaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    /** Ocorrência existente e não excluída logicamente, sem considerar visibilidade. */
    public Ocorrencia carregar(Integer id) {
        return ocorrenciaRepository.findByIdAndExcluidaEmIsNull(id)
                .orElseThrow(() -> ApiException.notFound("Ocorrência não encontrada."));
    }

    public Ocorrencia carregarVisivel(UsuarioPrincipal principal, Integer id) {
        Ocorrencia ocorrencia = carregar(id);
        if (!podeVer(principal, ocorrencia)) {
            throw ApiException.notFound("Ocorrência não encontrada.");
        }
        return ocorrencia;
    }

    public boolean podeVer(UsuarioPrincipal principal, Ocorrencia ocorrencia) {
        return principal.isEquipe()
                || ehAutor(principal, ocorrencia)
                || ocorrencia.getStatus() == StatusOcorrencia.PUBLICADA;
    }

    public boolean ehAutor(UsuarioPrincipal principal, Ocorrencia ocorrencia) {
        return principal.id() != null && principal.id().equals(ocorrencia.getUsuarioId());
    }
}
