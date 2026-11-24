package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.exception.ApiException;

class LimitadorJanelaDeslizanteTest {

    @Test
    void bloqueiaAposOMaximoELiberaDepoisDaJanela() {
        AtomicLong relogio = new AtomicLong();
        var limitador = new LimitadorJanelaDeslizante(2, Duration.ofMinutes(1), "limite", relogio::get);

        limitador.registrar("ip");
        limitador.registrar("ip");
        assertEquals(429, assertThrows(ApiException.class, () -> limitador.registrar("ip")).getStatus().value());
        assertDoesNotThrow(() -> limitador.registrar("outro-ip"));

        relogio.addAndGet(Duration.ofMinutes(2).toNanos());
        assertDoesNotThrow(() -> limitador.registrar("ip"));
    }
}
