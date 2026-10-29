package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import br.com.urbansafe.backend.entity.TokenVerificacaoEmail;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.TokenVerificacaoEmailRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.support.Fixtures;
import br.com.urbansafe.backend.util.TokenUtils;

class TokenVerificacaoEmailServiceTest {

    private TokenVerificacaoEmailRepository tokenRepository;
    private UsuarioRepository usuarioRepository;
    private TokenVerificacaoEmailService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        tokenRepository = mock(TokenVerificacaoEmailRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        service = new TokenVerificacaoEmailService(tokenRepository, usuarioRepository, 24, 60);

        usuario = Fixtures.usuario(7, "maria@exemplo.com", "USUARIO");
        usuario.setEmailVerificado(false);
    }

    private TokenVerificacaoEmail tokenPendente(String original, OffsetDateTime expiraEm) {
        TokenVerificacaoEmail t = new TokenVerificacaoEmail();
        t.setUsuario(usuario);
        t.setTokenHash(TokenUtils.sha256Hex(original));
        t.setExpiraEm(expiraEm);
        when(tokenRepository.findByTokenHash(t.getTokenHash())).thenReturn(Optional.of(t));
        return t;
    }

    // ---------- emissão ----------

    @Test
    void emissaoPersisteSomenteOHashComValidadeDe24Horas() {
        OffsetDateTime antes = OffsetDateTime.now(ZoneOffset.UTC);

        String original = service.gerarToken(usuario);

        ArgumentCaptor<TokenVerificacaoEmail> captor = ArgumentCaptor.forClass(TokenVerificacaoEmail.class);
        verify(tokenRepository).save(captor.capture());
        TokenVerificacaoEmail salvo = captor.getValue();

        assertNotNull(original);
        assertTrue(original.length() >= 43, "256 bits em Base64 URL-safe têm 43 caracteres");
        assertNotEquals(original, salvo.getTokenHash());
        assertEquals(TokenUtils.sha256Hex(original), salvo.getTokenHash());
        assertEquals(64, salvo.getTokenHash().length());
        assertEquals(usuario, salvo.getUsuario());

        Duration validade = Duration.between(antes, salvo.getExpiraEm());
        assertTrue(validade.toMinutes() >= 24 * 60 - 1 && validade.toHours() <= 24);
        assertEquals(null, salvo.getUtilizadoEm());
    }

    @Test
    void doisTokensEmitidosSaoDiferentes() {
        assertNotEquals(service.gerarToken(usuario), service.gerarToken(usuario));
    }

    @Test
    void novaEmissaoInvalidaTokensPendentesAntesDeSalvar() {
        service.gerarToken(usuario);

        InOrder ordem = inOrder(tokenRepository);
        ordem.verify(tokenRepository).invalidarPendentes(eq(7), any(OffsetDateTime.class));
        ordem.verify(tokenRepository).save(any(TokenVerificacaoEmail.class));
    }

    @Test
    void validadeSegueAConfiguracao() {
        service = new TokenVerificacaoEmailService(tokenRepository, usuarioRepository, 2, 60);
        OffsetDateTime antes = OffsetDateTime.now(ZoneOffset.UTC);

        service.gerarToken(usuario);

        ArgumentCaptor<TokenVerificacaoEmail> captor = ArgumentCaptor.forClass(TokenVerificacaoEmail.class);
        verify(tokenRepository).save(captor.capture());
        assertTrue(Duration.between(antes, captor.getValue().getExpiraEm()).toMinutes() <= 120);
    }

    // ---------- verificação ----------

    @Test
    void verificacaoValidaConsomeTokenEAtivaEmail() {
        TokenVerificacaoEmail t = tokenPendente("tok", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(tokenRepository.consumir(eq(t.getTokenHash()), any())).thenReturn(1);

        service.verificarEmail("tok");

        assertTrue(usuario.isEmailVerificado());
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).invalidarPendentes(eq(7), any());
    }

    @Test
    void tokenComEspacosNasPontasEAceito() {
        TokenVerificacaoEmail t = tokenPendente("tok", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(tokenRepository.consumir(eq(t.getTokenHash()), any())).thenReturn(1);

        service.verificarEmail("  tok \n");

        assertTrue(usuario.isEmailVerificado());
    }

    @Test
    void tokenInexistenteRetorna400ENaoAtivaNada() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.verificarEmail("nao-existe"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(TokenVerificacaoEmailService.MSG_TOKEN_INVALIDO, ex.getMessage());
        assertFalse(usuario.isEmailVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void tokenNuloOuEmBrancoRetorna400() {
        assertThrows(ApiException.class, () -> service.verificarEmail(null));
        assertThrows(ApiException.class, () -> service.verificarEmail("   "));
        verify(tokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void tokenExpiradoRetorna400ENaoConsome() {
        tokenPendente("velho", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        ApiException ex = assertThrows(ApiException.class, () -> service.verificarEmail("velho"));

        assertEquals(TokenVerificacaoEmailService.MSG_TOKEN_EXPIRADO, ex.getMessage());
        assertFalse(usuario.isEmailVerificado());
        verify(tokenRepository, never()).consumir(any(), any());
    }

    @Test
    void tokenJaUtilizadoRetorna400() {
        TokenVerificacaoEmail t = tokenPendente("usado", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        t.setUtilizadoEm(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));

        ApiException ex = assertThrows(ApiException.class, () -> service.verificarEmail("usado"));

        assertEquals(TokenVerificacaoEmailService.MSG_TOKEN_USADO, ex.getMessage());
        assertFalse(usuario.isEmailVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reutilizacaoDoMesmoTokenFalhaNaSegundaChamada() {
        TokenVerificacaoEmail t = tokenPendente("uma-vez", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(tokenRepository.consumir(eq(t.getTokenHash()), any())).thenReturn(1);

        service.verificarEmail("uma-vez");
        // Simula o estado do banco depois do consumo.
        t.setUtilizadoEm(OffsetDateTime.now(ZoneOffset.UTC));

        assertThrows(ApiException.class, () -> service.verificarEmail("uma-vez"));
        verify(usuarioRepository, org.mockito.Mockito.times(1)).save(usuario);
    }

    @Test
    void consumoConcorrenteQuePerdeAEscritaNaoAtivaOEmail() {
        // Duas requisições leem o token como pendente; o UPDATE condicional só vale
        // para a primeira.
        TokenVerificacaoEmail t = tokenPendente("corrida", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(tokenRepository.consumir(eq(t.getTokenHash()), any())).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> service.verificarEmail("corrida"));

        assertEquals(TokenVerificacaoEmailService.MSG_TOKEN_USADO, ex.getMessage());
        assertFalse(usuario.isEmailVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void contaDesativadaNaoConsomeNemVerifica() {
        usuario.setAtivo(false);
        tokenPendente("tok", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

        ApiException ex = assertThrows(ApiException.class, () -> service.verificarEmail("tok"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertFalse(usuario.isEmailVerificado());
        verify(tokenRepository, never()).consumir(any(), any());
    }

    @Test
    void tokenDeRecuperacaoDeSenhaNaoEAceitoPorqueSoExisteNaOutraTabela() {
        // O repositório de verificação só enxerga tokens_verificacao_email; um token de
        // outro fluxo não é achado.
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex("token-de-recuperacao")))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.verificarEmail("token-de-recuperacao"));
    }

    // ---------- intervalo entre reenvios ----------

    @Test
    void intervaloDeReenvioConsultaTokensRecentesDoUsuario() {
        when(tokenRepository.existsByUsuario_IdAndCriadoEmAfter(eq(7), any())).thenReturn(true);

        assertTrue(service.emIntervaloDeReenvio(7));
    }
}