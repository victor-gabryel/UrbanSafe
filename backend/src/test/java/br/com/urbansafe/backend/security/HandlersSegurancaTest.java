package br.com.urbansafe.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class HandlersSegurancaTest {

    @Test
    void semAutenticacaoResponde401EmJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usuarios/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint().commence(request, response,
                new InsufficientAuthenticationException("x"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentAsString().contains("\"status\":401"));
        assertTrue(response.getContentAsString().contains("Autenticação necessária"));
    }

    @Test
    void tokenExpiradoUsaMensagemEspecifica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usuarios/me");
        request.setAttribute(JwtAuthenticationFilter.ATRIBUTO_MOTIVO, TokenJwtException.Motivo.EXPIRADO);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint().commence(request, response,
                new InsufficientAuthenticationException("x"));

        assertTrue(response.getContentAsString().contains("Token de acesso expirado."));
    }

    @Test
    void semPermissaoResponde403EmJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usuarios/9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler().handle(request, response, new AccessDeniedException("x"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":403"));
    }
}
