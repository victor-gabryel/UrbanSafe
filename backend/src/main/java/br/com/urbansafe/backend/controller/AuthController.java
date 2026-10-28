package br.com.urbansafe.backend.controller;

import br.com.urbansafe.backend.config.OpenApiConfig;
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
import br.com.urbansafe.backend.exception.ErroResponse;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.AuthService;
import br.com.urbansafe.backend.service.LimitadorReenvioVerificacao;
import br.com.urbansafe.backend.service.RecuperacaoSenhaService;
import br.com.urbansafe.backend.service.TokenVerificacaoEmailService;
import br.com.urbansafe.backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, verificação de e-mail, login, sessão e recuperação de senha")
public class AuthController {

    private final TokenVerificacaoEmailService tokenVerificacaoEmailService;
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final LimitadorReenvioVerificacao limitadorReenvio;

    public AuthController(
            UsuarioService usuarioService,
            TokenVerificacaoEmailService tokenVerificacaoEmailService,
            AuthService authService,
            RecuperacaoSenhaService recuperacaoSenhaService,
            LimitadorReenvioVerificacao limitadorReenvio) {
        this.usuarioService = usuarioService;
        this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.limitadorReenvio = limitadorReenvio;
    }

    @Operation(summary = "Cadastrar usuário",
            description = "Cria a conta com perfil USUARIO e e-mail não verificado, e envia o link de verificação. "
                    + "Qualquer campo de perfil enviado é ignorado.")
    @ApiResponse(responseCode = "201", description = "Conta criada (e-mail ainda não verificado)")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request) {

        CadastroResponse resposta = usuarioService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @Operation(summary = "Verificar e-mail",
            description = "Valida o token recebido por e-mail (uso único, validade configurável, padrão 24 h) "
                    + "e ativa a conta. Resposta em texto puro.")
    @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = @Schema(example = "E-mail verificado com sucesso.")))
    @ApiResponse(responseCode = "400", description = "Token ausente, inválido, expirado ou já utilizado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/verificar-email")
    public ResponseEntity<String> verificarEmail(
            @Valid @RequestBody VerificarEmailRequest request) {

        tokenVerificacaoEmailService.verificarEmail(request.token());

        return ResponseEntity.ok("E-mail verificado com sucesso.");
    }

    @Operation(summary = "Reenviar e-mail de verificação",
            description = "Resposta 202 genérica, exista ou não a conta e esteja ela verificada ou não "
                    + "(evita enumeração de e-mails). Emitir novo link invalida o anterior. "
                    + "Há intervalo mínimo por conta e limite por IP.")
    @ApiResponse(responseCode = "202", description = "Solicitação aceita")
    @ApiResponse(responseCode = "400", description = "E-mail em formato inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "429", description = "Limite de solicitações por IP excedido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<MensagemResponse> reenviarVerificacao(
            @Valid @RequestBody ReenviarVerificacaoRequest request,
            HttpServletRequest http) {

        limitadorReenvio.registrar("reenvio-verificacao:" + http.getRemoteAddr());

        authService.reenviarVerificacao(request.email());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemResponse(
                "Se o e-mail estiver cadastrado e pendente de verificação, enviaremos um novo link."));
    }

    @Operation(summary = "Login", description = "Emite access token (JWT) e refresh token. "
            + "Contas com e-mail não verificado ou desativadas recebem 403 (após senha correta).")
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "403", description = "E-mail não verificado ou conta desativada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Renovar sessão", description = "Troca o refresh token por um novo par de tokens (rotação).")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Logout", description = "Revoga o refresh token informado (idempotente).")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Usuário autenticado", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER))
    @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        return ResponseEntity.ok(usuarioService.buscarPerfil(principal.id()));
    }

    @Operation(summary = "Solicitar recuperação de senha",
            description = "Sempre responde 202, sem revelar se o e-mail existe.")
    @ApiResponse(responseCode = "202", description = "Solicitação aceita")
    @PostMapping("/recuperar-senha")
    public ResponseEntity<MensagemResponse> recuperarSenha(
            @Valid @RequestBody RecuperarSenhaRequest request) {

        recuperacaoSenhaService.solicitar(request.email());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemResponse(
                "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha."));
    }

    @Operation(summary = "Redefinir senha", description = "Usa o token de recuperação (uso único) e revoga as sessões.")
    @ApiResponse(responseCode = "200", description = "Senha redefinida")
    @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado, ou senha inválida",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest request) {

        recuperacaoSenhaService.redefinir(request.token(), request.novaSenha());

        return ResponseEntity.ok(new MensagemResponse(
                "Senha redefinida com sucesso. Faça login com a nova senha."));
    }
}
