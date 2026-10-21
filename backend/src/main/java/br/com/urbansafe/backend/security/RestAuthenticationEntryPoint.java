package br.com.urbansafe.backend.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401 em JSON para requisições sem autenticação válida (ausente, inválida ou expirada). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        Object motivo = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_MOTIVO);

        String mensagem;
        if (TokenJwtException.Motivo.EXPIRADO.equals(motivo)) {
            mensagem = "Token de acesso expirado.";
        } else if (TokenJwtException.Motivo.INVALIDO.equals(motivo)) {
            mensagem = "Token de acesso inválido.";
        } else {
            mensagem = "Autenticação necessária. Informe um token de acesso válido.";
        }

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ErroJsonWriter.escrever(response, HttpStatus.UNAUTHORIZED, mensagem, request.getRequestURI());
    }
}
