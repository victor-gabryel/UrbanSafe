package br.com.urbansafe.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.repository.UsuarioRepository;

/**
 * Autentica a requisição a partir do header "Authorization: Bearer ...".
 * Token inválido/expirado NÃO interrompe a cadeia: a requisição segue sem autenticação e o
 * AuthenticationEntryPoint responde 401 nas rotas protegidas (rotas públicas continuam funcionando).
 * O usuário é recarregado do banco a cada requisição: conta desativada perde o acesso imediatamente
 * e o perfil usado nas autoridades é sempre o atual.
 *
 * Não é um @Component de propósito: é instanciado no SecurityConfig para não ser registrado duas vezes.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_MOTIVO = "urbansafe.auth.motivo";

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null
                && header.regionMatches(true, 0, PREFIXO_BEARER, 0, PREFIXO_BEARER.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIXO_BEARER.length()).trim();

            try {
                Integer usuarioId = jwtService.validarEExtrairUsuarioId(token);
                Optional<Usuario> usuario = usuarioRepository.findComPerfilById(usuarioId);

                // Conta desativada ou com e-mail não verificado não acessa rotas protegidas, mesmo com JWT válido.
                if (usuario.isPresent() && usuario.get().isAtivo() && usuario.get().isEmailVerificado()) {
                    autenticar(usuario.get(), request);
                } else {
                    request.setAttribute(ATRIBUTO_MOTIVO, TokenJwtException.Motivo.INVALIDO);
                }
            } catch (TokenJwtException e) {
                request.setAttribute(ATRIBUTO_MOTIVO, e.getMotivo());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario, HttpServletRequest request) {
        String perfil = usuario.getPerfil().getNome();

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario.getId(), usuario.getEmail(), perfil);
        List<GrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + perfil));

        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(principal, null, autoridades);
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
    }
}
