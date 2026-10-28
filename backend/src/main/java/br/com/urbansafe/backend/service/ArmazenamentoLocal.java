package br.com.urbansafe.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import br.com.urbansafe.backend.exception.ApiException;

/**
 * Armazenamento em disco. Os arquivos ficam fora de qualquer diretório servido estaticamente e só são
 * entregues pela rota autenticada de download. Nomes de arquivo são UUIDs gerados aqui.
 */
@Component
public class ArmazenamentoLocal implements ArmazenamentoArquivos {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoLocal.class);
    private static final Pattern EXTENSAO_SEGURA = Pattern.compile("[a-z0-9]{1,5}");

    private final Path base;

    public ArmazenamentoLocal(@Value("${app.storage.local.path:./storage}") String caminho) {
        this.base = Path.of(caminho).toAbsolutePath().normalize();
    }

    @Override
    public Armazenado salvar(InputStream conteudo, String extensao, long limiteBytes) throws IOException {
        if (extensao == null || !EXTENSAO_SEGURA.matcher(extensao).matches()) {
            throw new IllegalArgumentException("Extensão de arquivo inválida.");
        }

        Files.createDirectories(base);
        String chave = UUID.randomUUID() + "." + extensao;
        Path destino = base.resolve(chave).normalize();

        long total = 0;
        boolean excedeu = false;
        try (OutputStream saida = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = conteudo.read(buffer)) != -1) {
                total += lidos;
                if (total > limiteBytes) {
                    excedeu = true;
                    break;
                }
                saida.write(buffer, 0, lidos);
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(destino);
            throw e;
        }

        if (excedeu) {
            Files.deleteIfExists(destino);
            throw ApiException.payloadTooLarge("O arquivo excede o tamanho máximo permitido.");
        }
        return new Armazenado(chave, total);
    }

    @Override
    public Resource carregar(String chave) {
        Path arquivo = resolver(chave);
        if (arquivo == null || !Files.isRegularFile(arquivo)) {
            throw ApiException.notFound("Arquivo não encontrado.");
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public void remover(String chave) {
        Path arquivo = resolver(chave);
        if (arquivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo {}", chave);
        }
    }

    /** Impede path traversal: a chave resolvida precisa continuar dentro do diretório base. */
    private Path resolver(String chave) {
        if (chave == null || chave.isBlank()) {
            return null;
        }
        Path resolvido = base.resolve(chave).normalize();
        return resolvido.startsWith(base) ? resolvido : null;
    }
}
