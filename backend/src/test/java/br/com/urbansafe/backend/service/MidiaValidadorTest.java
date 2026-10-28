package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import br.com.urbansafe.backend.entity.TipoMidia;

class MidiaValidadorTest {

    private final MidiaValidador validador = new MidiaValidador();

    private byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void detectaFormatosPermitidosPeloConteudo() {
        var jpeg = validador.detectar(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0 });
        assertEquals(TipoMidia.IMAGEM, jpeg.orElseThrow().tipo());
        assertEquals("image/jpeg", jpeg.orElseThrow().mime());

        var png = validador.detectar(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 });
        assertEquals("image/png", png.orElseThrow().mime());

        assertEquals("image/webp", validador.detectar(ascii("RIFF\0\0\0\0WEBPVP8 ")).orElseThrow().mime());
        assertEquals("audio/wav", validador.detectar(ascii("RIFF\0\0\0\0WAVEfmt ")).orElseThrow().mime());
        assertEquals(TipoMidia.VIDEO, validador.detectar(ascii("\0\0\0\u0018ftypmp42\0\0\0\0")).orElseThrow().tipo());
        assertEquals(TipoMidia.DOCUMENTO, validador.detectar(ascii("%PDF-1.7\n")).orElseThrow().tipo());
        assertEquals("audio/mpeg", validador.detectar(ascii("ID3\u0004\0\0\0\0\0\0")).orElseThrow().mime());
    }

    @Test
    void rejeitaExecutaveisScriptsEHeic() {
        assertTrue(validador.detectar(ascii("MZ\u0090\0\u0003\0\0\0")).isEmpty()); // .exe
        assertTrue(validador.detectar(ascii("<?php echo 1;")).isEmpty());
        assertTrue(validador.detectar(ascii("#!/bin/sh\nrm -rf")).isEmpty());
        assertTrue(validador.detectar(ascii("<html><script>")).isEmpty());
        assertTrue(validador.detectar(ascii("\0\0\0\u0018ftypheic\0\0\0\0")).isEmpty());
        assertTrue(validador.detectar(ascii("RIFF\0\0\0\0AVI LIST")).isEmpty());
    }

    @Test
    void ignoraConteudoVazioOuCurto() {
        assertTrue(validador.detectar(null).isEmpty());
        assertTrue(validador.detectar(new byte[0]).isEmpty());
        assertTrue(validador.detectar(new byte[] { 1, 2 }).isEmpty());
    }
}
