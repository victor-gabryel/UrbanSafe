package br.com.urbansafe.backend.util;

import java.nio.charset.StandardCharsets;

import br.com.urbansafe.backend.exception.ApiException;

public final class SenhaValidator {

    /** Limite real do BCrypt: 72 BYTES (e não caracteres). */
    private static final int LIMITE_BYTES_BCRYPT = 72;

    private SenhaValidator() {
    }

    public static void validarTamanhoBcrypt(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT) {
            throw ApiException.badRequest(
                    "A senha deve ter no máximo 72 bytes (caracteres acentuados e especiais ocupam mais de 1 byte).");
        }
    }
}
