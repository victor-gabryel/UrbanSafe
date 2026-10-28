package br.com.urbansafe.backend.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.urbansafe.backend.dto.AlterarPerfilRequest;
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

    public AdministracaoService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
            AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.auditoria = auditoria;
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
