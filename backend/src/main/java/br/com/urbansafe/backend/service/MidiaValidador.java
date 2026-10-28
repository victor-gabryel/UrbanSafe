package br.com.urbansafe.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.urbansafe.backend.entity.TipoMidia;

/**
 * Detecta o tipo REAL do arquivo pelos primeiros bytes (assinatura / "magic number"). A extensão e o
 * Content-Type enviados pelo cliente são ignorados. Formatos aceitos:
 * JPEG, PNG, WebP (IMAGEM); MP4 (VIDEO); MP3, WAV (AUDIO); PDF (DOCUMENTO).
 */
@Component
public class MidiaValidador {

    public static final int BYTES_CABECALHO = 16;

    private static final Set<String> MARCAS_MP4 = Set.of(
            "isom", "iso2", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "dash", "M4V ");

    public record Detectado(TipoMidia tipo, String mime, String extensao) {
    }

    public Optional<Detectado> detectar(byte[] h) {
        if (h == null || h.length < 4) {
            return Optional.empty();
        }

        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return detectado(TipoMidia.IMAGEM, "image/jpeg", "jpg");
        }
        if (comeca(h, new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A })) {
            return detectado(TipoMidia.IMAGEM, "image/png", "png");
        }
        if (h.length >= 12 && ascii(h, 0, 4).equals("RIFF")) {
            String formato = ascii(h, 8, 12);
            if (formato.equals("WEBP")) {
                return detectado(TipoMidia.IMAGEM, "image/webp", "webp");
            }
            if (formato.equals("WAVE")) {
                return detectado(TipoMidia.AUDIO, "audio/wav", "wav");
            }
            return Optional.empty();
        }
        if (h.length >= 12 && ascii(h, 4, 8).equals("ftyp") && MARCAS_MP4.contains(ascii(h, 8, 12))) {
            return detectado(TipoMidia.VIDEO, "video/mp4", "mp4");
        }
        if (h.length >= 5 && ascii(h, 0, 5).equals("%PDF-")) {
            return detectado(TipoMidia.DOCUMENTO, "application/pdf", "pdf");
        }
        if (ascii(h, 0, 3).equals("ID3") || ehQuadroMp3(h)) {
            return detectado(TipoMidia.AUDIO, "audio/mpeg", "mp3");
        }
        return Optional.empty();
    }

    private boolean ehQuadroMp3(byte[] h) {
        // Sincronismo de quadro MPEG: 11 bits em 1 e camada diferente de "00".
        return (h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0 && (h[1] & 0x06) != 0x00;
    }

    private boolean comeca(byte[] dados, byte[] prefixo) {
        return dados.length >= prefixo.length
                && Arrays.equals(Arrays.copyOfRange(dados, 0, prefixo.length), prefixo);
    }

    private String ascii(byte[] dados, int inicio, int fim) {
        return new String(dados, inicio, Math.min(fim, dados.length) - inicio, StandardCharsets.ISO_8859_1);
    }

    private Optional<Detectado> detectado(TipoMidia tipo, String mime, String extensao) {
        return Optional.of(new Detectado(tipo, mime, extensao));
    }
}
