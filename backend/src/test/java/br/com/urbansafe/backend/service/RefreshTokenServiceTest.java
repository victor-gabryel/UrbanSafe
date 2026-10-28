package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.springframework.test.util.ReflectionTestUtils;

import br.com.urbansafe.backend.entity.RefreshToken;
import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.exception.ApiException;
import br.com.urbansafe.backend.repository.RefreshTokenRepository;
import br.com.urbansafe.backend.support.Fixtures;
import br.com.urbansafe.backend.util.TokenUtils;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository, 7);
        usuario = Fixtures.usuario(1, "a@b.com", "USUARIO");
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RefreshToken tokenArmazenado(String original, OffsetDateTime expira, OffsetDateTime revogado) {
        RefreshToken token = new RefreshToken();
        ReflectionTestUtils.setField(token, "id", 10);
        token.setUsuario(usuario);
        token.setTokenHash(TokenUtils.sha256Hex(original));
        token.setExpiraEm(expira);
        token.setRevogadoEm(revogado);
        when(repository.findByTokenHash(TokenUtils.sha256Hex(original))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void emitirGravaApenasOHashDoToken() {
        String original = service.emitir(usuario);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        assertNotEquals(original, captor.getValue().getTokenHash());
        assertEquals(TokenUtils.sha256Hex(original), captor.getValue().getTokenHash());
        assertEquals(64, captor.getValue().getTokenHash().length());
    }

    @Test
    void rotacionarRevogaOAtualEEmiteUmNovo() {
        tokenArmazenado("atual", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        when(repository.revogarSeAtivo(anyInt(), any())).thenReturn(1);

        RefreshTokenService.RefreshEmitido emitido = service.rotacionar("atual");

        verify(repository).revogarSeAtivo(anyInt(), any());
        assertNotEquals("atual", emitido.tokenOriginal());
        assertEquals(usuario, emitido.usuario());
    }

    @Test
    void tokenDesconhecidoRetorna401() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.rotacionar("qualquer"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void tokenExpiradoRetorna401SemEmitirOutro() {
        tokenArmazenado("velho", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);

        assertThrows(ApiException.class, () -> service.rotacionar("velho"));

        verify(repository, never()).save(any());
    }

    @Test
    void reusoDeTokenRevogadoDerrubaTodasAsSessoesDoUsuario() {
        tokenArmazenado("usado", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));

        ApiException ex = assertThrows(ApiException.class, () -> service.rotacionar("usado"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(repository).revogarTodosDoUsuario(anyInt(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void usoConcorrenteDoMesmoTokenERejeitado() {
        tokenArmazenado("corrida", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        when(repository.revogarSeAtivo(anyInt(), any())).thenReturn(0);

        assertThrows(ApiException.class, () -> service.rotacionar("corrida"));

        verify(repository).revogarTodosDoUsuario(anyInt(), any());
    }

    @Test
    void contaDesativadaNaoRenovaSessao() {
        usuario.setAtivo(false);
        tokenArmazenado("valido", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), null);

        assertThrows(ApiException.class, () -> service.rotacionar("valido"));

        verify(repository, never()).save(any());
    }

    @Test
    void revogarTokenDesconhecidoNaoFalha() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revogar("nada");

        verify(repository, never()).revogarSeAtivo(anyInt(), any());
    }
}
