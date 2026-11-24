package br.com.urbansafe.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonSimplesTest {

    @Test
    void leObjetosVetoresETipos() {
        Object o = JsonSimples.ler("{\"a\":[1,2.5,-3e1],\"b\":\"x\\u00e9\\n\",\"c\":null,\"d\":true}");
        Map<?, ?> m = (Map<?, ?>) o;
        assertEquals(List.of(1.0, 2.5, -30.0), m.get("a"));
        assertEquals("xé\n", m.get("b"));
        assertNull(m.get("c"));
        assertEquals(Boolean.TRUE, m.get("d"));
    }

    @Test
    void rejeitaJsonInvalido() {
        for (String ruim : new String[] { "{", "[1,]", "{\"a\" 1}", "tru", "\"x", "{} x", "" }) {
            assertThrows(IllegalArgumentException.class, () -> JsonSimples.ler(ruim), ruim);
        }
    }

    @Test
    void limitaProfundidade() {
        String fundo = "[".repeat(200) + "]".repeat(200);
        assertThrows(IllegalArgumentException.class, () -> JsonSimples.ler(fundo));
    }
}
