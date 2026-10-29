package br.com.urbansafe.backend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AcaoModeracaoTest {

    @Test
    void aprovarPublicaPendenteOcultaERejeitadaMasNaoPublicada() {
        assertTrue(AcaoModeracao.APROVAR.permitidaPara(StatusOcorrencia.PENDENTE));
        assertTrue(AcaoModeracao.APROVAR.permitidaPara(StatusOcorrencia.OCULTA));
        assertTrue(AcaoModeracao.APROVAR.permitidaPara(StatusOcorrencia.REJEITADA));
        assertFalse(AcaoModeracao.APROVAR.permitidaPara(StatusOcorrencia.PUBLICADA));
        assertEquals(StatusOcorrencia.PUBLICADA, AcaoModeracao.APROVAR.getStatusResultante());
    }

    @Test
    void rejeitadaNaoPodeSerRejeitadaNemOcultadaDeNovo() {
        assertFalse(AcaoModeracao.REJEITAR.permitidaPara(StatusOcorrencia.REJEITADA));
        assertFalse(AcaoModeracao.OCULTAR_TEMPORARIAMENTE.permitidaPara(StatusOcorrencia.REJEITADA));
        assertFalse(AcaoModeracao.OCULTAR_TEMPORARIAMENTE.permitidaPara(StatusOcorrencia.OCULTA));
        assertFalse(AcaoModeracao.SOLICITAR_CORRECAO.permitidaPara(StatusOcorrencia.REJEITADA));
    }

    @Test
    void justificativaEObrigatoriaExcetoNaAprovacao() {
        assertFalse(AcaoModeracao.APROVAR.exigeJustificativa());
        assertTrue(AcaoModeracao.REJEITAR.exigeJustificativa());
        assertTrue(AcaoModeracao.SOLICITAR_CORRECAO.exigeJustificativa());
        assertTrue(AcaoModeracao.OCULTAR_TEMPORARIAMENTE.exigeJustificativa());
    }
}