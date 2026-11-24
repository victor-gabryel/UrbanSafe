package br.com.urbansafe.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.CalcularRotaRequest;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.dto.PontoRequest;
import br.com.urbansafe.backend.dto.RotaResponse;
import br.com.urbansafe.backend.dto.RotaResumoResponse;
import br.com.urbansafe.backend.entity.PerfilDeslocamento;
import br.com.urbansafe.backend.entity.Rota;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.RotaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.util.Geografia;

/**
 * Cálculo de rotas (público, com limite de requisições) e histórico do usuário autenticado.
 * Consultas anônimas são calculadas e devolvidas, mas NUNCA gravadas (minimização de dados de localização).
 * A chamada ao serviço de roteamento não ocorre dentro de transação de banco.
 */
@Service
public class RotaService {

    static final int LIMITE_MAXIMO_PAGINA = 100;
    private static final double DISTANCIA_MINIMA_METROS = 10.0;
    private static final int ESCALA_COORDENADA = 7;
    private static final String MSG_LIMITE = "Muitas consultas de rota em pouco tempo. Aguarde alguns minutos e tente novamente.";

    private final RoteamentoClient roteamento;
    private final ExposicaoRotaService exposicaoService;
    private final RotaRepository rotaRepository;
    private final LimitadorJanelaDeslizante limitador;
    private final double distanciaMaximaMetros;

    public RotaService(
            RoteamentoClient roteamento,
            ExposicaoRotaService exposicaoService,
            RotaRepository rotaRepository,
            @Value("${app.rotas.distancia-maxima-km:30}") double distanciaMaximaKm,
            @Value("${app.rotas.limite-requisicoes:30}") int limiteRequisicoes,
            @Value("${app.rotas.limite-janela-minutos:10}") long janelaMinutos) {
        this.roteamento = roteamento;
        this.exposicaoService = exposicaoService;
        this.rotaRepository = rotaRepository;
        this.limitador = new LimitadorJanelaDeslizante(limiteRequisicoes, Duration.ofMinutes(janelaMinutos),
                MSG_LIMITE);
        this.distanciaMaximaMetros = distanciaMaximaKm * 1000.0;
    }

    /**
     * @param principal   usuário autenticado ou null (visitante)
     * @param chaveLimite identificador de quem consulta (IP) para o limite de requisições
     */
    public RotaResponse calcular(UsuarioPrincipal principal, CalcularRotaRequest request, String chaveLimite) {
        limitador.registrar("rotas:" + chaveLimite);

        PerfilDeslocamento perfil = request.perfilEfetivo();
        double origemLat = request.origem().latitude().doubleValue();
        double origemLon = request.origem().longitude().doubleValue();
        double destinoLat = request.destino().latitude().doubleValue();
        double destinoLon = request.destino().longitude().doubleValue();

        double retaMetros = Geografia.distanciaMetros(origemLat, origemLon, destinoLat, destinoLon);
        if (retaMetros < DISTANCIA_MINIMA_METROS) {
            throw ApiException.badRequest("Origem e destino devem ser pontos diferentes.");
        }
        if (retaMetros > distanciaMaximaMetros) {
            throw ApiException.badRequest("A distância entre origem e destino excede o máximo de "
                    + Math.round(distanciaMaximaMetros / 1000.0) + " km.");
        }

        RoteamentoClient.RotaCalculada rota = roteamento.calcular(perfil, origemLat, origemLon, destinoLat,
                destinoLon);

        RotaResponse.Exposicao exposicao = request.calcularExposicao()
                ? exposicaoService.avaliar(rota.coordenadas(), request.ponderarPorDistancia())
                : exposicaoService.naoSolicitada(request.ponderarPorDistancia());

        Integer idGravado = null;
        OffsetDateTime calculadaEm = OffsetDateTime.now(ZoneOffset.UTC);
        if (principal != null) {
            Rota gravada = rotaRepository.save(montarRota(principal, request, rota, exposicao));
            idGravado = gravada.getId();
            calculadaEm = gravada.getDataCalculo();
        }

        return new RotaResponse(
                idGravado,
                perfil,
                new RotaResponse.Ponto(origemLat, origemLon, rotulo(request.origem())),
                new RotaResponse.Ponto(destinoLat, destinoLon, rotulo(request.destino())),
                arredondar(rota.distanciaMetros()),
                arredondar(rota.duracaoSegundos()),
                new RotaResponse.Geometria("LineString", paraListas(rota.coordenadas())),
                exposicao,
                calculadaEm);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RotaResumoResponse> listar(UsuarioPrincipal principal, int pagina, int limite) {
        if (pagina < 1) {
            throw ApiException.badRequest("O parâmetro page deve ser maior ou igual a 1.");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO_PAGINA) {
            throw ApiException.badRequest("O parâmetro limit deve estar entre 1 e " + LIMITE_MAXIMO_PAGINA + ".");
        }

        var resultado = rotaRepository.findByUsuarioId(principal.id(),
                PageRequest.of(pagina - 1, limite, Sort.by(Sort.Direction.DESC, "dataCalculo", "id")));

        return PaginaResponse.de(
                resultado.getContent().stream().map(RotaResumoResponse::de).toList(),
                pagina, limite, resultado.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RotaResumoResponse buscar(UsuarioPrincipal principal, Integer id) {
        return RotaResumoResponse.de(carregarDoUsuario(principal, id));
    }

    /** Remove definitivamente uma consulta do histórico do próprio usuário (dado de localização). */
    @Transactional
    public void excluir(UsuarioPrincipal principal, Integer id) {
        rotaRepository.delete(carregarDoUsuario(principal, id));
    }

    // ---------------------------------------------------------------------------------------------

    private Rota carregarDoUsuario(UsuarioPrincipal principal, Integer id) {
        // Rota de outra pessoa responde 404 (não revela que existe).
        return rotaRepository.findByIdAndUsuarioId(id, principal.id())
                .orElseThrow(() -> ApiException.notFound("Rota não encontrada."));
    }

    private Rota montarRota(UsuarioPrincipal principal, CalcularRotaRequest request,
            RoteamentoClient.RotaCalculada calculada, RotaResponse.Exposicao exposicao) {
        Rota rota = new Rota();
        rota.setUsuarioId(principal.id());
        rota.setOrigem(rotulo(request.origem()));
        rota.setDestino(rotulo(request.destino()));
        rota.setOrigemLatitude(escala(request.origem().latitude()));
        rota.setOrigemLongitude(escala(request.origem().longitude()));
        rota.setDestinoLatitude(escala(request.destino().latitude()));
        rota.setDestinoLongitude(escala(request.destino().longitude()));
        rota.setDistancia(BigDecimal.valueOf(calculada.distanciaMetros()).setScale(2, RoundingMode.HALF_UP));
        rota.setTempoEstimado((int) Math.min(Integer.MAX_VALUE, Math.round(calculada.duracaoSegundos())));
        rota.setIndiceExposicao(exposicao.indice() == null
                ? null
                : BigDecimal.valueOf(exposicao.indice()).setScale(2, RoundingMode.HALF_UP));
        return rota;
    }

    private String rotulo(PontoRequest ponto) {
        return ponto.rotulo() == null || ponto.rotulo().isBlank() ? null : ponto.rotulo().trim();
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(ESCALA_COORDENADA, RoundingMode.HALF_UP);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private List<List<Double>> paraListas(List<double[]> coordenadas) {
        List<List<Double>> lista = new ArrayList<>(coordenadas.size());
        for (double[] c : coordenadas) {
            lista.add(List.of(c[0], c[1]));
        }
        return lista;
    }
}
