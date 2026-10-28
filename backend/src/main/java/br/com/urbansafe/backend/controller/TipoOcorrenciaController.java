package br.com.urbansafe.backend.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.AtualizarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.CriarTipoOcorrenciaRequest;
import br.com.urbansafe.backend.dto.TipoOcorrenciaResponse;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.TipoOcorrenciaService;

@RestController
@RequestMapping("/api/tipos-ocorrencia")
@Tag(name = "Tipos de ocorrência", description = "Catálogo de tipos. Leitura: qualquer usuário autenticado. "
        + "Criação, edição e desativação: somente ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class TipoOcorrenciaController {

    private final TipoOcorrenciaService service;

    public TipoOcorrenciaController(TipoOcorrenciaService service) {
        this.service = service;
    }

    @Operation(summary = "Listar tipos ativos", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. Retorna somente tipos ativos, ordenados por nome.")
    @ApiResponse(responseCode = "200", description = "Lista de tipos ativos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @GetMapping
    public ResponseEntity<List<TipoOcorrenciaResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @Operation(summary = "Consultar tipo por id", description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. Tipos inativos só são visíveis ao ADMINISTRADOR (404 para os demais).")
    @ApiResponse(responseCode = "200", description = "Tipo")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Tipo não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<TipoOcorrenciaResponse> buscar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        return ResponseEntity.ok(service.buscar(principal, id));
    }

    @Operation(summary = "Criar tipo de ocorrência", description = "Perfil: ADMINISTRADOR.")
    @ApiResponse(responseCode = "201", description = "Tipo criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMINISTRADOR)")
    @ApiResponse(responseCode = "409", description = "Já existe tipo com este nome")
    @PostMapping
    public ResponseEntity<TipoOcorrenciaResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CriarTipoOcorrenciaRequest request) {
        TipoOcorrenciaResponse criado = service.criar(principal, request);
        return ResponseEntity.created(URI.create("/api/tipos-ocorrencia/" + criado.id())).body(criado);
    }

    @Operation(summary = "Atualizar tipo parcialmente", description = "Perfil: ADMINISTRADOR. Campos ausentes não são alterados. 'ativo=true' reativa o tipo.")
    @ApiResponse(responseCode = "200", description = "Tipo atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMINISTRADOR)")
    @ApiResponse(responseCode = "404", description = "Tipo não encontrado")
    @ApiResponse(responseCode = "409", description = "Já existe tipo com este nome")
    @PatchMapping("/{id}")
    public ResponseEntity<TipoOcorrenciaResponse> atualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id,
            @Valid @RequestBody AtualizarTipoOcorrenciaRequest request) {
        return ResponseEntity.ok(service.atualizar(principal, id, request));
    }

    @Operation(summary = "Desativar tipo (exclusão lógica)", description = "Perfil: ADMINISTRADOR. Não apaga o tipo nem afeta ocorrências já cadastradas.")
    @ApiResponse(responseCode = "204", description = "Tipo desativado")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige ADMINISTRADOR)")
    @ApiResponse(responseCode = "404", description = "Tipo não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Integer id) {
        service.desativar(principal, id);
        return ResponseEntity.noContent().build();
    }
}
