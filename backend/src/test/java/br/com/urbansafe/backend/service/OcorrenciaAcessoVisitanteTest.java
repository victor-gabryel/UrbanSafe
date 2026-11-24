package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;

/** Visitante anônimo (principal nulo) enxerga somente ocorrências PUBLICADAS. */
class OcorrenciaAcessoVisitanteTest {

    private static Ocorrencia com(StatusOcorrencia status) {
        Ocorrencia o = new Ocorrencia();
        o.setUsuarioId(1);
        o.setStatus(status);
        return o;
    }

    @Test
    void visitanteVePublicadaMasNaoPendente() {
        var acesso = new OcorrenciaAcesso(mock(OcorrenciaRepository.class));

        assertTrue(acesso.podeVer(null, com(StatusOcorrencia.PUBLICADA)));
        for (StatusOcorrencia s : new StatusOcorrencia[] { StatusOcorrencia.PENDENTE, StatusOcorrencia.REJEITADA,
                StatusOcorrencia.OCULTA }) {
            assertFalse(acesso.podeVer(null, com(s)));
        }
        assertFalse(acesso.ehAutor(null, com(StatusOcorrencia.PUBLICADA)));
    }

    @Test
    void ocorrenciaNaoPublicadaResponde404ParaVisitante() {
        var repo = mock(OcorrenciaRepository.class);
        when(repo.findByIdAndExcluidaEmIsNull(5)).thenReturn(Optional.of(com(StatusOcorrencia.PENDENTE)));

        var ex = assertThrows(ApiException.class, () -> new OcorrenciaAcesso(repo).carregarVisivel(null, 5));
        assertEquals(404, ex.getStatus().value());
    }
}
