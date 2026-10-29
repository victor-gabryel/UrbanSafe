package br.com.urbansafe.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.exception.ApiException;

class BoundingBoxTest {

    @Test
    void parseValidoUsaOrdemLongitudeLatitude() {
        BoundingBox b = BoundingBox.parse("-34.95,-8.10,-34.85,-8.00", 5.0);
        assertEquals(-34.95, b.minLon());
        assertEquals(-8.10, b.minLat());
        assertEquals(-34.85, b.maxLon());
        assertEquals(-8.00, b.maxLat());
    }

    @Test
    void rejeitaAusenteMalFormadoENaoNumerico() {
        assertThrows(ApiException.class, () -> BoundingBox.parse(null, 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse(" ", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("1,2,3", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("a,b,c,d", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("NaN,0,1,1", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,0,Infinity,1", 5.0));
    }

    @Test
    void rejeitaLimitesGeograficosInvalidos() {
        assertThrows(ApiException.class, () -> BoundingBox.parse("-181,0,-180,1", 5.0)); // longitude
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,-91,1,0", 5.0)); // latitude
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,0,1,91", 5.0));
    }

    @Test
    void rejeitaMinimoNaoMenorQueMaximo() {
        assertThrows(ApiException.class, () -> BoundingBox.parse("1,0,0,1", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,1,1,0", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,0,0,1", 5.0));
    }

    @Test
    void rejeitaAreaMaiorQueOLimite() {
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,0,6,1", 5.0));
        assertThrows(ApiException.class, () -> BoundingBox.parse("0,0,1,6", 5.0));
    }
}