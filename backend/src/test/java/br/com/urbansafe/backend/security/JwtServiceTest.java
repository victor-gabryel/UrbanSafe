package br.com.urbansafe.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.support.Fixtures;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(Fixtures.SEGREDO, "urbansafe-teste", 15);
    private final Usuario usuario = Fixtures.usuario(7, "a@b.com", "USUARIO");

    @Test
    void tokenValidoDevolveIdDoUsuario() {
        String token = jwtService.gerarAccessToken(usuario);

        assertEquals(7, jwtService.validarEExtrairUsuarioId(token));
    }

    @Test
    void tokenExpiradoELancadoComoExpirado() {
        String token = jwtService.gerarAccessToken(usuario, Instant.now().minusSeconds(3600));

        TokenJwtException ex = assertThrows(TokenJwtException.class,
                () -> jwtService.validarEExtrairUsuarioId(token));

        assertEquals(TokenJwtException.Motivo.EXPIRADO, ex.getMotivo());
    }

    @Test
    void tokenMalformadoELancadoComoInvalido() {
        TokenJwtException ex = assertThrows(TokenJwtException.class,
                () -> jwtService.validarEExtrairUsuarioId("isto.nao.eh-um-jwt"));

        assertEquals(TokenJwtException.Motivo.INVALIDO, ex.getMotivo());
    }

    @Test
    void tokenAssinadoComOutroSegredoEInvalido() {
        JwtService outro = new JwtService(
                "outro-segredo-totalmente-diferente-com-32-bytes-ou-mais", "urbansafe-teste", 15);
        String token = outro.gerarAccessToken(usuario);

        TokenJwtException ex = assertThrows(TokenJwtException.class,
                () -> jwtService.validarEExtrairUsuarioId(token));

        assertEquals(TokenJwtException.Motivo.INVALIDO, ex.getMotivo());
    }

    @Test
    void tokenDeOutroEmissorEInvalido() {
        JwtService outro = new JwtService(Fixtures.SEGREDO, "outro-emissor", 15);
        String token = outro.gerarAccessToken(usuario);

        assertThrows(TokenJwtException.class, () -> jwtService.validarEExtrairUsuarioId(token));
    }

    @Test
    void segredoCurtoImpedeInicializacao() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto", "x", 15));
    }

    @Test
    void expiracaoDoAccessTokenEhConfiguravel() {
        assertEquals(900, jwtService.getExpiracaoEmSegundos());
    }
}
