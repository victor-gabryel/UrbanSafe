package br.com.urbansafe.backend.controller;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import br.com.urbansafe.backend.dto.AtualizarStatusOcorrenciaRequest;
import br.com.urbansafe.backend.dto.ModeracaoResponse;
import br.com.urbansafe.backend.dto.ModerarRequest;
import br.com.urbansafe.backend.dto.OcorrenciaResponse;
import br.com.urbansafe.backend.dto.PaginaResponse;
import br.com.urbansafe.backend.entity.AcaoModeracao;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.ModeracaoService;
import br.com.urbansafe.backend.service.OcorrenciaService;

@RestController
@RequestMapping("/api/admin/ocorrencias")
@Tag(name = "Moderação (fila)", description = "Fila e gestão de ocorrências. Perfis: GESTOR e ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class AdminOcorrenciaController {
    private final OcorrenciaService ocorrenciaService;
    private final ModeracaoService moderacaoService;

    public AdminOcorrenciaController(OcorrenciaService ocorrenciaService, ModeracaoService moderacaoService) {
        this.ocorrenciaService = ocorrenciaService;
        this.moderacaoService = moderacaoService;
    }

    @Operation(summary = "Listar ocorrências pendentes de análise", description = "Perfis: GESTOR, ADMINISTRADOR. Mais recentes primeiro.")
    @ApiResponse(responseCode = "200", description = "Página de ocorrências PENDENTES")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @GetMapping("/pendentes")
    public ResponseEntity<PaginaResponse<OcorrenciaResponse>> pendentes(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(name = "tipo", required = false) Integer tipo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ocorrenciaService.listar(principal, null, tipo, null, null,
                StatusOcorrencia.PENDENTE, false, page, limit));
    }

    @Operation(summary = "Listar ocorrências para gestão", description = "Perfis: GESTOR, ADMINISTRADOR. Todas as ocorrências não excluídas, com filtros opcionais.")
    @ApiResponse(responseCode = "200", description = "Página de ocorrências")
    @ApiResponse(responseCode = "400", description = "Filtros inválidos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @GetMapping
    public ResponseEntity<PaginaResponse<OcorrenciaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(name = "status", required = false) StatusOcorrencia status,
            @RequestParam(name = "tipo", required = false) Integer tipo,
            @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ocorrenciaService.listar(principal, null, tipo, dataInicio, dataFim, status,
                false, page, limit));
    }

    @Operation(summary = "Alterar o status de uma ocorrência", description = "Perfis: GESTOR, ADMINISTRADOR. Sempre passa pela moderação (registro imutável + histórico): "
            + "PUBLICADA=APROVAR, REJEITADA=REJEITAR, OCULTA=OCULTAR_TEMPORARIAMENTE, PENDENTE=SOLICITAR_CORRECAO. "
            + "Mesmas regras de transição e justificativa de POST /api/ocorrencias/{id}/moderacao.")
    @ApiResponse(responseCode = "200", description = "Decisão registrada")
    @ApiResponse(responseCode = "400", description = "Justificativa ausente quando obrigatória")
    @ApiResponse(responseCode = "403", description = "Sem permissão ou ocorrência do próprio moderador")
    @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada")
    @ApiResponse(responseCode = "409", description = "Transição não permitida")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ModeracaoResponse> alterarStatus(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusOcorrenciaRequest request) {
        AcaoModeracao acao = switch (request.status()) {
            case PUBLICADA -> AcaoModeracao.APROVAR;
            case REJEITADA -> AcaoModeracao.REJEITAR;
            case OCULTA -> AcaoModeracao.OCULTAR_TEMPORARIAMENTE;
            case PENDENTE -> AcaoModeracao.SOLICITAR_CORRECAO;
        };
        return ResponseEntity.ok(moderacaoService.moderar(principal, id, new ModerarRequest(acao,
                request.justificativa())));
    }
}
