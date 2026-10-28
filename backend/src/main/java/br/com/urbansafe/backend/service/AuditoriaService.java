package br.com.urbansafe.backend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AuditoriaResponse;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.entity.AuditoriaAdministrativa;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.AuditoriaAdministrativaRepository;

/** Registro (somente acréscimo) e consulta das operações administrativas relevantes. */
@Service
public class AuditoriaService {

    static final int LIMITE_MAXIMO = 100;

    private final AuditoriaAdministrativaRepository repository;

    public AuditoriaService(AuditoriaAdministrativaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Integer usuarioId, String acao, String entidade, Integer entidadeId, String detalhes) {
        repository.save(new AuditoriaAdministrativa(usuarioId, acao, entidade, entidadeId, detalhes));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> listar(int pagina, int limite) {
        if (pagina < 1) {
            throw ApiException.badRequest("O parâmetro page deve ser maior ou igual a 1.");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw ApiException.badRequest("O parâmetro limit deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }

        var resultado = repository.findAll(
                PageRequest.of(pagina - 1, limite, Sort.by(Sort.Direction.DESC, "dataEvento", "id")));

        return PaginaResponse.de(
                resultado.getContent().stream().map(AuditoriaResponse::de).toList(),
                pagina, limite, resultado.getTotalElements());
    }
}
