package br.com.urbansafe.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AtualizarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.FiltroOcorrencia;
import br.com.urbansafe.backend.dto.OcorrenciaResponse;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.entity.AcaoHistorico;
import br.com.urbansafe.backend.entity.Ocorrencia;
import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.entity.TipoOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.OcorrenciaConsultaRepository;
import br.com.urbansafe.backend.repository.OcorrenciaRepository;
import br.com.urbansafe.backend.repository.TipoOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.util.Filtros;

@Service
public class OcorrenciaService {

    /** Tolerância para relógios de clientes levemente adiantados ao validar "data no futuro". */
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final int ESCALA_COORDENADA = 7;

    private final OcorrenciaRepository ocorrenciaRepository;
    private final TipoOcorrenciaRepository tipoRepository;
    private final OcorrenciaConsultaRepository consultaRepository;
    private final OcorrenciaAcesso acesso;
    private final HistoricoService historicoService;
    private final int limiteMaximo;
    private final double maxLadoBbox;

    public OcorrenciaService(
            OcorrenciaRepository ocorrenciaRepository,
            TipoOcorrenciaRepository tipoRepository,
            OcorrenciaConsultaRepository consultaRepository,
            OcorrenciaAcesso acesso,
            HistoricoService historicoService,
            @Value("${app.ocorrencias.limite-maximo:100}") int limiteMaximo,
            @Value("${app.mapa.max-lado-bbox-graus:5.0}") double maxLadoBbox) {

        this.ocorrenciaRepository = ocorrenciaRepository;
        this.tipoRepository = tipoRepository;
        this.consultaRepository = consultaRepository;
        this.acesso = acesso;
        this.historicoService = historicoService;
        this.limiteMaximo = limiteMaximo;
        this.maxLadoBbox = maxLadoBbox;
    }

    @Transactional
    public OcorrenciaResponse criar(UsuarioPrincipal principal, CriarOcorrenciaRequest request) {
        TipoOcorrencia tipo = tipoAtivo(request.tipoOcorrenciaId());
        validarDataNaoFutura(request.dataOcorrencia());

        Ocorrencia ocorrencia = new Ocorrencia();
        // O autor é SEMPRE a identidade autenticada; o corpo da requisição não tem campo de usuário.
        ocorrencia.setUsuarioId(principal.id());
        ocorrencia.setTipo(tipo);
        ocorrencia.setTitulo(request.titulo().trim());
        ocorrencia.setDescricao(request.descricao().trim());
        ocorrencia.setLatitude(escala(request.latitude()));
        ocorrencia.setLongitude(escala(request.longitude()));
        ocorrencia.setEndereco(textoOuNulo(request.endereco()));
        ocorrencia.setDataOcorrencia(request.dataOcorrencia());
        ocorrencia.setAnonimato(Boolean.TRUE.equals(request.anonimato()));
        ocorrencia.setRelevancia(Relevancia.MEDIA);
        // Tipos que exigem revisão nascem PENDENTES (moderação); os demais são publicados direto.
        ocorrencia.setStatus(tipo.isExigeRevisao() ? StatusOcorrencia.PENDENTE : StatusOcorrencia.PUBLICADA);

        Ocorrencia salva = ocorrenciaRepository.saveAndFlush(ocorrencia);

        historicoService.registrar(salva.getId(), principal.id(), AcaoHistorico.CRIACAO,
                null, salva.getStatus(), "Ocorrência registrada.");

        return OcorrenciaResponse.de(salva, true);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<OcorrenciaResponse> listar(UsuarioPrincipal principal, String bbox, Integer tipoId,
            OffsetDateTime dataInicio, OffsetDateTime dataFim, StatusOcorrencia status, boolean apenasMinhas,
            int pagina, int limite) {

        if (pagina < 1) {
            throw ApiException.badRequest("O parâmetro page deve ser maior ou igual a 1.");
        }
        if (limite < 1 || limite > limiteMaximo) {
            throw ApiException.badRequest("O parâmetro limit deve estar entre 1 e " + limiteMaximo + ".");
        }

        FiltroOcorrencia filtro = Filtros.montar(bbox, maxLadoBbox, false, tipoId, dataInicio, dataFim,
                status, apenasMinhas);
        var escopo = new OcorrenciaConsultaRepository.Escopo(principal.id(), principal.isEquipe(), false);

        long total = consultaRepository.contar(filtro, escopo);
        List<OcorrenciaResponse> itens = total == 0
                ? List.of()
                : consultaRepository.listar(filtro, escopo, limite, (long) (pagina - 1) * limite);

        return PaginaResponse.de(itens, pagina, limite, total);
    }

    @Transactional(readOnly = true)
    public OcorrenciaResponse buscar(UsuarioPrincipal principal, Integer id) {
        Ocorrencia ocorrencia = acesso.carregarVisivel(principal, id);
        return OcorrenciaResponse.de(ocorrencia, mostrarAutor(principal, ocorrencia));
    }

    /**
     * Atualização parcial (campos ausentes não são alterados).
     * <ul>
     * <li>Autor: campos do relato e tipo, somente enquanto PENDENTE.</li>
     * <li>GESTOR/ADMINISTRADOR: apenas tipo e relevância (correção de classificação) em qualquer ocorrência
     * visível; o conteúdo do relato de outra pessoa nunca é reescrito.</li>
     * <li>O status só muda pelo fluxo de moderação.</li>
     * </ul>
     */
    @Transactional
    public OcorrenciaResponse atualizar(UsuarioPrincipal principal, Integer id, AtualizarOcorrenciaRequest r) {
        if (r.vazio()) {
            throw ApiException.badRequest("Informe ao menos um campo para atualizar.");
        }

        Ocorrencia o = acesso.carregarVisivel(principal, id);
        boolean autor = acesso.ehAutor(principal, o);
        boolean equipe = principal.isEquipe();
        boolean pendente = o.getStatus() == StatusOcorrencia.PENDENTE;

        if (r.relevancia() != null && !equipe) {
            throw ApiException.forbidden("Somente GESTOR ou ADMINISTRADOR podem alterar a relevância.");
        }
        if (r.temCamposDoRelato()) {
            if (!autor) {
                throw ApiException.forbidden("Você só pode editar as suas próprias ocorrências.");
            }
            if (!pendente) {
                throw ApiException.conflict("O autor só pode editar ocorrências com status PENDENTE.");
            }
        }
        if (r.tipoOcorrenciaId() != null && !equipe) {
            if (!autor) {
                throw ApiException.forbidden("Você só pode editar as suas próprias ocorrências.");
            }
            if (!pendente) {
                throw ApiException.conflict("O autor só pode editar ocorrências com status PENDENTE.");
            }
        }
        if (r.dataOcorrencia() != null) {
            validarDataNaoFutura(r.dataOcorrencia());
        }

        List<String> alterados = new ArrayList<>();

        if (r.tipoOcorrenciaId() != null && !r.tipoOcorrenciaId().equals(o.getTipo().getId())) {
            o.setTipo(tipoAtivo(r.tipoOcorrenciaId()));
            alterados.add("tipo");
        }
        if (r.titulo() != null && !r.titulo().trim().equals(o.getTitulo())) {
            o.setTitulo(r.titulo().trim());
            alterados.add("titulo");
        }
        if (r.descricao() != null && !r.descricao().trim().equals(o.getDescricao())) {
            o.setDescricao(r.descricao().trim());
            alterados.add("descricao");
        }
        if (r.latitude() != null && escala(r.latitude()).compareTo(o.getLatitude()) != 0) {
            o.setLatitude(escala(r.latitude()));
            alterados.add("latitude");
        }
        if (r.longitude() != null && escala(r.longitude()).compareTo(o.getLongitude()) != 0) {
            o.setLongitude(escala(r.longitude()));
            alterados.add("longitude");
        }
        if (r.endereco() != null && !Objects.equals(textoOuNulo(r.endereco()), o.getEndereco())) {
            o.setEndereco(textoOuNulo(r.endereco()));
            alterados.add("endereco");
        }
        if (r.dataOcorrencia() != null && !mesmoInstante(r.dataOcorrencia(), o.getDataOcorrencia())) {
            o.setDataOcorrencia(r.dataOcorrencia());
            alterados.add("dataOcorrencia");
        }
        if (r.anonimato() != null && r.anonimato() != o.isAnonimato()) {
            o.setAnonimato(r.anonimato());
            alterados.add("anonimato");
        }
        if (r.relevancia() != null && r.relevancia() != o.getRelevancia()) {
            o.setRelevancia(r.relevancia());
            alterados.add("relevancia");
        }

        if (alterados.isEmpty()) {
            return OcorrenciaResponse.de(o, mostrarAutor(principal, o));
        }

        Ocorrencia salva = ocorrenciaRepository.saveAndFlush(o);
        // O histórico registra QUAIS campos mudaram (sem copiar o conteúdo anterior).
        historicoService.registrar(salva.getId(), principal.id(), AcaoHistorico.ATUALIZACAO, null, null,
                "Campos alterados: " + String.join(", ", alterados) + ".");

        return OcorrenciaResponse.de(salva, mostrarAutor(principal, salva));
    }

    /**
     * Exclusão lógica. O autor exclui apenas enquanto PENDENTE ou REJEITADA; o ADMINISTRADOR pode excluir
     * qualquer ocorrência; GESTOR usa a moderação (ocultar/rejeitar). Histórico e mídias são preservados.
     */
    @Transactional
    public void excluir(UsuarioPrincipal principal, Integer id) {
        Ocorrencia o = acesso.carregarVisivel(principal, id);
        boolean autor = acesso.ehAutor(principal, o);
        boolean administrador = principal.isAdministrador();

        if (!autor && !administrador) {
            throw ApiException.forbidden("Você só pode excluir as suas próprias ocorrências.");
        }
        if (!administrador && o.getStatus() != StatusOcorrencia.PENDENTE
                && o.getStatus() != StatusOcorrencia.REJEITADA) {
            throw ApiException.conflict("O autor só pode excluir ocorrências PENDENTES ou REJEITADAS.");
        }

        StatusOcorrencia statusAtual = o.getStatus();
        o.excluirLogicamente(principal.id());
        ocorrenciaRepository.saveAndFlush(o);

        historicoService.registrar(o.getId(), principal.id(), AcaoHistorico.EXCLUSAO, statusAtual, null,
                "Ocorrência excluída logicamente.");
    }

    // ---------------------------------------------------------------------------------------------

    private boolean mostrarAutor(UsuarioPrincipal principal, Ocorrencia ocorrencia) {
        return principal.isEquipe() || acesso.ehAutor(principal, ocorrencia);
    }

    private TipoOcorrencia tipoAtivo(Integer id) {
        TipoOcorrencia tipo = tipoRepository.findById(id)
                .orElseThrow(() -> ApiException.badRequest("Tipo de ocorrência inexistente."));
        if (!tipo.isAtivo()) {
            throw ApiException.badRequest("Tipo de ocorrência inativo.");
        }
        return tipo;
    }

    private void validarDataNaoFutura(OffsetDateTime data) {
        if (data != null && data.isAfter(OffsetDateTime.now(ZoneOffset.UTC).plus(TOLERANCIA_FUTURO))) {
            throw ApiException.badRequest("A data da ocorrência não pode estar no futuro.");
        }
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(ESCALA_COORDENADA, RoundingMode.HALF_UP);
    }

    private boolean mesmoInstante(OffsetDateTime a, OffsetDateTime b) {
        return b != null && a.toInstant().equals(b.toInstant());
    }

    private String textoOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
