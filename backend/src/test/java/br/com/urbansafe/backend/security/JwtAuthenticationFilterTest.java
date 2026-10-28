package br.com.urbansafe.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.support.Fixtures;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = new JwtService(Fixtures.SEGREDO, "urbansafe-teste", 15);
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService, repository);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requisicaoComToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usuarios/me");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void semHeaderNaoAutentica() throws Exception {
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValidoAutenticaComPerfilDoBanco() throws Exception {
        Usuario usuario = Fixtures.usuario(3, "a@b.com", "GESTOR");
        when(repository.findComPerfilById(3)).thenReturn(Optional.of(usuario));

        filtro.doFilter(requisicaoComToken(jwtService.gerarAccessToken(usuario)),
                new MockHttpServletResponse(), new MockFilterChain());

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacao);
        assertEquals(3, ((UsuarioPrincipal) autenticacao.getPrincipal()).id());
        assertEquals("ROLE_GESTOR", autenticacao.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void tokenInvalidoNaoAutenticaEMarcaMotivo() throws Exception {
        MockHttpServletRequest request = requisicaoComToken("lixo");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(TokenJwtException.Motivo.INVALIDO,
                request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_MOTIVO));
    }

    @Test
    void tokenExpiradoNaoAutenticaEMarcaMotivo() throws Exception {
        Usuario usuario = Fixtures.usuario(3, "a@b.com", "USUARIO");
        String expirado = jwtService.gerarAccessToken(usuario, Instant.now().minusSeconds(7200));
        MockHttpServletRequest request = requisicaoComToken(expirado);

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(TokenJwtException.Motivo.EXPIRADO,
                request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_MOTIVO));
    }

    @Test
    void contaDesativadaComTokenValidoNaoAutentica() throws Exception {
        Usuario usuario = Fixtures.usuario(3, "a@b.com", "USUARIO");
        usuario.setAtivo(false);
        when(repository.findComPerfilById(3)).thenReturn(Optional.of(usuario));

        filtro.doFilter(requisicaoComToken(jwtService.gerarAccessToken(usuario)),
                new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void emailNaoVerificadoComTokenValidoNaoAutentica() throws Exception {
        Usuario usuario = Fixtures.usuario(3, "a@b.com", "USUARIO");
        usuario.setEmailVerificado(false);
        when(repository.findComPerfilById(3)).thenReturn(Optional.of(usuario));

        filtro.doFilter(requisicaoComToken(jwtService.gerarAccessToken(usuario)),
                new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
