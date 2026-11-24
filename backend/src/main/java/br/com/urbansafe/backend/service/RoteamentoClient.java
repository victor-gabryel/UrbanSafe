package br.com.urbansafe.backend.service;

import java.util.List;

import br.com.urbansafe.backend.entity.PerfilDeslocamento;

/**
 * Serviço externo de roteamento (OSRM ou equivalente). A interface isola o provedor: trocar de serviço
 * significa implementar esta interface, sem mexer na regra de exposição (seções 10.2 e 15.1).
 */
public interface RoteamentoClient {

    /**
     * @param coordenadas pontos do percurso como pares {@code [longitude, latitude]} (ordem GeoJSON)
     */
    record RotaCalculada(double distanciaMetros, double duracaoSegundos, List<double[]> coordenadas) {
    }

    /**
     * @throws br.com.urbansafe.backend.exception.ApiException 422 se não existir caminho entre os pontos; 503 se o
     *                                                         serviço de roteamento estiver indisponível
     */
    RotaCalculada calcular(PerfilDeslocamento perfil, double origemLat, double origemLon, double destinoLat,
            double destinoLon);
}
