package br.com.urbansafe.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.AlterarPerfilRequest;
import br.com.urbansafe.backend.dto.AuditoriaResponse;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.AdministracaoService;
import br.com.urbansafe.backend.service.AuditoriaService;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração", description = "Somente ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class AdminController {

    private final AdministracaoService administracaoService;
    private final AuditoriaService auditoriaService;

    public AdminController(AdministracaoService administracaoService, AuditoriaService auditoriaService) {
        this.administracaoService = administracaoService;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Alterar o perfil de um usuário", description = "Perfil: ADMINISTRADOR. Perfis possíveis: USUARIO (usuário comum), GESTOR, ADMINISTRADOR. "
            + "Não é possível alterar o próprio perfil. A operação é registrada na auditoria.")
    @ApiResponse(responseCode = "200", description = "Perfil alterado")
    @ApiResponse(responseCode = "400", description = "Perfil inválido")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMINISTRADOR) ou tentativa de alterar o próprio perfil")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @PatchMapping("/usuarios/{id}/perfil")
    public ResponseEntity<UsuarioResponse> alterarPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody AlterarPerfilRequest request) {
        return ResponseEntity.ok(administracaoService.alterarPerfil(principal, id, request));
    }

    @Operation(summary = "Consultar a auditoria administrativa", description = "Perfil: ADMINISTRADOR. Registros imutáveis (mudança de perfil, gestão de tipos), mais recentes primeiro.")
    @ApiResponse(responseCode = "200", description = "Página de registros")
    @ApiResponse(responseCode = "400", description = "Paginação inválida")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMINISTRADOR)")
    @GetMapping("/auditoria")
    public ResponseEntity<PaginaResponse<AuditoriaResponse>> auditoria(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        // Defesa em profundidade: a SecurityConfig já restringe /api/admin/** ao
        // ADMINISTRADOR.
        if (principal == null || !principal.isAdministrador()) {
            throw ApiException.forbidden("Somente ADMINISTRADOR pode consultar a auditoria.");
        }
        return ResponseEntity.ok(auditoriaService.listar(page, limit));
    }
}
