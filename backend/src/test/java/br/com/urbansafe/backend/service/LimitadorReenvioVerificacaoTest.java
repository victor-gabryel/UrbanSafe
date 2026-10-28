package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.urbansafe.backend.exception.ApiException;

class LimitadorReenvioVerificacaoTest {

    private final AtomicLong agora = new AtomicLong(0);
    private final LimitadorReenvioVerificacao limitador =
            new LimitadorReenvioVerificacao(3, Duration.ofMinutes(10), agora::get);

    @Test
    void bloqueiaAposOMaximoDentroDaJanela() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip-1");
        }

        ApiException ex = assertThrows(ApiException.class, () -> limitador.registrar("ip-1"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void chavesDiferentesTemContagensIndependentes() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip-1");
        }

        assertDoesNotThrow(() -> limitador.registrar("ip-2"));
    }

    @Test
    void liberaDepoisQueAJanelaPassa() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip-1");
        }

        agora.addAndGet(Duration.ofMinutes(10).toNanos());

        assertDoesNotThrow(() -> limitador.registrar("ip-1"));
    }
}
