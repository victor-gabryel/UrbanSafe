package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.dto.AtualizarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.OcorrenciaResponse;
import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.entity.TipoOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;
import br.com.urbansafe.backend.repository.TipoOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

class OcorrenciaServiceTest {

    private static final UsuarioPrincipal AUTOR = new UsuarioPrincipal(10, "a@x.com", "USUARIO");
    private static final UsuarioPrincipal OUTRO = new UsuarioPrincipal(11, "b@x.com", "USUARIO");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(20, "g@x.com", "GESTOR");
    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(30, "adm@x.com", "ADMINISTRADOR");

    private OcorrenciaRepository ocorrenciaRepository;
    private TipoOcorrenciaRepository tipoRepository;
    private HistoricoService historico;
    private OcorrenciaService service;
    private TipoOcorrencia tipo;

    @BeforeEach
    void preparar() {
        ocorrenciaRepository = mock(OcorrenciaRepository.class);
        tipoRepository = mock(TipoOcorrenciaRepository.class);
        historico = mock(HistoricoService.class);
        service = new OcorrenciaService(ocorrenciaRepository, tipoRepository,
                mock(OcorrenciaConsultaRepository.class), new OcorrenciaAcesso(ocorrenciaRepository),
                historico, 100, 5.0);

        tipo = tipo(1, true, true);
        when(tipoRepository.findById(1)).thenReturn(Optional.of(tipo));
        when(ocorrenciaRepository.saveAndFlush(any(Ocorrencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private TipoOcorrencia tipo(int id, boolean ativo, boolean exigeRevisao) {
        TipoOcorrencia t = new TipoOcorrencia();
        ReflectionTestUtils.setField(t, "id", id);
        t.setNome("Furto");
        t.setAtivo(ativo);
        t.setExigeRevisao(exigeRevisao);
        return t;
    }

    private Ocorrencia ocorrencia(int id, int autorId, StatusOcorrencia status) {
        Ocorrencia o = new Ocorrencia();
        ReflectionTestUtils.setField(o, "id", id);
        o.setUsuarioId(autorId);
        o.setTipo(tipo);
        o.setTitulo("Titulo");
        o.setDescricao("Descricao");
        o.setLatitude(new BigDecimal("-8.0476000"));
        o.setLongitude(new BigDecimal("-34.8770000"));
        o.setStatus(status);
        o.setRelevancia(Relevancia.MEDIA);
        when(ocorrenciaRepository.findByIdAndExcluidaEmIsNull(id)).thenReturn(Optional.of(o));
        return o;
    }

    private CriarOcorrenciaRequest criar(Integer tipoId) {
        return new CriarOcorrenciaRequest(tipoId, " Titulo ", " Descricao ", new BigDecimal("-8.0476"),
                new BigDecimal("-34.8770"), " ", null, null);
    }

    private ApiException erro(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    @Test
    void criarAssociaAoUsuarioAutenticadoEDefineStatusPendente() {
        OcorrenciaResponse r = service.criar(AUTOR, criar(1));

        assertEquals(StatusOcorrencia.PENDENTE, r.status());
        assertEquals(10, r.autorId());
        assertEquals("Titulo", r.titulo());
        assertNull(r.endereco());
        verify(historico).registrar(any(), eq(10), eq(AcaoHistorico.CRIACAO), eq(null),
                eq(StatusOcorrencia.PENDENTE), any());
    }

    @Test
    void tipoSemRevisaoNascePublicada() {
        tipo = tipo(2, true, false);
        when(tipoRepository.findById(2)).thenReturn(Optional.of(tipo));

        assertEquals(StatusOcorrencia.PUBLICADA, service.criar(AUTOR, criar(2)).status());
    }

    @Test
    void criarRejeitaTipoInexistenteEInativoEDataFutura() {
        when(tipoRepository.findById(99)).thenReturn(Optional.empty());
        when(tipoRepository.findById(3)).thenReturn(Optional.of(tipo(3, false, true)));

        assertEquals(HttpStatus.BAD_REQUEST, erro(() -> service.criar(AUTOR, criar(99))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, erro(() -> service.criar(AUTOR, criar(3))).getStatus());

        var futura = new CriarOcorrenciaRequest(1, "t", "d", BigDecimal.ONE, BigDecimal.ONE, null,
                OffsetDateTime.now().plusDays(2), null);
        assertEquals(HttpStatus.BAD_REQUEST, erro(() -> service.criar(AUTOR, futura)).getStatus());
        verify(ocorrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    void listarValidaPaginacaoEIntervaloDeDatas() {
        var agora = OffsetDateTime.now();
        assertEquals(HttpStatus.BAD_REQUEST,
                erro(() -> service.listar(AUTOR, null, null, null, null, null, false, 0, 20)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                erro(() -> service.listar(AUTOR, null, null, null, null, null, false, 1, 101)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                erro(() -> service.listar(AUTOR, null, null, agora, agora.minusDays(1), null, false, 1, 20))
                        .getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                erro(() -> service.listar(AUTOR, "0,0,50,1", null, null, null, null, false, 1, 20)).getStatus());
    }

    @Test
    void usuarioComumNaoVeOcorrenciaNaoPublicadaDeOutraPessoa() {
        ocorrencia(5, 10, StatusOcorrencia.PENDENTE);

        assertEquals(HttpStatus.NOT_FOUND, erro(() -> service.buscar(OUTRO, 5)).getStatus());
        assertEquals(5, service.buscar(AUTOR, 5).id());
        assertEquals(5, service.buscar(GESTOR, 5).id());
    }

    @Test
    void autorIdFicaOcultoParaOutrosUsuariosEmOcorrenciaPublicada() {
        ocorrencia(6, 10, StatusOcorrencia.PUBLICADA);

        assertNull(service.buscar(OUTRO, 6).autorId());
        assertEquals(10, service.buscar(AUTOR, 6).autorId());
        assertEquals(10, service.buscar(ADMIN, 6).autorId());
    }

    @Test
    void usuarioComumNaoEditaOcorrenciaDeOutraPessoa() {
        ocorrencia(7, 10, StatusOcorrencia.PUBLICADA);
        var req = new AtualizarOcorrenciaRequest(null, "Novo", null, null, null, null, null, null, null);

        assertEquals(HttpStatus.FORBIDDEN, erro(() -> service.atualizar(OUTRO, 7, req)).getStatus());
        verify(ocorrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    void autorEditaPendenteMasNaoPublicada() {
        Ocorrencia pendente = ocorrencia(8, 10, StatusOcorrencia.PENDENTE);
        var req = new AtualizarOcorrenciaRequest(null, "Novo titulo", null, null, null, null, null, null, null);

        OcorrenciaResponse r = service.atualizar(AUTOR, 8, req);

        assertEquals("Novo titulo", r.titulo());
        assertEquals("Descricao", pendente.getDescricao()); // campo não enviado NÃO é apagado
        verify(historico).registrar(eq(8), eq(10), eq(AcaoHistorico.ATUALIZACAO), eq(null), eq(null),
                eq("Campos alterados: titulo."));

        ocorrencia(9, 10, StatusOcorrencia.PUBLICADA);
        assertEquals(HttpStatus.CONFLICT, erro(() -> service.atualizar(AUTOR, 9, req)).getStatus());
    }

    @Test
    void usuarioComumNaoAlteraRelevanciaNemStatus() {
        ocorrencia(12, 10, StatusOcorrencia.PENDENTE);
        var req = new AtualizarOcorrenciaRequest(null, null, null, null, null, null, null, null, Relevancia.ALTA);
        assertEquals(HttpStatus.FORBIDDEN, erro(() -> service.atualizar(AUTOR, 12, req)).getStatus());
    }

    @Test
    void gestorSoCorrigeTipoERelevanciaDeOutraPessoa() {
        Ocorrencia o = ocorrencia(13, 10, StatusOcorrencia.PUBLICADA);

        var classificacao = new AtualizarOcorrenciaRequest(null, null, null, null, null, null, null, null,
                Relevancia.ALTA);
        assertEquals(Relevancia.ALTA, service.atualizar(GESTOR, 13, classificacao).relevancia());
        assertEquals(Relevancia.ALTA, o.getRelevancia());

        var conteudo = new AtualizarOcorrenciaRequest(null, "Reescrito", null, null, null, null, null, null, null);
        assertEquals(HttpStatus.FORBIDDEN, erro(() -> service.atualizar(GESTOR, 13, conteudo)).getStatus());
    }

    @Test
    void patchVazioRetorna400() {
        var req = new AtualizarOcorrenciaRequest(null, null, null, null, null, null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, erro(() -> service.atualizar(AUTOR, 1, req)).getStatus());
    }

    @Test
    void exclusaoLogicaRespeitaPropriedadeEStatus() {
        Ocorrencia o = ocorrencia(14, 10, StatusOcorrencia.PENDENTE);
        ocorrencia(15, 10, StatusOcorrencia.PUBLICADA);
        // outro usuário comum: a ocorrência PENDENTE nem é visível (404); na PUBLICADA
        // falta permissão (403)
        assertEquals(HttpStatus.NOT_FOUND, erro(() -> service.excluir(OUTRO, 14)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, erro(() -> service.excluir(OUTRO, 15)).getStatus());
        // gestor não exclui relato alheio; autor não exclui publicada
        assertEquals(HttpStatus.FORBIDDEN, erro(() -> service.excluir(GESTOR, 15)).getStatus());
        assertEquals(HttpStatus.CONFLICT, erro(() -> service.excluir(AUTOR, 15)).getStatus());

        service.excluir(AUTOR, 14);
        assertTrue(o.isExcluida());
        assertEquals(10, o.getExcluidaPor());
        verify(historico).registrar(eq(14), eq(10), eq(AcaoHistorico.EXCLUSAO), eq(StatusOcorrencia.PENDENTE),
                eq(null), any());

        // administrador exclui qualquer status
        Ocorrencia publicada = ocorrencia(16, 10, StatusOcorrencia.PUBLICADA);
        service.excluir(ADMIN, 16);
        assertTrue(publicada.isExcluida());
    }
}