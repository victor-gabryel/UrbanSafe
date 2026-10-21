package br.com.urbansafe.backend.security;

public class TokenJwtException extends RuntimeException {

    public enum Motivo {
        EXPIRADO,
        INVALIDO
    }

    private final Motivo motivo;

    public TokenJwtException(Motivo motivo, Throwable causa) {
        super(motivo.name(), causa);
        this.motivo = motivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }
}
