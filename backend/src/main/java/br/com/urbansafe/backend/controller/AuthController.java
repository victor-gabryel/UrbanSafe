package br.com.urbansafe.backend.controller;

import br.com.urbansafe.backend.dto.VerificarEmailRequest;
import br.com.urbansafe.backend.service.TokenVerificacaoEmailService;

import br.com.urbansafe.backend.dto.CadastroRequest;
import br.com.urbansafe.backend.dto.CadastroResponse;
import br.com.urbansafe.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenVerificacaoEmailService tokenVerificacaoEmailService;

    private final UsuarioService usuarioService;

    public AuthController(
            UsuarioService usuarioService,
            TokenVerificacaoEmailService tokenVerificacaoEmailService) {
        this.usuarioService = usuarioService;
        this.tokenVerificacaoEmailService = tokenVerificacaoEmailService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request) {

        System.out.println(">>> ENTROU NO ENDPOINT DE CADASTRO");

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
}