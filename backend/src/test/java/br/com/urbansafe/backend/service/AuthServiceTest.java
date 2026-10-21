package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.urbansafe.backend.dto.LoginRequest;
import br.com.urbansafe.backend.dto.LoginResponse;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.JwtService;
import br.com.urbansafe.backend.support.Fixtures;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private RefreshTokenService refreshTokenService;
    private TokenVerificacaoEmailService tokenVerificacaoService;
    private EmailService emailService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(Fixtures.SEGREDO, "urbansafe-teste", 15);
    private AuthService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        tokenVerificacaoService = mock(TokenVerificacaoEmailService.class);
        emailService = mock(EmailService.class);
        service = new AuthService(usuarioRepository, encoder, jwtService,
                refreshTokenService, tokenVerificacaoService, emailService);

        usuario = Fixtures.usuario(1, "maria@exemplo.com", "USUARIO");
        usuario.setSenhaHash(encoder.encode("SenhaForte123"));
        when(usuarioRepository.findComPerfilByEmailIgnoreCase("maria@exemplo.com"))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void loginValidoEmiteAccessERefreshSemExporSenha() {
        when(refreshTokenService.emitir(usuario)).thenReturn("refresh-original");

        LoginResponse resposta = service.login(new LoginRequest(" Maria@Exemplo.com ", "SenhaForte123"));

        assertEquals("Bearer", resposta.tokenType());
        assertEquals(900, resposta.expiresIn());
        assertEquals("refresh-original", resposta.refreshToken());
        assertEquals(1, jwtService.validarEExtrairUsuarioId(resposta.accessToken()));
        assertEquals("USUARIO", resposta.usuario().perfil());
        assertTrue(!resposta.toString().contains(usuario.getSenhaHash()));
    }

    @Test
    void senhaErradaERetornaMesmaMensagemDeEmailInexistente() {
        when(usuarioRepository.findComPerfilByEmailIgnoreCase("naoexiste@exemplo.com"))
                .thenReturn(Optional.empty());

        ApiException senhaErrada = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("maria@exemplo.com", "errada")));
        ApiException emailInexistente = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("naoexiste@exemplo.com", "qualquer")));

        assertEquals(HttpStatus.UNAUTHORIZED, senhaErrada.getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, emailInexistente.getStatus());
        assertEquals(senhaErrada.getMessage(), emailInexistente.getMessage());
        verify(refreshTokenService, never()).emitir(any());
    }

    @Test
    void contaDesativadaNaoAutentica() {
        usuario.setAtivo(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("maria@exemplo.com", "SenhaForte123")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(refreshTokenService, never()).emitir(any());
    }

    @Test
    void emailNaoVerificadoNaoAutentica() {
        usuario.setEmailVerificado(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("maria@exemplo.com", "SenhaForte123")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void contaDesativadaComSenhaErradaContinuaRetornando401() {
        usuario.setAtivo(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("maria@exemplo.com", "errada")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refreshEmiteNovoAccessERefreshRotacionado() {
        when(refreshTokenService.rotacionar("antigo"))
                .thenReturn(new RefreshTokenService.RefreshEmitido("novo-refresh", usuario));

        LoginResponse resposta = service.refresh("antigo");

        assertEquals("novo-refresh", resposta.refreshToken());
        assertEquals(1, jwtService.validarEExtrairUsuarioId(resposta.accessToken()));
    }

    @Test
    void refreshInvalidoPropaga401() {
        when(refreshTokenService.rotacionar("ruim"))
                .thenThrow(ApiException.unauthorized("Refresh token inválido, expirado ou revogado."));

        ApiException ex = assertThrows(ApiException.class, () -> service.refresh("ruim"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void logoutRevogaORefreshToken() {
        service.logout("meu-refresh");

        verify(refreshTokenService).revogar("meu-refresh");
    }

    @Test
    void reenvioNaoEnviaParaEmailJaVerificado() {
        when(usuarioRepository.findByEmailIgnoreCase("maria@exemplo.com")).thenReturn(Optional.of(usuario));

        service.reenviarVerificacao("maria@exemplo.com");

        verify(emailService, never()).enviarVerificacao(anyString(), anyString());
    }

    @Test
    void reenvioNaoFazNadaParaEmailInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("x@exemplo.com")).thenReturn(Optional.empty());

        service.reenviarVerificacao("x@exemplo.com");

        verify(emailService, never()).enviarVerificacao(anyString(), anyString());
    }

    @Test
    void reenvioEnviaNovoTokenParaContaPendente() {
        usuario.setEmailVerificado(false);
        when(usuarioRepository.findByEmailIgnoreCase("maria@exemplo.com")).thenReturn(Optional.of(usuario));
        when(tokenVerificacaoService.gerarToken(usuario)).thenReturn("tok");

        service.reenviarVerificacao(" Maria@Exemplo.com ");

        verify(emailService).enviarVerificacao("maria@exemplo.com", "tok");
    }
}
