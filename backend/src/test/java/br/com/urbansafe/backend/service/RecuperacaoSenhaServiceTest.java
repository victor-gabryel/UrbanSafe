package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.urbansafe.backend.entity.TokenRecuperacaoSenha;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TokenRecuperacaoSenhaRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.support.Fixtures;
import br.com.urbansafe.backend.util.TokenUtils;

class RecuperacaoSenhaServiceTest {

    private UsuarioRepository usuarioRepository;
    private TokenRecuperacaoSenhaRepository tokenRepository;
    private RefreshTokenService refreshTokenService;
    private EmailService emailService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RecuperacaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenRecuperacaoSenhaRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailService = mock(EmailService.class);
        service = new RecuperacaoSenhaService(usuarioRepository, tokenRepository,
                refreshTokenService, encoder, emailService, 60);
        usuario = Fixtures.usuario(1, "maria@exemplo.com", "USUARIO");
        usuario.setSenhaHash(encoder.encode("SenhaAntiga123"));
    }

    private TokenRecuperacaoSenha tokenArmazenado(String original, OffsetDateTime expira, OffsetDateTime usado) {
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
        token.setUsuario(usuario);
        token.setTokenHash(TokenUtils.sha256Hex(original));
        token.setExpiraEm(expira);
        token.setUtilizadoEm(usado);
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex(original))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void solicitarParaEmailExistenteGravaHashEEnviaOTokenOriginal() {
        when(usuarioRepository.findByEmailIgnoreCase("maria@exemplo.com")).thenReturn(Optional.of(usuario));

        service.solicitar(" Maria@Exemplo.com ");

        ArgumentCaptor<TokenRecuperacaoSenha> salvo = ArgumentCaptor.forClass(TokenRecuperacaoSenha.class);
        verify(tokenRepository).save(salvo.capture());
        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRecuperacaoSenha(anyString(), enviado.capture());

        assertEquals(TokenUtils.sha256Hex(enviado.getValue()), salvo.getValue().getTokenHash());
        assertTrue(salvo.getValue().getExpiraEm().isAfter(OffsetDateTime.now(ZoneOffset.UTC)));
        verify(tokenRepository).invalidarPendentes(anyInt(), any());
    }

    @Test
    void solicitarParaEmailInexistenteNaoFazNadaENaoFalha() {
        when(usuarioRepository.findByEmailIgnoreCase("x@exemplo.com")).thenReturn(Optional.empty());

        service.solicitar("x@exemplo.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarRecuperacaoSenha(anyString(), anyString());
    }

    @Test
    void solicitarParaContaDesativadaNaoEnvia() {
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmailIgnoreCase("maria@exemplo.com")).thenReturn(Optional.of(usuario));

        service.solicitar("maria@exemplo.com");

        verify(emailService, never()).enviarRecuperacaoSenha(anyString(), anyString());
    }

    @Test
    void redefinirTrocaASenhaComBcryptConsomeOTokenERevogaSessoes() {
        TokenRecuperacaoSenha token = tokenArmazenado("tok",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30), null);

        service.redefinir("tok", "NovaSenha456");

        assertTrue(encoder.matches("NovaSenha456", usuario.getSenhaHash()));
        assertNotNull(token.getUtilizadoEm());
        verify(refreshTokenService).revogarTodos(1);
    }

    @Test
    void tokenExpiradoERejeitadoSemTrocarASenha() {
        tokenArmazenado("tok", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);
        String hashAntes = usuario.getSenhaHash();

        ApiException ex = assertThrows(ApiException.class, () -> service.redefinir("tok", "NovaSenha456"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(hashAntes, usuario.getSenhaHash());
        verify(refreshTokenService, never()).revogarTodos(anyInt());
    }

    @Test
    void tokenJaUtilizadoERejeitadoComAMesmaMensagem() {
        tokenArmazenado("usado", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex("inexistente"))).thenReturn(Optional.empty());

        ApiException usado = assertThrows(ApiException.class, () -> service.redefinir("usado", "NovaSenha456"));
        ApiException inexistente = assertThrows(ApiException.class,
                () -> service.redefinir("inexistente", "NovaSenha456"));

        assertEquals(usado.getMessage(), inexistente.getMessage());
    }

    @Test
    void novaSenhaAcimaDe72BytesERejeitada() {
        assertThrows(ApiException.class, () -> service.redefinir("tok", "é".repeat(40)));

        verify(tokenRepository, never()).findByTokenHash(anyString());
    }
}