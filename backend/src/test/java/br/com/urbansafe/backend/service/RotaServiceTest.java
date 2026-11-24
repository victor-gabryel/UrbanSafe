package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.dto.CalcularRotaRequest;
import br.com.urbansafe.backend.dto.PontoRequest;
import br.com.urbansafe.backend.dto.PreferenciasRotaRequest;
import br.com.urbansafe.backend.dto.RotaResponse;
import br.com.urbansafe.backend.entity.PerfilDeslocamento;
import br.com.urbansafe.backend.entity.Rota;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.RotaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

class RotaServiceTest {

    private RoteamentoClient roteamento;
    private ExposicaoRotaService exposicao;
    private RotaRepository repository;
    private RotaService service;

    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(7, "a@b.com", "USUARIO");

    @BeforeEach
    void preparar() {
        roteamento = mock(RoteamentoClient.class);
        exposicao = mock(ExposicaoRotaService.class);
        repository = mock(RotaRepository.class);
        service = new RotaService(roteamento, exposicao, repository, 30, 100, 10);

        when(roteamento.calcular(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RoteamentoClient.RotaCalculada(1300.4, 960.0,
                        List.of(new double[] { -34.877, -8.0476 }, new double[] { -34.8829, -8.0578 })));
        when(exposicao.avaliar(any(), anyBoolean())).thenReturn(exposicaoComIndice(42.0));
        when(repository.save(any(Rota.class))).thenAnswer(inv -> {
            Rota r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 99);
            ReflectionTestUtils.setField(r, "dataCalculo", java.time.OffsetDateTime.now());
            return r;
        });
    }

    private static RotaResponse.Exposicao exposicaoComIndice(Double indice) {
        return new RotaResponse.Exposicao(indice, 3, 150, 180, null, null, "aviso", null);
    }

    private static PontoRequest ponto(String lat, String lon) {
        return new PontoRequest(new BigDecimal(lat), new BigDecimal(lon), "  Marco Zero ");
    }

    private static CalcularRotaRequest pedido(PreferenciasRotaRequest prefs) {
        return new CalcularRotaRequest(ponto("-8.0476", "-34.8770"), ponto("-8.0578", "-34.8829"), null, prefs);
    }

    @Test
    void visitanteRecebeRotaSemGravar() {
        RotaResponse r = service.calcular(null, pedido(null), "1.1.1.1");

        assertNull(r.id());
        assertEquals(PerfilDeslocamento.A_PE, r.perfil());
        assertEquals(42.0, r.exposicao().indice());
        assertEquals(2, r.geometria().coordenadas().size());
        verify(repository, never()).save(any());
    }

    @Test
    void usuarioAutenticadoTemConsultaGravadaComRotuloLimpo() {
        RotaResponse r = service.calcular(USUARIO, pedido(null), "1.1.1.1");

        assertEquals(99, r.id());
        assertEquals("Marco Zero", r.origem().rotulo());
        verify(repository).save(any(Rota.class));
    }

    @Test
    void preferenciaDesligaOCalculoDeExposicao() {
        when(exposicao.naoSolicitada(anyBoolean())).thenReturn(exposicaoComIndice(null));

        RotaResponse r = service.calcular(null, pedido(new PreferenciasRotaRequest(false, true)), "ip");

        assertNull(r.exposicao().indice());
        verify(exposicao, never()).avaliar(any(), anyBoolean());
    }

    @Test
    void rejeitaPontosIguaisEDistanciaExcessiva() {
        var iguais = new CalcularRotaRequest(ponto("-8.0476", "-34.8770"), ponto("-8.0476", "-34.8770"), null, null);
        assertEquals(400, assertThrows(ApiException.class, () -> service.calcular(null, iguais, "ip"))
                .getStatus().value());

        var longe = new CalcularRotaRequest(ponto("-8.0476", "-34.8770"), ponto("-23.55", "-46.63"), null, null);
        assertEquals(400, assertThrows(ApiException.class, () -> service.calcular(null, longe, "ip"))
                .getStatus().value());
        verify(roteamento, never()).calcular(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void limitaConsultasPorIp() {
        var limitado = new RotaService(roteamento, exposicao, repository, 30, 1, 10);
        limitado.calcular(null, pedido(null), "9.9.9.9");
        assertEquals(429, assertThrows(ApiException.class, () -> limitado.calcular(null, pedido(null), "9.9.9.9"))
                .getStatus().value());
    }

    @Test
    void rotaDeOutroUsuarioResponde404() {
        when(repository.findByIdAndUsuarioId(5, 7)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ApiException.class, () -> service.buscar(USUARIO, 5)).getStatus().value());
        assertNotNull(service);
    }
}
