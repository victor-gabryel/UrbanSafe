package com.urbansafe.seguranca;

import com.urbansafe.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long tempoExpiracaoSegundos;

    public JwtService(
            @Value("${app.jwt.secret}") String segredo,
            @Value("${app.jwt.expiration-seconds:3600}") long tempoExpiracaoSegundos) {

        if (segredo == null
                || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "A chave JWT deve possuir pelo menos 32 bytes.");
        }

        this.chave = Keys.hmacShaKeyFor(
                segredo.getBytes(StandardCharsets.UTF_8));

        this.tempoExpiracaoSegundos = tempoExpiracaoSegundos;
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(
                agora.getTime() + tempoExpiracaoSegundos * 1000L);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().getNome())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairUsuarioId(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (io.jsonwebtoken.JwtException
                | IllegalArgumentException e) {
            return false;
        }
    }

    public long getTempoExpiracaoSegundos() {
        return tempoExpiracaoSegundos;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}