package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.entity.PerfilDeslocamento;
import br.com.urbansafe.backend.exception.ApiException;

class OsrmRoteamentoClientTest {

    private static final String OK = "{\"code\":\"Ok\",\"routes\":[{\"distance\":1234.5,\"duration\":900.2,"
            + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[-34.877,-8.0476],[-34.88,-8.05]]}}]}";

    private OsrmRoteamentoClient cliente(String resposta, AtomicReference<URI> capturada) {
        return new OsrmRoteamentoClient("http://foot/route/v1/driving/", "http://bike/route/v1/driving",
                uri -> {
                    capturada.set(uri);
                    return resposta;
                });
    }

    @Test
    void interpretaRotaEMontaUrlPorPerfilComLongitudePrimeiro() {
        var uri = new AtomicReference<URI>();
        var rota = cliente(OK, uri).calcular(PerfilDeslocamento.A_PE, -8.0476, -34.877, -8.05, -34.88);

        assertEquals(1234.5, rota.distanciaMetros());
        assertEquals(2, rota.coordenadas().size());
        assertTrue(uri.get().toString().startsWith("http://foot/route/v1/driving/-34.877000,-8.047600;-34.880000,-8.050000"));
        assertTrue(uri.get().toString().contains("geometries=geojson"));

        cliente(OK, uri).calcular(PerfilDeslocamento.BICICLETA, -8.0476, -34.877, -8.05, -34.88);
        assertTrue(uri.get().toString().startsWith("http://bike/"));
    }

    @Test
    void semCaminhoRetorna422() {
        var ex = assertThrows(ApiException.class, () -> cliente("{\"code\":\"NoRoute\"}", new AtomicReference<>())
                .calcular(PerfilDeslocamento.A_PE, 0, 0, 1, 1));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void falhaDeRedeOuRespostaInvalidaRetorna503() {
        var rede = new OsrmRoteamentoClient("http://a", "http://b", uri -> {
            throw new IOException("sem rede");
        });
        assertEquals(503, assertThrows(ApiException.class,
                () -> rede.calcular(PerfilDeslocamento.A_PE, 0, 0, 1, 1)).getStatus().value());

        for (String ruim : new String[] { "nao e json", "{\"code\":\"Ok\"}", "{\"code\":\"Ok\",\"routes\":[]}" }) {
            assertEquals(503, assertThrows(ApiException.class, () -> cliente(ruim, new AtomicReference<>())
                    .calcular(PerfilDeslocamento.A_PE, 0, 0, 1, 1)).getStatus().value(), ruim);
        }
    }
}
