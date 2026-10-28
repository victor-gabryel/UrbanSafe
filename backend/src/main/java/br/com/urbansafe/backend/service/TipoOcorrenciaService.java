package br.com.urbansafe.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AtualizarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.TipoOcorrenciaResponse;
import br.com.urbansafe.backend.entity.TipoOcorrencia;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TipoOcorrenciaRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

@Service
public class TipoOcorrenciaService {

    private static final String ENTIDADE = "tipos_ocorrencia";

    private final TipoOcorrenciaRepository repository;
    private final AuditoriaService auditoria;

    public TipoOcorrenciaService(TipoOcorrenciaRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<TipoOcorrenciaResponse> listarAtivos() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream().map(TipoOcorrenciaResponse::de).toList();
    }

    /** Tipo inativo só é visível ao ADMINISTRADOR (para os demais, 404), mas continua referenciável por ocorrências antigas. */
    @Transactional(readOnly = true)
    public TipoOcorrenciaResponse buscar(UsuarioPrincipal principal, Integer id) {
        TipoOcorrencia tipo = carregar(id);
        if (!tipo.isAtivo() && !principal.isAdministrador()) {
            throw ApiException.notFound("Tipo de ocorrência não encontrado.");
        }
        return TipoOcorrenciaResponse.de(tipo);
    }

    @Transactional
    public TipoOcorrenciaResponse criar(UsuarioPrincipal principal, CriarTipoOcorrenciaRequest request) {
        exigirAdministrador(principal);

        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw ApiException.conflict("Já existe um tipo de ocorrência com este nome.");
        }

        TipoOcorrencia tipo = new TipoOcorrencia();
        tipo.setNome(nome);
        tipo.setDescricao(textoOuNulo(request.descricao()));
        tipo.setCategoria(textoOuNulo(request.categoria()));
        tipo.setAtivo(true);
        tipo.setExigeRevisao(request.exigeRevisao() == null || request.exigeRevisao());

        TipoOcorrencia salvo = repository.save(tipo);
        auditoria.registrar(principal.id(), "CRIAR_TIPO_OCORRENCIA", ENTIDADE, salvo.getId(), "nome=" + nome);
        return TipoOcorrenciaResponse.de(salvo);
    }

    /** Atualização parcial: somente os campos enviados são alterados. */
    @Transactional
    public TipoOcorrenciaResponse atualizar(UsuarioPrincipal principal, Integer id,
            AtualizarTipoOcorrenciaRequest request) {

        exigirAdministrador(principal);
        if (request.vazio()) {
            throw ApiException.badRequest("Informe ao menos um campo para atualizar.");
        }

        TipoOcorrencia tipo = carregar(id);
        List<String> alterados = new ArrayList<>();

        if (request.nome() != null) {
            String nome = request.nome().trim();
            if (!nome.equals(tipo.getNome())) {
                if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
                    throw ApiException.conflict("Já existe um tipo de ocorrência com este nome.");
                }
                tipo.setNome(nome);
                alterados.add("nome");
            }
        }
        if (request.descricao() != null) {
            tipo.setDescricao(textoOuNulo(request.descricao()));
            alterados.add("descricao");
        }
        if (request.categoria() != null) {
            tipo.setCategoria(textoOuNulo(request.categoria()));
            alterados.add("categoria");
        }
        if (request.ativo() != null && request.ativo() != tipo.isAtivo()) {
            tipo.setAtivo(request.ativo());
            alterados.add("ativo=" + request.ativo());
        }
        if (request.exigeRevisao() != null && request.exigeRevisao() != tipo.isExigeRevisao()) {
            tipo.setExigeRevisao(request.exigeRevisao());
            alterados.add("exigeRevisao=" + request.exigeRevisao());
        }

        TipoOcorrencia salvo = repository.save(tipo);
        if (!alterados.isEmpty()) {
            auditoria.registrar(principal.id(), "ATUALIZAR_TIPO_OCORRENCIA", ENTIDADE, id,
                    "campos: " + String.join(", ", alterados));
        }
        return TipoOcorrenciaResponse.de(salvo);
    }

    /** Desativação lógica (idempotente): o registro e as ocorrências associadas são preservados. */
    @Transactional
    public void desativar(UsuarioPrincipal principal, Integer id) {
        exigirAdministrador(principal);

        TipoOcorrencia tipo = carregar(id);
        if (tipo.isAtivo()) {
            tipo.setAtivo(false);
            repository.save(tipo);
            auditoria.registrar(principal.id(), "DESATIVAR_TIPO_OCORRENCIA", ENTIDADE, id, "nome=" + tipo.getNome());
        }
    }

    private TipoOcorrencia carregar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Tipo de ocorrência não encontrado."));
    }

    // Defesa em profundidade: a SecurityConfig já restringe a rota, mas o serviço não confia só nela.
    private void exigirAdministrador(UsuarioPrincipal principal) {
        if (principal == null || !principal.isAdministrador()) {
            throw ApiException.forbidden("Somente ADMINISTRADOR pode gerenciar tipos de ocorrência.");
        }
    }

    private String textoOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
