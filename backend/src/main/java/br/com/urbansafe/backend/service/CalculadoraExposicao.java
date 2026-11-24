package br.com.urbansafe.backend.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.urbansafe.backend.entity.Relevancia;

/**
 * Indicador informativo de exposição a ocorrências registradas próximas a um percurso (seções 3.7 e 16.1).
 * Classe pura (sem Spring), para que a fórmula seja documentada e testada isoladamente.
 *
 * <p>
 * Cada ocorrência publicada dentro da distância de busca contribui com:
 *
 * <pre>
 * contribuicao = pesoRelevancia x decaimentoTemporal x fatorProximidade
 *   pesoRelevancia     BAIXA = 1, MEDIA = 2, ALTA = 3
 *   decaimentoTemporal 0,5 ^ (idadeEmDias / meiaVidaDias)          (relevância temporal)
 *   fatorProximidade   1 - distancia / distanciaBusca              (1 se a ponderação por distância estiver desligada)
 * soma   = soma das contribuições  (mais ocorrências próximas => soma maior: concentração)
 * indice = 100 x (1 - e^(-soma / escala))                          (0 a 100; satura em 100)
 * </pre>
 *
 * Os pesos e parâmetros são configuráveis (application.properties) e devem ser avaliados antes de o
 * indicador ser apresentado como classificação (seção 10.2). O resultado NÃO é probabilidade de crime
 * nem garantia de segurança.
 */
public final class CalculadoraExposicao {

    public static final int PESO_BAIXA = 1;
    public static final int PESO_MEDIA = 2;
    public static final int PESO_ALTA = 3;

    private static final double SEGUNDOS_POR_DIA = 86_400.0;

    private CalculadoraExposicao() {
    }

    /**
     * @param distanciaBuscaMetros   raio ao redor do percurso em que uma ocorrência é considerada
     * @param periodoDias            ocorrências mais antigas que isso são ignoradas
     * @param meiaVidaDias           a cada meia-vida o peso temporal de uma ocorrência cai pela metade
     * @param escala                 soma ponderada que equivale a ~63% do índice máximo
     * @param ponderarPorDistancia   se false, toda ocorrência dentro do raio conta com fator de proximidade 1
     */
    public record Parametros(double distanciaBuscaMetros, int periodoDias, double meiaVidaDias, double escala,
            boolean ponderarPorDistancia) {

        public Parametros {
            if (!(distanciaBuscaMetros > 0) || periodoDias <= 0 || !(meiaVidaDias > 0) || !(escala > 0)) {
                throw new IllegalArgumentException(
                        "Parâmetros de exposição devem ser positivos (distância, período, meia-vida e escala).");
            }
        }

        public Parametros comPonderacaoPorDistancia(boolean ponderar) {
            return new Parametros(distanciaBuscaMetros, periodoDias, meiaVidaDias, escala, ponderar);
        }
    }

    public record Item(Relevancia relevancia, OffsetDateTime dataReferencia, double distanciaMetros) {
    }

    /**
     * @param indice           0 a 100, com duas casas decimais
     * @param quantidade       ocorrências efetivamente consideradas (dentro do raio e do período)
     * @param somaPonderada    soma das contribuições antes da saturação
     */
    public record Resultado(double indice, int quantidade, double somaPonderada) {
    }

    public static int peso(Relevancia relevancia) {
        return switch (relevancia) {
            case BAIXA -> PESO_BAIXA;
            case MEDIA -> PESO_MEDIA;
            case ALTA -> PESO_ALTA;
        };
    }

    public static Resultado calcular(List<Item> itens, Parametros p, OffsetDateTime agora) {
        double soma = 0;
        int considerados = 0;

        for (Item item : itens) {
            if (item.distanciaMetros() > p.distanciaBuscaMetros() || item.distanciaMetros() < 0) {
                continue;
            }
            double idadeDias = Math.max(0.0,
                    Duration.between(item.dataReferencia(), agora).toSeconds() / SEGUNDOS_POR_DIA);
            if (idadeDias > p.periodoDias()) {
                continue;
            }

            double decaimento = Math.pow(0.5, idadeDias / p.meiaVidaDias());
            double proximidade = p.ponderarPorDistancia()
                    ? Math.max(0.0, 1.0 - item.distanciaMetros() / p.distanciaBuscaMetros())
                    : 1.0;

            soma += peso(item.relevancia()) * decaimento * proximidade;
            considerados++;
        }

        double indice = 100.0 * (1.0 - Math.exp(-soma / p.escala()));
        return new Resultado(arredondar(indice), considerados, arredondar(soma));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
