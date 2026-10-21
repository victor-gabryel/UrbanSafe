package br.com.urbansafe.backend.controller;

import br.com.urbansafe.backend.dto.CadastroRequest;
import br.com.urbansafe.backend.dto.CadastroResponse;
import br.com.urbansafe.backend.dto.LoginRequest;
import br.com.urbansafe.backend.dto.LoginResponse;
import br.com.urbansafe.backend.dto.MensagemResponse;
import br.com.urbansafe.backend.dto.RecuperarSenhaRequest;
import br.com.urbansafe.backend.dto.RedefinirSenhaRequest;
import br.com.urbansafe.backend.dto.ReenviarVerificacaoRequest;
import br.com.urbansafe.backend.dto.RefreshTokenRequest;
import br.com.urbansafe.backend.dto.UsuarioResponse;
import br.com.urbansafe.backend.dto.VerificarEmailRequest;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.AuthService;
import br.com.urbansafe.backend.service.RecuperacaoSenhaService;
import br.com.urbansafe.backend.service.TokenVerificacaoEmailService;
import br.com.urbansafe.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenVerificacaoEmailService tokenVerificacaoEmailService;
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(
            UsuarioService usuarioService,
            TokenVerificacaoEmailService tokenVerificacaoEmailService,
            AuthService authService,
            RecuperacaoSenhaService recuperacaoSenhaService) {
        this.usuarioService = usuarioService;
        this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request) {

        CadastroResponse resposta = usuarioService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<String> verificarEmail(
            @Valid @RequestBody VerificarEmailRequest request) {

        tokenVerificacaoEmailService.verificarEmail(request.token());

        return ResponseEntity.ok("E-mail verificado com sucesso.");
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<MensagemResponse> reenviarVerificacao(
            @Valid @RequestBody ReenviarVerificacaoRequest request) {

        authService.reenviarVerificacao(request.email());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemResponse(
                "Se o e-mail estiver cadastrado e pendente de verificação, enviaremos um novo link."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        return ResponseEntity.ok(usuarioService.buscarPerfil(principal.id()));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<MensagemResponse> recuperarSenha(
            @Valid @RequestBody RecuperarSenhaRequest request) {

        recuperacaoSenhaService.solicitar(request.email());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemResponse(
                "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha."));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest request) {

        recuperacaoSenhaService.redefinir(request.token(), request.novaSenha());

        return ResponseEntity.ok(new MensagemResponse(
                "Senha redefinida com sucesso. Faça login com a nova senha."));
    }
}
