package br.com.urbansafe.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeografiaTest {

    @Test
    void distanciaEntrePontosDoRecife() {
        assertEquals(1307.0, Geografia.distanciaMetros(-8.0476, -34.8770, -8.0578, -34.8829), 5.0);
    }

    @Test
    void mesmoPontoTemDistanciaZero() {
        assertEquals(0.0, Geografia.distanciaMetros(-8.0, -34.0, -8.0, -34.0), 0.0001);
    }
}
