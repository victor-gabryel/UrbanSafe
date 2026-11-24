package br.com.urbansafe.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.urbansafe.backend.entity.PerfilDeslocamento;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.util.JsonSimples;

/**
 * Cliente da API HTTP do OSRM ({@code /route/v1/...}). Os endereços-base por perfil são configuráveis
 * (application.properties), pois o servidor demo do OSRM só roteia para carros: para caminhada e bicicleta é
 * preciso apontar para uma instância com os perfis foot/bike (ex.: routing.openstreetmap.de ou OSRM próprio).
 * Os servidores públicos têm limites de uso; veja o README.
 */
@Component
public class OsrmRoteamentoClient implements RoteamentoClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmRoteamentoClient.class);

    /** Busca o corpo da resposta de uma URL. Separado para permitir testes sem rede. */
    @FunctionalInterface
    interface Buscador {

        String buscar(URI uri) throws IOException, InterruptedException;
    }

    private final String urlAPe;
    private final String urlBicicleta;
    private final Buscador buscador;

    @Autowired
    public OsrmRoteamentoClient(
            @Value("${app.rotas.osrm.url-a-pe}") String urlAPe,
            @Value("${app.rotas.osrm.url-bicicleta}") String urlBicicleta,
            @Value("${app.rotas.osrm.timeout-segundos:10}") long timeoutSegundos,
            @Value("${app.rotas.osrm.user-agent:UrbanSafe}") String userAgent) {
        this(urlAPe, urlBicicleta, buscadorHttp(timeoutSegundos, userAgent));
    }

    OsrmRoteamentoClient(String urlAPe, String urlBicicleta, Buscador buscador) {
        this.urlAPe = semBarraFinal(urlAPe);
        this.urlBicicleta = semBarraFinal(urlBicicleta);
        this.buscador = buscador;
    }

    @Override
    public RotaCalculada calcular(PerfilDeslocamento perfil, double origemLat, double origemLon,
            double destinoLat, double destinoLon) {
        String base = perfil == PerfilDeslocamento.BICICLETA ? urlBicicleta : urlAPe;
        URI uri = URI.create(base + "/"
                + String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f", origemLon, origemLat, destinoLon, destinoLat)
                + "?overview=full&geometries=geojson&alternatives=false&steps=false");

        String corpo;
        try {
            corpo = buscador.buscar(uri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw indisponivel(e);
        } catch (IOException | RuntimeException e) {
            throw indisponivel(e);
        }

        return interpretar(corpo);
    }

    // Visível ao pacote para teste.
    RotaCalculada interpretar(String corpo) {
        Object raiz;
        try {
            raiz = JsonSimples.ler(corpo);
        } catch (IllegalArgumentException e) {
            throw indisponivel(e);
        }

        if (!(raiz instanceof Map<?, ?> mapa)) {
            throw indisponivel(new IllegalStateException("Resposta inesperada do serviço de roteamento."));
        }

        Object codigo = mapa.get("code");
        if ("NoRoute".equals(codigo) || "NoSegment".equals(codigo)) {
            throw ApiException.unprocessableEntity(
                    "Não foi encontrado um caminho entre a origem e o destino para este modo de deslocamento.");
        }
        if (!"Ok".equals(codigo)) {
            throw indisponivel(new IllegalStateException("Código de resposta do roteamento: " + codigo));
        }

        try {
            Map<?, ?> rota = (Map<?, ?>) ((List<?>) mapa.get("routes")).get(0);
            double distancia = ((Number) rota.get("distance")).doubleValue();
            double duracao = ((Number) rota.get("duration")).doubleValue();
            List<?> pontos = (List<?>) ((Map<?, ?>) rota.get("geometry")).get("coordinates");

            List<double[]> coordenadas = new ArrayList<>(pontos.size());
            for (Object ponto : pontos) {
                List<?> par = (List<?>) ponto;
                coordenadas.add(new double[] { ((Number) par.get(0)).doubleValue(),
                        ((Number) par.get(1)).doubleValue() });
            }
            if (coordenadas.size() < 2) {
                throw new IllegalStateException("Geometria com menos de dois pontos.");
            }
            return new RotaCalculada(distancia, duracao, coordenadas);
        } catch (ClassCastException | NullPointerException | IndexOutOfBoundsException | IllegalStateException e) {
            throw indisponivel(e);
        }
    }

    private ApiException indisponivel(Exception causa) {
        // Detalhes técnicos só no log: a resposta ao cliente não revela a infraestrutura.
        log.warn("Falha ao consultar o serviço de roteamento: {}", causa.toString());
        return ApiException.serviceUnavailable(
                "Serviço de roteamento indisponível no momento. Tente novamente em instantes.");
    }

    private static String semBarraFinal(String url) {
        String limpa = url.trim();
        while (limpa.endsWith("/")) {
            limpa = limpa.substring(0, limpa.length() - 1);
        }
        return limpa;
    }

    private static Buscador buscadorHttp(long timeoutSegundos, String userAgent) {
        Duration timeout = Duration.ofSeconds(Math.max(1, timeoutSegundos));
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        return uri -> {
            HttpRequest requisicao = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
            int status = resposta.statusCode();
            // O OSRM devolve JSON com "code" também em alguns 4xx (ex.: NoRoute em 400); deixe o corpo decidir.
            if (status >= 500 || resposta.body() == null || resposta.body().isBlank()) {
                throw new IOException("HTTP " + status);
            }
            return resposta.body();
        };
    }
}
