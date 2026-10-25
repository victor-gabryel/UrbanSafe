package br.com.urbansafe.backend.controller;

import br.com.urbansafe.backend.dto.AtualizarPerfilRequest;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.UsuarioService;

import br.com.urbansafe.backend.config.OpenApiConfig;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Perfil e conta do usuário autenticado")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Consultar o próprio perfil")
    @ApiResponse(responseCode = "200", description = "Perfil")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> consultarMeuPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        return ResponseEntity.ok(usuarioService.buscarPerfil(principal.id()));
    }

    @Operation(summary = "Atualizar o próprio perfil", description = "Apenas o nome é editável.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PatchMapping("/me")
    public ResponseEntity<UsuarioResponse> atualizarMeuPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizarPerfilRequest request) {

        return ResponseEntity.ok(usuarioService.atualizarPerfil(principal.id(), request));
    }

    @Operation(summary = "Desativar a própria conta", description = "Desativação lógica; sessões são revogadas.")
    @ApiResponse(responseCode = "204", description = "Conta desativada")
    @DeleteMapping("/me")
    public ResponseEntity<Void> desativarMinhaConta(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        usuarioService.desativarConta(principal.id());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar usuário por id", description = "Somente o próprio usuário ou ADMINISTRADOR.")
    @ApiResponse(responseCode = "200", description = "Usuário")
    @ApiResponse(responseCode = "403", description = "Sem permissão")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> consultarPorId(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Integer id) {

        return ResponseEntity.ok(usuarioService.buscarPorId(principal, id));
    }
}
