package br.com.urbansafe.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.ModeracaoResponse;
import br.com.urbansafe.backend.dto.ModerarRequest;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.ModeracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ocorrencias/{id}/moderacao")
@Tag(name = "Moderação", description = "Somente GESTOR e ADMINISTRADOR. Único caminho para alterar o status de uma ocorrência.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class ModeracaoController {

    private final ModeracaoService moderacaoService;

    public ModeracaoController(ModeracaoService moderacaoService) {
        this.moderacaoService = moderacaoService;
    }

    @Operation(summary = "Registrar ação de moderação",
            description = "Perfis: GESTOR, ADMINISTRADOR. Transições: APROVAR (PENDENTE/OCULTA/REJEITADA → PUBLICADA); "
                    + "REJEITAR (PENDENTE/PUBLICADA/OCULTA → REJEITADA); SOLICITAR_CORRECAO (PENDENTE/PUBLICADA → PENDENTE); "
                    + "OCULTAR_TEMPORARIAMENTE (PENDENTE/PUBLICADA → OCULTA). Justificativa obrigatória, exceto em APROVAR. "
                    + "Não é permitido moderar a própria ocorrência.")
    @ApiResponse(responseCode = "201", description = "Decisão registrada")
    @ApiResponse(responseCode = "400", description = "Ação ausente ou justificativa obrigatória ausente")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige GESTOR/ADMINISTRADOR) ou ocorrência própria")
    @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada")
    @ApiResponse(responseCode = "409", description = "Transição de status não permitida")
    @PostMapping
    public ResponseEntity<ModeracaoResponse> moderar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody ModerarRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(moderacaoService.moderar(principal, id, request));
    }

    @Operation(summary = "Consultar histórico de moderação",
            description = "Perfis: GESTOR, ADMINISTRADOR. Registros imutáveis, em ordem cronológica.")
    @ApiResponse(responseCode = "200", description = "Decisões de moderação")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige GESTOR/ADMINISTRADOR)")
    @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada")
    @GetMapping
    public ResponseEntity<List<ModeracaoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok(moderacaoService.listar(principal, id));
    }
}
