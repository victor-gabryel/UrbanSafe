package br.com.urbansafe.backend.controller;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.AtualizarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarOcorrenciaRequest;
import br.com.urbansafe.backend.dto.HistoricoResponse;
import br.com.urbansafe.backend.dto.OcorrenciaResponse;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.HistoricoService;
import br.com.urbansafe.backend.service.OcorrenciaService;

@RestController
@RequestMapping("/api/ocorrencias")
@Tag(name = "Ocorrências", description = "Registro, consulta, edição e exclusão lógica de ocorrências, com filtros "
        + "geoespaciais (PostGIS) e histórico. Todas as rotas exigem JWT.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class OcorrenciaController {
    private final OcorrenciaService ocorrenciaService;
    private final HistoricoService historicoService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService, HistoricoService historicoService) {
        this.ocorrenciaService = ocorrenciaService;
        this.historicoService = historicoService;
    }

    @Operation(summary = "Listar ocorrências (paginado, com filtros)", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. **Visibilidade:** USUARIO vê as PUBLICADAS e as próprias "
            + "(qualquer status); GESTOR e ADMINISTRADOR veem todas (exceto excluídas logicamente). O filtro por "
            + "área usa PostGIS (ST_Intersects/ST_MakeEnvelope + índice GiST). Ordenação: mais recentes primeiro.")
    @ApiResponse(responseCode = "200", description = "Página de ocorrências")
    @ApiResponse(responseCode = "400", description = "Filtro ou paginação inválidos (bbox, datas, page, limit, status...)")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @GetMapping
    public ResponseEntity<PaginaResponse<OcorrenciaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Página (começa em 1)", example = "1") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Itens por página (1 a 100)", example = "20") @RequestParam(name = "limit", defaultValue = "20") int limit,
            @Parameter(description = "Área retangular: minLon,minLat,maxLon,maxLat (graus; lado máximo configurável, padrão 5°)", example = "-34.95,-8.10,-34.85,-8.00") @RequestParam(name = "bbox", required = false) String bbox,
            @Parameter(description = "Id do tipo de ocorrência", example = "2") @RequestParam(name = "tipo", required = false) Integer tipo,
            @Parameter(description = "Início do período (ISO-8601, ex.: 2026-10-01T00:00:00Z); aplica-se à data da "
                    + "ocorrência (ou ao cadastro, se ausente)") @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @Parameter(description = "Fim do período (ISO-8601); deve ser maior ou igual a dataInicio") @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @Parameter(description = "Status: PENDENTE, PUBLICADA, REJEITADA ou OCULTA (usuário comum só enxerga os próprios fora de PUBLICADA)") @RequestParam(name = "status", required = false) StatusOcorrencia status,
            @Parameter(description = "true = somente as ocorrências registradas por mim") @RequestParam(name = "minhas", defaultValue = "false") boolean minhas) {
        return ResponseEntity.ok(ocorrenciaService.listar(principal, bbox, tipo, dataInicio, dataFim, status,
                minhas, page, limit));
    }

    @Operation(summary = "Registrar ocorrência", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. O autor é o usuário do JWT (qualquer 'usuarioId' enviado é "
            + "ignorado). Status inicial: PENDENTE se o tipo exige revisão; caso contrário PUBLICADA.")
    @ApiResponse(responseCode = "201", description = "Ocorrência criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (coordenadas fora dos limites, tipo inexistente/inativo...)")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @PostMapping
    public ResponseEntity<OcorrenciaResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CriarOcorrenciaRequest request) {
        OcorrenciaResponse criada = ocorrenciaService.criar(principal, request);
        return ResponseEntity.created(URI.create("/api/ocorrencias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Consultar ocorrência", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. Ocorrência não visível para o solicitante responde 404.")
    @ApiResponse(responseCode = "200", description = "Ocorrência")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Não encontrada (ou não visível)")
    @GetMapping("/{id}")
    public ResponseEntity<OcorrenciaResponse> buscar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok(ocorrenciaService.buscar(principal, id));
    }

    @Operation(summary = "Atualizar ocorrência parcialmente", description = "Perfis: autor (USUARIO/GESTOR/ADMINISTRADOR) enquanto PENDENTE; GESTOR/ADMINISTRADOR podem alterar "
            + "apenas 'tipoOcorrenciaId' e 'relevancia' de qualquer ocorrência visível. Campos ausentes não são "
            + "alterados. O status só muda pela moderação.")
    @ApiResponse(responseCode = "200", description = "Ocorrência atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (ocorrência de outra pessoa ou campo restrito)")
    @ApiResponse(responseCode = "404", description = "Não encontrada (ou não visível)")
    @ApiResponse(responseCode = "409", description = "Status atual não permite a edição pelo autor")
    @PatchMapping("/{id}")
    public ResponseEntity<OcorrenciaResponse> atualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody AtualizarOcorrenciaRequest request) {
        return ResponseEntity.ok(ocorrenciaService.atualizar(principal, id, request));
    }

    @Operation(summary = "Excluir ocorrência (exclusão lógica)", description = "Perfis: autor enquanto PENDENTE ou REJEITADA; ADMINISTRADOR em qualquer status. GESTOR usa a "
            + "moderação. O histórico e as mídias são preservados.")
    @ApiResponse(responseCode = "204", description = "Ocorrência excluída logicamente")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Não encontrada (ou não visível)")
    @ApiResponse(responseCode = "409", description = "Status atual não permite a exclusão pelo autor")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        ocorrenciaService.excluir(principal, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar histórico da ocorrência", description = "Perfis: autor da ocorrência, GESTOR e ADMINISTRADOR. Eventos em ordem cronológica; somente leitura "
            + "(não há rota para editar ou apagar eventos).")
    @ApiResponse(responseCode = "200", description = "Eventos do histórico")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Não encontrada (ou não visível)")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoResponse>> historico(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok(historicoService.consultar(principal, id));
    }
}
