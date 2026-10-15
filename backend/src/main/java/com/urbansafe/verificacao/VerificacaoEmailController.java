package com.urbansafe.verificacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class VerificacaoEmailController {

    private final VerificacaoEmailService verificacaoEmailService;

    public VerificacaoEmailController(
            VerificacaoEmailService verificacaoEmailService) {
        this.verificacaoEmailService = verificacaoEmailService;
    }

    @PostMapping("/verificar-email")
    @ResponseStatus(HttpStatus.OK)
    public MensagemResponse verificarEmail(
            @Valid @RequestBody VerificarEmailRequest request) {

        verificacaoEmailService.confirmarCodigo(
                request.email(),
                request.codigo()
        );

        return new MensagemResponse(
                "E-mail verificado com sucesso. Sua conta está ativa."
        );
    }

    public record VerificarEmailRequest(
            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "Informe um e-mail válido.")
            String email,

            @NotBlank(message = "O código é obrigatório.")
            @Pattern(
                    regexp = "\\d{6}",
                    message = "O código deve conter 6 dígitos."
            )
            String codigo
    ) {}

    public record MensagemResponse(String mensagem) {}
}