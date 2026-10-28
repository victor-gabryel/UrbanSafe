package br.com.urbansafe.backend.entity;

/** Eventos registrados em historico_ocorrencia (coluna "acao"). */
public enum AcaoHistorico {
    CRIACAO,
    ATUALIZACAO,
    MUDANCA_STATUS,
    MODERACAO,
    EXCLUSAO,
    MIDIA_ADICIONADA,
    MIDIA_REMOVIDA
}
