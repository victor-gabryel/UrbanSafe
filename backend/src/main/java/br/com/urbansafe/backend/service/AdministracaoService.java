package br.com.urbansafe.backend.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AlterarPerfilRequest;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.dto.UsuarioAdminResponse;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.entity.Perfil;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.UsuarioPrincipal;

/**
 * Operações administrativas sobre contas. Existem exatamente TRÊS perfis funcionais:
 * USUARIO (usuário comum), GESTOR e ADMINISTRADOR. "USUARIO_COMUM" é aceito como sinônimo de USUARIO
 * na entrada; o nome persistido continua sendo USUARIO (o JWT, os testes e o catálogo já o utilizam).
 */
@Service
public class AdministracaoService {

    static final Set<String> PERFIS_VALIDOS = Set.of("USUARIO", "GESTOR", "ADMINISTRADOR");

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final AuditoriaService auditoria;
    private final RefreshTokenService refreshTokenService;

    public AdministracaoService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
            AuditoriaService auditoria, RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.auditoria = auditoria;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioAdminResponse> listarUsuarios(UsuarioPrincipal administrador, int pagina,
            int limite) {
        exigirAdministrador(administrador);
        if (pagina < 1) {
            throw ApiException.badRequest("O parâmetro page deve ser maior ou igual a 1.");
        }
        if (limite < 1 || limite > 100) {
            throw ApiException.badRequest("O parâmetro limit deve estar entre 1 e 100.");
        }
        var resultado = usuarioRepository.findAllBy(
                PageRequest.of(pagina - 1, limite, Sort.by(Sort.Direction.ASC, "id")));
        return PaginaResponse.de(resultado.getContent().stream().map(UsuarioAdminResponse::de).toList(),
                pagina, limite, resultado.getTotalElements());
    }

    /**
     * Bloqueia (ativo=false) ou reativa uma conta. Ninguém altera o próprio status. Ao bloquear, todas as
     * sessões (refresh tokens) são revogadas; o JWT de acesso já emitido perde efeito na próxima requisição,
     * pois o filtro recarrega o usuário do banco.
     */
    @Transactional
    public UsuarioAdminResponse alterarStatus(UsuarioPrincipal administrador, Integer usuarioId, boolean ativo) {
        exigirAdministrador(administrador);
        if (administrador.id().equals(usuarioId)) {
            throw ApiException.forbidden("Você não pode alterar o status da sua própria conta.");
        }
        Usuario alvo = usuarioRepository.findComPerfilById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));

        if (alvo.isAtivo() != ativo) {
            alvo.setAtivo(ativo);
            usuarioRepository.save(alvo);
            if (!ativo) {
                refreshTokenService.revogarTodos(usuarioId);
            }
            auditoria.registrar(administrador.id(), ativo ? "REATIVAR_USUARIO" : "BLOQUEAR_USUARIO", "usuarios",
                    usuarioId, null);
        }
        return UsuarioAdminResponse.de(alvo);
    }

    private void exigirAdministrador(UsuarioPrincipal administrador) {
        if (administrador == null || !administrador.isAdministrador()) {
            throw ApiException.forbidden("Somente ADMINISTRADOR pode administrar contas.");
        }
    }

    /**
     * Altera o perfil de OUTRO usuário. Ninguém altera o próprio perfil (evita auto-promoção e deixar o
     * sistema sem administrador por engano). O perfil vem do corpo validado, nunca de dados não confiáveis do JWT.
     */
    @Transactional
    public UsuarioResponse alterarPerfil(UsuarioPrincipal administrador, Integer usuarioId,
            AlterarPerfilRequest request) {
        if (administrador == null || !administrador.isAdministrador()) {
            throw ApiException.forbidden("Somente ADMINISTRADOR pode alterar perfis de usuários.");
        }

        String nomePerfil = normalizar(request.perfil());
        if (!PERFIS_VALIDOS.contains(nomePerfil)) {
            throw ApiException.badRequest("Perfil inválido. Valores aceitos: USUARIO, GESTOR, ADMINISTRADOR.");
        }
        if (administrador.id().equals(usuarioId)) {
            throw ApiException.forbidden("Você não pode alterar o seu próprio perfil.");
        }

        Usuario alvo = usuarioRepository.findComPerfilById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));

        String anterior = alvo.getPerfil().getNome();
        if (anterior.equals(nomePerfil)) {
            return UsuarioResponse.de(alvo);
        }

        Perfil perfil = perfilRepository.findByNome(nomePerfil)
                .orElseThrow(() -> new IllegalStateException("Perfil " + nomePerfil + " não encontrado no banco."));

        alvo.setPerfil(perfil);
        Usuario salvo = usuarioRepository.save(alvo);

        auditoria.registrar(administrador.id(), "ALTERAR_PERFIL_USUARIO", "usuarios", usuarioId,
                anterior + " -> " + nomePerfil);

        return UsuarioResponse.de(salvo);
    }

    private String normalizar(String perfil) {
        String nome = perfil == null ? "" : perfil.trim().toUpperCase(Locale.ROOT);
        return nome.equals("USUARIO_COMUM") ? "USUARIO" : nome;
    }
}
