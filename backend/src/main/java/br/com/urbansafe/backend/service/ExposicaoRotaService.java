package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.urbansafe.backend.dto.RotaResponse;
import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository.OcorrenciaProxima;

/**
 * Cruza a geometria de uma rota com as ocorrências PUBLICADAS próximas (PostGIS) e calcula o indicador
 * informativo de exposição (fórmula em {@link CalculadoraExposicao}). Ocorrências pendentes, rejeitadas,
 * ocultas ou excluídas nunca entram no cálculo.
 */
@Service
public class ExposicaoRotaService {

    static final String AVISO = "Indicador informativo de exposição a ocorrências registradas e publicadas próximas "
            + "ao percurso. Depende dos registros disponíveis e não representa probabilidade real de crime nem "
            + "garantia de segurança.";

    private static final int LIMITE_OCORRENCIAS_CONSULTA = 5_000;

    private final OcorrenciaConsultaRepository consultaRepository;
    private final CalculadoraExposicao.Parametros parametrosBase;
    private final double coberturaMetros;
    private final int minimoCobertura;

    public ExposicaoRotaService(
            OcorrenciaConsultaRepository consultaRepository,
            @Value("${app.rotas.exposicao.distancia-metros:150}") double distanciaMetros,
            @Value("${app.rotas.exposicao.periodo-dias:180}") int periodoDias,
            @Value("${app.rotas.exposicao.meia-vida-dias:60}") double meiaVidaDias,
            @Value("${app.rotas.exposicao.escala:6.0}") double escala,
            @Value("${app.rotas.exposicao.cobertura-metros:2000}") double coberturaMetros,
            @Value("${app.rotas.exposicao.minimo-cobertura:1}") int minimoCobertura) {
        this.consultaRepository = consultaRepository;
        this.parametrosBase = new CalculadoraExposicao.Parametros(distanciaMetros, periodoDias, meiaVidaDias, escala,
                true);
        this.coberturaMetros = Math.max(coberturaMetros, distanciaMetros);
        this.minimoCobertura = minimoCobertura;
    }

    /** Resposta para quem desligou o cálculo de exposição nas preferências. */
    public RotaResponse.Exposicao naoSolicitada(boolean ponderarPorDistancia) {
        return montar(null, 0, null, "Cálculo de exposição desativado nas preferências desta consulta.",
                ponderarPorDistancia);
    }

    /**
     * @param coordenadas pontos do percurso como [longitude, latitude]
     */
    public RotaResponse.Exposicao avaliar(List<double[]> coordenadas, boolean ponderarPorDistancia) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        CalculadoraExposicao.Parametros parametros = parametrosBase.comPonderacaoPorDistancia(ponderarPorDistancia);

        OffsetDateTime desde = agora.minusDays(parametros.periodoDias());
        List<OcorrenciaProxima> proximas = consultaRepository.proximasDoPercurso(
                geoJsonLinha(coordenadas), coberturaMetros, desde, LIMITE_OCORRENCIAS_CONSULTA);
        OffsetDateTime atualizadoEm = consultaRepository.ultimaAtualizacaoPublicada();

        // "Dados suficientes": é preciso haver ao menos algum registro publicado na região do percurso;
        // sem cobertura, um índice 0 seria enganoso (ausência de dados não é ausência de ocorrências).
        if (proximas.size() < minimoCobertura) {
            return montar(null, 0, atualizadoEm,
                    "Sem dados suficientes: não há ocorrências publicadas na região deste percurso no período "
                            + "considerado.",
                    ponderarPorDistancia);
        }

        List<CalculadoraExposicao.Item> itens = proximas.stream()
                .map(o -> new CalculadoraExposicao.Item(o.relevancia(), o.dataReferencia(), o.distanciaMetros()))
                .toList();
        CalculadoraExposicao.Resultado resultado = CalculadoraExposicao.calcular(itens, parametros, agora);

        return montar(resultado.indice(), resultado.quantidade(), atualizadoEm, null, ponderarPorDistancia);
    }

    private RotaResponse.Exposicao montar(Double indice, int quantidade, OffsetDateTime atualizadoEm,
            String observacao, boolean ponderarPorDistancia) {
        return new RotaResponse.Exposicao(
                indice,
                quantidade,
                (int) Math.round(parametrosBase.distanciaBuscaMetros()),
                parametrosBase.periodoDias(),
                atualizadoEm,
                observacao,
                AVISO,
                new RotaResponse.Parametros(
                        parametrosBase.meiaVidaDias(),
                        parametrosBase.escala(),
                        ponderarPorDistancia,
                        Map.of(
                                Relevancia.BAIXA.name(), CalculadoraExposicao.PESO_BAIXA,
                                Relevancia.MEDIA.name(), CalculadoraExposicao.PESO_MEDIA,
                                Relevancia.ALTA.name(), CalculadoraExposicao.PESO_ALTA)));
    }

    /** LineString em GeoJSON; números com ponto decimal independentemente do idioma do servidor. */
    static String geoJsonLinha(List<double[]> coordenadas) {
        StringBuilder sb = new StringBuilder("{\"type\":\"LineString\",\"coordinates\":[");
        for (int i = 0; i < coordenadas.size(); i++) {
            double[] c = coordenadas.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "[%.7f,%.7f]", c[0], c[1]));
        }
        return sb.append("]}").toString();
    }
}
