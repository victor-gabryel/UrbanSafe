package br.com.urbansafe.backend.exception;

import org.springframework.http.HttpStatus;

/** Erro de negócio/aplicação com status HTTP já definido e mensagem segura para o cliente. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ApiException unauthorized(String mensagem) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensagem);
    }

    public static ApiException forbidden(String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, mensagem);
    }

    public static ApiException notFound(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException conflict(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    public static ApiException tooManyRequests(String mensagem) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, mensagem);
    }

    /** 413. Usa valueOf(int) para não depender do nome da constante, que mudou entre versões do Spring. */
    public static ApiException payloadTooLarge(String mensagem) {
        return new ApiException(HttpStatus.valueOf(413), mensagem);
    }

    public static ApiException unsupportedMediaType(String mensagem) {
        return new ApiException(HttpStatus.valueOf(415), mensagem);
    }
}
