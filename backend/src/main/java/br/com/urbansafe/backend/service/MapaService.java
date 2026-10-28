package br.com.urbansafe.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.FiltroOcorrencia;
import br.com.urbansafe.backend.dto.MapaCalorResponse;
import br.com.urbansafe.backend.dto.MapaOcorrenciaResponse;
import br.com.urbansafe.backend.dto.MapaOcorrenciasResponse;
import br.com.urbansafe.backend.dto.PontoCalorResponse;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.util.Filtros;

/**
 * Dados para o mapa e o mapa de calor. Usa somente ocorrências PUBLICADAS (a equipe pode escolher outro
 * status), exige bbox e limita área, quantidade de marcadores e de células.
 */
@Service
public class MapaService {

    private final OcorrenciaConsultaRepository consultaRepository;
    private final int maxMarcadores;
    private final double maxLadoBbox;
    private final int maxCelulas;
    private final double celulaMin;
    private final double celulaMax;
    private final double celulaPadrao;

    public MapaService(
            OcorrenciaConsultaRepository consultaRepository,
            @Value("${app.mapa.max-resultados:500}") int maxMarcadores,
            @Value("${app.mapa.max-lado-bbox-graus:5.0}") double maxLadoBbox,
            @Value("${app.mapa.calor.max-celulas:2000}") int maxCelulas,
            @Value("${app.mapa.calor.celula-min-graus:0.0005}") double celulaMin,
            @Value("${app.mapa.calor.celula-max-graus:0.05}") double celulaMax,
            @Value("${app.mapa.calor.celula-padrao-graus:0.005}") double celulaPadrao) {

        this.consultaRepository = consultaRepository;
        this.maxMarcadores = maxMarcadores;
        this.maxLadoBbox = maxLadoBbox;
        this.maxCelulas = maxCelulas;
        this.celulaMin = celulaMin;
        this.celulaMax = celulaMax;
        this.celulaPadrao = celulaPadrao;
    }

    @Transactional(readOnly = true)
    public MapaOcorrenciasResponse ocorrencias(UsuarioPrincipal principal, String bbox, Integer tipoId,
            OffsetDateTime dataInicio, OffsetDateTime dataFim, StatusOcorrencia status, Integer limite) {

        int limiteEfetivo = limite == null ? maxMarcadores : limite;
        if (limiteEfetivo < 1 || limiteEfetivo > maxMarcadores) {
            throw ApiException.badRequest("O parâmetro limit deve estar entre 1 e " + maxMarcadores + ".");
        }

        FiltroOcorrencia filtro = filtro(principal, bbox, tipoId, dataInicio, dataFim, status);
        var escopo = escopo(principal);

        // Busca limite+1 para saber se houve truncamento sem uma segunda consulta de contagem.
        List<MapaOcorrenciaResponse> itens = new ArrayList<>(
                consultaRepository.marcadores(filtro, escopo, limiteEfetivo + 1));
        boolean truncado = itens.size() > limiteEfetivo;
        if (truncado) {
            itens = new ArrayList<>(itens.subList(0, limiteEfetivo));
        }
        return new MapaOcorrenciasResponse(itens, limiteEfetivo, truncado);
    }

    @Transactional(readOnly = true)
    public MapaCalorResponse calor(UsuarioPrincipal principal, String bbox, Integer tipoId,
            OffsetDateTime dataInicio, OffsetDateTime dataFim, StatusOcorrencia status, Double celula) {

        double celulaEfetiva = celula == null ? celulaPadrao : celula;
        if (!Double.isFinite(celulaEfetiva) || celulaEfetiva < celulaMin || celulaEfetiva > celulaMax) {
            throw ApiException.badRequest(
                    "O parâmetro celula (graus) deve estar entre " + celulaMin + " e " + celulaMax + ".");
        }

        FiltroOcorrencia filtro = filtro(principal, bbox, tipoId, dataInicio, dataFim, status);
        var escopo = escopo(principal);

        List<PontoCalorResponse> celulas = new ArrayList<>(
                consultaRepository.calor(filtro, escopo, celulaEfetiva, maxCelulas + 1));
        boolean truncado = celulas.size() > maxCelulas;
        if (truncado) {
            celulas = new ArrayList<>(celulas.subList(0, maxCelulas));
        }

        long maior = celulas.stream().mapToLong(PontoCalorResponse::quantidade).max().orElse(0L);
        List<PontoCalorResponse> pontos = celulas.stream()
                .map(c -> new PontoCalorResponse(c.latitude(), c.longitude(), c.quantidade(),
                        maior == 0 ? 0.0 : Math.round(c.quantidade() * 1000.0 / maior) / 1000.0))
                .toList();

        long total = consultaRepository.contar(filtro, escopo);
        return new MapaCalorResponse(celulaEfetiva, total, pontos, truncado, MapaCalorResponse.AVISO_PADRAO);
    }

    private FiltroOcorrencia filtro(UsuarioPrincipal principal, String bbox, Integer tipoId,
            OffsetDateTime dataInicio, OffsetDateTime dataFim, StatusOcorrencia status) {

        // Usuário comum só vê dados públicos (PUBLICADA); pedir outro status é falta de permissão, não filtro vazio.
        if (status != null && status != StatusOcorrencia.PUBLICADA && !principal.isEquipe()) {
            throw ApiException.forbidden("Você não tem permissão para filtrar o mapa por este status.");
        }
        return Filtros.montar(bbox, maxLadoBbox, true, tipoId, dataInicio, dataFim, status, false);
    }

    private OcorrenciaConsultaRepository.Escopo escopo(UsuarioPrincipal principal) {
        return new OcorrenciaConsultaRepository.Escopo(principal.id(), principal.isEquipe(), true);
    }
}
