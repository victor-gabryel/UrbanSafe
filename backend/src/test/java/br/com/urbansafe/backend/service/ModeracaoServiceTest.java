package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

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

class ModeracaoServiceTest {

    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(10, "u@x.com", "USUARIO");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(20, "g@x.com", "GESTOR");
    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(30, "a@x.com", "ADMINISTRADOR");

    private OcorrenciaRepository ocorrenciaRepository;
    private ModeracaoRepository moderacaoRepository;
    private HistoricoService historico;
    private ModeracaoService service;

    @BeforeEach
    void preparar() {
        ocorrenciaRepository = mock(OcorrenciaRepository.class);
        moderacaoRepository = mock(ModeracaoRepository.class);
        historico = mock(HistoricoService.class);
        service = new ModeracaoService(ocorrenciaRepository, moderacaoRepository,
                new OcorrenciaAcesso(ocorrenciaRepository), historico);

        when(moderacaoRepository.save(any(Moderacao.class))).thenAnswer(i -> i.getArgument(0));
        when(ocorrenciaRepository.saveAndFlush(any(Ocorrencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Ocorrencia ocorrencia(int autorId, StatusOcorrencia status) {
        Ocorrencia o = new Ocorrencia();
        ReflectionTestUtils.setField(o, "id", 1);
        o.setUsuarioId(autorId);
        o.setStatus(status);
        when(ocorrenciaRepository.findByIdAndExcluidaEmIsNull(1)).thenReturn(Optional.of(o));
        return o;
    }

    private ApiException erro(UsuarioPrincipal p, ModerarRequest r) {
        return assertThrows(ApiException.class, () -> service.moderar(p, 1, r));
    }

    @Test
    void usuarioComumNaoModeraNemConsultaModeracao() {
        ocorrencia(99, StatusOcorrencia.PENDENTE);

        assertEquals(HttpStatus.FORBIDDEN, erro(USUARIO, new ModerarRequest(AcaoModeracao.APROVAR, null)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> service.listar(USUARIO, 1)).getStatus());
        verify(moderacaoRepository, never()).save(any());
    }

    @Test
    void gestorAprovaPendenteERegistraModeracaoEHistorico() {
        Ocorrencia o = ocorrencia(99, StatusOcorrencia.PENDENTE);

        ModeracaoResponse r = service.moderar(GESTOR, 1, new ModerarRequest(AcaoModeracao.APROVAR, null));

        assertEquals(StatusOcorrencia.PUBLICADA, o.getStatus());
        assertEquals(StatusOcorrencia.PENDENTE, r.statusAnterior());
        assertEquals(StatusOcorrencia.PUBLICADA, r.statusNovo());
        assertEquals(20, r.moderadorId());
        verify(historico).registrar(eq(1), eq(20), eq(AcaoHistorico.MODERACAO),
                eq(StatusOcorrencia.PENDENTE), eq(StatusOcorrencia.PUBLICADA), any());
    }

    @Test
    void administradorTambemModeraEJustificativaFicaRegistrada() {
        Ocorrencia o = ocorrencia(99, StatusOcorrencia.PUBLICADA);

        ModeracaoResponse r = service.moderar(ADMIN, 1,
                new ModerarRequest(AcaoModeracao.OCULTAR_TEMPORARIAMENTE, "  dado sensível  "));

        assertEquals(StatusOcorrencia.OCULTA, o.getStatus());
        assertEquals("dado sensível", r.justificativa());
    }

    @Test
    void rejeitarExigeJustificativa() {
        Ocorrencia o = ocorrencia(99, StatusOcorrencia.PENDENTE);

        assertEquals(HttpStatus.BAD_REQUEST,
                erro(GESTOR, new ModerarRequest(AcaoModeracao.REJEITAR, "   ")).getStatus());
        assertEquals(StatusOcorrencia.PENDENTE, o.getStatus());
        verify(moderacaoRepository, never()).save(any());
    }

    @Test
    void transicaoInvalidaRetorna409SemAlterarNada() {
        Ocorrencia o = ocorrencia(99, StatusOcorrencia.PUBLICADA);

        assertEquals(HttpStatus.CONFLICT,
                erro(GESTOR, new ModerarRequest(AcaoModeracao.APROVAR, null)).getStatus());
        assertEquals(StatusOcorrencia.PUBLICADA, o.getStatus());
        verify(moderacaoRepository, never()).save(any());
        verify(historico, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void naoModeraAPropriaOcorrencia() {
        ocorrencia(20, StatusOcorrencia.PENDENTE); // autor = gestor
        assertEquals(HttpStatus.FORBIDDEN,
                erro(GESTOR, new ModerarRequest(AcaoModeracao.APROVAR, null)).getStatus());
    }

    @Test
    void ocorrenciaInexistenteRetorna404() {
        when(ocorrenciaRepository.findByIdAndExcluidaEmIsNull(1)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                erro(GESTOR, new ModerarRequest(AcaoModeracao.APROVAR, null)).getStatus());
    }
}
