package br.com.urbansafe.backend.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

/**
 * Abstração do armazenamento de arquivos de mídia. A implementação padrão grava em disco local
 * ({@link ArmazenamentoLocal}); para S3-compatível ou outro provedor, basta outra implementação deste bean.
 * O banco guarda somente a chave devolvida por {@link #salvar}.
 */
public interface ArmazenamentoArquivos {

    record Armazenado(String chave, long bytes) {
    }

    /**
     * Grava o conteúdo e devolve a chave interna.
     *
     * @param extensao  extensão segura derivada do tipo detectado (nunca o nome enviado pelo cliente)
     * @param limiteBytes interrompe e descarta o arquivo se o conteúdo real exceder este tamanho
     */
    Armazenado salvar(InputStream conteudo, String extensao, long limiteBytes) throws IOException;

    /** Abre o arquivo; lança ApiException 404 se não existir. */
    Resource carregar(String chave);

    /** Remove o arquivo, se existir (usado para desfazer uploads cuja gravação no banco falhou). */
    void remover(String chave);
}
