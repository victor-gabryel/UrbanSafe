package br.com.urbansafe.backend.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApi(ApiException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroResponse.CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroResponse.CampoErro(e.getField(), e.getDefaultMessage()))
                .toList();

        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou em formato JSON inválido.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'.", request, List.of());
    }

    /** Upload acima do limite do servidor (413) ou requisição multipart malformada (400). */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErroResponse> tratarMultipart(MultipartException ex, HttpServletRequest request) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return resposta(HttpStatus.valueOf(413),
                    "O arquivo ou a requisição excede o tamanho máximo permitido.", request, List.of());
        }
        return resposta(HttpStatus.BAD_REQUEST, "Requisição multipart inválida.", request, List.of());
    }

    /** Ex.: corrida de dois cadastros simultâneos com o mesmo e-mail (índice único). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade de dados em {}", request.getRequestURI());
        return resposta(HttpStatus.CONFLICT,
                "A operação conflita com dados já existentes.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(
            AccessDeniedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN,
                "Você não tem permissão para acessar este recurso.", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> tratarAutenticacao(
            AuthenticationException ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED,
                "Autenticação necessária.", request, List.of());
    }

    /**
     * Qualquer outra exceção. Exceções padrão do Spring MVC (405, 415, 404 de rota...)
     * implementam ErrorResponse e preservam o próprio status; o restante vira 500 genérico,
     * sem vazar detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return resposta(status, status.getReasonPhrase() + ".", request, List.of());
            }
        }

        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Tente novamente mais tarde.", request, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String mensagem, HttpServletRequest request,
            List<ErroResponse.CampoErro> campos) {
        return ResponseEntity.status(status)
                .body(ErroResponse.de(status, mensagem, request.getRequestURI(), campos));
    }
}
