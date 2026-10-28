package br.com.urbansafe.backend.entity;

/** Estado editorial do relato (tipo PostgreSQL status_ocorrencia). Não é confirmação oficial do fato. */
public enum StatusOcorrencia {
    PENDENTE,
    PUBLICADA,
    REJEITADA,
    OCULTA
}
