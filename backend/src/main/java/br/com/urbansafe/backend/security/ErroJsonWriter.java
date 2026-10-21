package br.com.urbansafe.backend.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletResponse;

/** Escreve o mesmo formato de ErroResponse sem depender de ObjectMapper (usado fora do MVC, nos handlers de segurança). */
final class ErroJsonWriter {

    private ErroJsonWriter() {
    }

    static void escrever(HttpServletResponse response, HttpStatus status, String mensagem, String caminho)
            throws IOException {

        String json = "{\"timestamp\":\"" + OffsetDateTime.now(ZoneOffset.UTC) + "\""
                + ",\"status\":" + status.value()
                + ",\"erro\":\"" + escapar(status.getReasonPhrase()) + "\""
                + ",\"mensagem\":\"" + escapar(mensagem) + "\""
                + ",\"caminho\":\"" + escapar(caminho) + "\""
                + ",\"campos\":[]}";

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(valor.length() + 8);
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
