package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import br.com.urbansafe.backend.dto.FiltroOcorrencia;
import br.com.urbansafe.backend.dto.MapaCalorResponse;
import br.com.urbansafe.backend.dto.MapaOcorrenciaResponse;
import br.com.urbansafe.backend.dto.MapaOcorrenciasResponse;
import br.com.urbansafe.backend.dto.PontoCalorResponse;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository.Escopo;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

class MapaServiceTest {

    private static final String BBOX = "-34.95,-8.10,-34.85,-8.00";
    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(3, "u@x.com", "USUARIO");
    private static final UsuarioPrincipal GESTOR = new UsuarioPrincipal(2, "g@x.com", "GESTOR");

    private OcorrenciaConsultaRepository repositorio;
    private MapaService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(OcorrenciaConsultaRepository.class);
        service = new MapaService(repositorio, 3, 5.0, 2, 0.0005, 0.05, 0.005);
    }

    private static MapaOcorrenciaResponse marcador(int id) {
        return new MapaOcorrenciaResponse(id, 1, "Furto", "t" + id, new BigDecimal("-8.05"),
                new BigDecimal("-34.88"), StatusOcorrencia.PUBLICADA, null, null);
    }

    @Test
    void visitanteSemPrincipalConsultaMarcadoresComEscopoPublico() {
        when(repositorio.marcadores(any(), any(), anyInt())).thenReturn(List.of(marcador(1)));

        MapaOcorrenciasResponse resposta = service.ocorrencias(null, BBOX, null, null, null, null, null);

        assertEquals(1, resposta.itens().size());
        ArgumentCaptor<Escopo> escopo = ArgumentCaptor.forClass(Escopo.class);
        verify(repositorio).marcadores(any(FiltroOcorrencia.class), escopo.capture(), eq(4));
        assertEquals(null, escopo.getValue().visualizadorId());
        assertFalse(escopo.getValue().equipe());
        assertTrue(escopo.getValue().mapa());
    }

    @Test
    void visitanteNaoPodePedirStatusDiferenteDePublicada() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.ocorrencias(null, BBOX, null, null, null, StatusOcorrencia.PENDENTE, null));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());

        e = assertThrows(ApiException.class,
                () -> service.calor(USUARIO, BBOX, null, null, null, StatusOcorrencia.OCULTA, null));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void visitantePodePedirExplicitamentePublicada() {
        when(repositorio.marcadores(any(), any(), anyInt())).thenReturn(List.of());
        MapaOcorrenciasResponse r = service.ocorrencias(null, BBOX, null, null, null, StatusOcorrencia.PUBLICADA, null);
        assertTrue(r.itens().isEmpty());
    }

    @Test
    void equipePodeFiltrarPorOutroStatus() {
        when(repositorio.marcadores(any(), any(), anyInt())).thenReturn(List.of());
        MapaOcorrenciasResponse r = service.ocorrencias(GESTOR, BBOX, null, null, null, StatusOcorrencia.PENDENTE, null);
        assertTrue(r.itens().isEmpty());
    }

    @Test
    void marcadoresSaoTruncadosNoLimiteEFlagIndicaExcesso() {
        when(repositorio.marcadores(any(), any(), anyInt()))
                .thenReturn(new ArrayList<>(List.of(marcador(1), marcador(2), marcador(3), marcador(4))));

        MapaOcorrenciasResponse r = service.ocorrencias(null, BBOX, null, null, null, null, null);

        assertEquals(3, r.itens().size());
        assertTrue(r.truncado());
        assertEquals(3, r.limite());
    }

    @Test
    void bboxObrigatorioELimiteValidado() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> service.ocorrencias(null, null, null, null, null, null, null))
                        .getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> service.ocorrencias(null, BBOX, null, null, null, null, 0))
                        .getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> service.ocorrencias(null, BBOX, null, null, null, null, 4))
                        .getStatus());
    }

    @Test
    void calorNormalizaIntensidadePelaMaiorCelulaEAceitaVisitante() {
        when(repositorio.calor(any(), any(), anyDouble(), anyInt())).thenReturn(List.of(
                new PontoCalorResponse(-8.04, -34.87, 10, 0), new PontoCalorResponse(-8.05, -34.88, 5, 0)));
        when(repositorio.contar(any(), any())).thenReturn(15L);

        MapaCalorResponse r = service.calor(null, BBOX, null, null, null, null, null);

        assertEquals(0.005, r.celulaGraus());
        assertEquals(15L, r.totalOcorrencias());
        assertEquals(1.0, r.pontos().get(0).intensidade());
        assertEquals(0.5, r.pontos().get(1).intensidade());
        assertFalse(r.truncado());
    }

    @Test
    void calorRejeitaCelulaForaDoIntervalo() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> service.calor(null, BBOX, null, null, null, null, 0.0001))
                        .getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> service.calor(null, BBOX, null, null, null, null, 1.0))
                        .getStatus());
    }
}
