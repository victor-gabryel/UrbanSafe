package br.com.urbansafe.backend.controller;

import br.com.urbansafe.backend.dto.AtualizarPerfilRequest;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.UsuarioService;

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
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> consultarMeuPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        return ResponseEntity.ok(usuarioService.buscarPerfil(principal.id()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UsuarioResponse> atualizarMeuPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizarPerfilRequest request) {

        return ResponseEntity.ok(usuarioService.atualizarPerfil(principal.id(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> desativarMinhaConta(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        usuarioService.desativarConta(principal.id());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> consultarPorId(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Integer id) {

        return ResponseEntity.ok(usuarioService.buscarPorId(principal, id));
    }
}
