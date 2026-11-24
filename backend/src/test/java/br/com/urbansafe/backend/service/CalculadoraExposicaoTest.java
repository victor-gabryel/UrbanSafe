package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.entity.Relevancia;

class CalculadoraExposicaoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 11, 23, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final CalculadoraExposicao.Parametros PADRAO = new CalculadoraExposicao.Parametros(150, 180, 60, 6,
            true);

    private static CalculadoraExposicao.Item item(Relevancia r, int idadeDias, double distancia) {
        return new CalculadoraExposicao.Item(r, AGORA.minusDays(idadeDias), distancia);
    }

    @Test
    void semOcorrenciasIndiceZero() {
        assertEquals(0.0, CalculadoraExposicao.calcular(List.of(), PADRAO, AGORA).indice());
    }

    @Test
    void ocorrenciaAltaRecenteNoPercursoPesaTres() {
        var r = CalculadoraExposicao.calcular(List.of(item(Relevancia.ALTA, 0, 0)), PADRAO, AGORA);
        assertEquals(3.0, r.somaPonderada(), 0.001);
        assertEquals(39.35, r.indice(), 0.01);
        assertEquals(1, r.quantidade());
    }

    @Test
    void pesoCaiPelaMetadeAposUmaMeiaVida() {
        var r = CalculadoraExposicao.calcular(List.of(item(Relevancia.ALTA, 60, 0)), PADRAO, AGORA);
        assertEquals(1.5, r.somaPonderada(), 0.001);
    }

    @Test
    void ignoraForaDoRaioEForaDoPeriodo() {
        var r = CalculadoraExposicao.calcular(
                List.of(item(Relevancia.ALTA, 0, 151), item(Relevancia.ALTA, 181, 0)), PADRAO, AGORA);
        assertEquals(0, r.quantidade());
        assertEquals(0.0, r.indice());
    }

    @Test
    void ponderacaoPorDistanciaPodeSerDesligada() {
        var com = CalculadoraExposicao.calcular(List.of(item(Relevancia.MEDIA, 0, 75)), PADRAO, AGORA);
        var sem = CalculadoraExposicao.calcular(List.of(item(Relevancia.MEDIA, 0, 75)),
                PADRAO.comPonderacaoPorDistancia(false), AGORA);
        assertEquals(1.0, com.somaPonderada(), 0.001);
        assertEquals(2.0, sem.somaPonderada(), 0.001);
    }

    @Test
    void indiceNuncaPassaDeCem() {
        List<CalculadoraExposicao.Item> muitas = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            muitas.add(item(Relevancia.ALTA, 0, 0));
        }
        assertTrue(CalculadoraExposicao.calcular(muitas, PADRAO, AGORA).indice() <= 100.0);
    }

    @Test
    void maisOcorrenciasAumentamOIndice() {
        var uma = CalculadoraExposicao.calcular(List.of(item(Relevancia.MEDIA, 5, 10)), PADRAO, AGORA);
        var tres = CalculadoraExposicao.calcular(
                List.of(item(Relevancia.MEDIA, 5, 10), item(Relevancia.MEDIA, 5, 10), item(Relevancia.MEDIA, 5, 10)),
                PADRAO, AGORA);
        assertTrue(tres.indice() > uma.indice());
    }

    @Test
    void parametrosInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class,
                () -> new CalculadoraExposicao.Parametros(0, 180, 60, 6, true));
        assertThrows(IllegalArgumentException.class,
                () -> new CalculadoraExposicao.Parametros(150, 180, 60, 0, true));
    }
}
