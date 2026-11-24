package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.urbansafe.backend.entity.PerfilDeslocamento;

@Schema(description = "Rota calculada, com geometria e indicador informativo de exposição")
public record RotaResponse(
                @Schema(description = "Id no histórico; nulo quando a consulta foi anônima e não foi gravada") Integer id,
                PerfilDeslocamento perfil,
                Ponto origem,
                Ponto destino,
                @Schema(description = "Distância do percurso em metros") double distanciaMetros,
                @Schema(description = "Duração estimada em segundos") double duracaoSegundos,
                Geometria geometria,
                Exposicao exposicao,
                OffsetDateTime calculadaEm) {

    public record Ponto(double latitude, double longitude, String rotulo) {
    }

    @Schema(description = "LineString no formato GeoJSON: cada coordenada é [longitude, latitude]")
    public record Geometria(String tipo, List<List<Double>> coordenadas) {
    }

    @Schema(description = "Indicador informativo de exposição a ocorrências registradas. Não é probabilidade real de "
                    + "crime nem garantia de segurança.")
    public record Exposicao(
                    @Schema(description = "0 a 100; nulo quando não calculado ou sem dados suficientes") Double indice,
                    @Schema(description = "Ocorrências publicadas dentro do raio de busca e do período") int ocorrenciasProximas,
                    int distanciaBuscaMetros,
                    int periodoDias,
                    @Schema(description = "Última atualização de uma ocorrência publicada (cobertura dos dados)") OffsetDateTime dadosAtualizadosEm,
                    @Schema(description = "Explica por que o índice não foi calculado, quando for o caso") String observacao,
                    String aviso,
                    Parametros parametros) {
    }

    @Schema(description = "Parâmetros usados no cálculo (fórmula em CalculadoraExposicao e docs/API-ROTAS.md)")
    public record Parametros(
                    double meiaVidaDias,
                    double escala,
                    boolean ponderadoPorDistancia,
                    Map<String, Integer> pesosRelevancia) {
    }
}
