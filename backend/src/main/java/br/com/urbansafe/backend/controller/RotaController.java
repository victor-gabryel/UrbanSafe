package br.com.urbansafe.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.CalcularRotaRequest;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.dto.RotaResponse;
import br.com.urbansafe.backend.dto.RotaResumoResponse;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.RotaService;

@RestController
@RequestMapping("/api/rotas")
@Tag(name = "Rotas", description = "Cálculo de percurso a pé ou de bicicleta com indicador informativo de exposição "
        + "a ocorrências registradas (MVP). O cálculo é público; o histórico exige JWT.")
public class RotaController {
    private final RotaService rotaService;

    public RotaController(RotaService rotaService) {
        this.rotaService = rotaService;
    }

    @Operation(summary = "Calcular rota", description = "Acesso: público (visitante) ou autenticado. Consulta o serviço de roteamento e cruza o "
            + "percurso com as ocorrências PUBLICADAS próximas (PostGIS). Com JWT a consulta é gravada no "
            + "histórico; sem JWT nada é gravado. Limite de requisições por IP (429). **O indicador de exposição "
            + "é informativo: não é probabilidade de crime nem garantia de segurança.**")
    @ApiResponse(responseCode = "200", description = "Rota calculada")
    @ApiResponse(responseCode = "400", description = "Coordenadas inválidas, pontos iguais ou distância acima do máximo")
    @ApiResponse(responseCode = "422", description = "Não existe caminho entre os pontos para o modo escolhido")
    @ApiResponse(responseCode = "429", description = "Muitas consultas em pouco tempo")
    @ApiResponse(responseCode = "503", description = "Serviço de roteamento indisponível")
    @PostMapping("/calcular")
    public ResponseEntity<RotaResponse> calcular(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CalcularRotaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(rotaService.calcular(principal, request, http.getRemoteAddr()));
    }

    @Operation(summary = "Listar o próprio histórico de rotas", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. Mais recentes primeiro.")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
    @ApiResponse(responseCode = "200", description = "Página de consultas")
    @ApiResponse(responseCode = "400", description = "Paginação inválida")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @GetMapping
    public ResponseEntity<PaginaResponse<RotaResumoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Página (começa em 1)", example = "1") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Itens por página (1 a 100)", example = "20") @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(rotaService.listar(principal, page, limit));
    }

    @Operation(summary = "Consultar uma rota do próprio histórico", description = "Rota de outra pessoa responde 404.")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
    @ApiResponse(responseCode = "200", description = "Consulta")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<RotaResumoResponse> buscar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok(rotaService.buscar(principal, id));
    }

    @Operation(summary = "Excluir uma rota do próprio histórico", description = "Remove definitivamente o registro (dado de localização).")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        rotaService.excluir(principal, id);
        return ResponseEntity.noContent().build();
    }
}
