package br.com.urbansafe.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Decisão de moderação imutável (o banco bloqueia UPDATE e DELETE). */
@Entity
@Table(name = "moderacoes")
public class Moderacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ocorrencia_id", nullable = false, updatable = false)
    private Integer ocorrenciaId;

    @Column(name = "moderador_id", nullable = false, updatable = false)
    private Integer moderadorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "acao", nullable = false, updatable = false, columnDefinition = "acao_moderacao")
    private AcaoModeracao acao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_anterior", nullable = false, updatable = false, columnDefinition = "status_ocorrencia")
    private StatusOcorrencia statusAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_novo", nullable = false, updatable = false, columnDefinition = "status_ocorrencia")
    private StatusOcorrencia statusNovo;

    @Column(name = "justificativa", updatable = false)
    private String justificativa;

    @Column(name = "data_moderacao", nullable = false, updatable = false)
    private OffsetDateTime dataModeracao;

    protected Moderacao() {
    }

    public Moderacao(Integer ocorrenciaId, Integer moderadorId, AcaoModeracao acao,
            StatusOcorrencia statusAnterior, StatusOcorrencia statusNovo, String justificativa) {
        this.ocorrenciaId = ocorrenciaId;
        this.moderadorId = moderadorId;
        this.acao = acao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.justificativa = justificativa;
    }

    @PrePersist
    protected void aoCriar() {
        if (dataModeracao == null) {
            dataModeracao = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Integer getId() {
        return id;
    }

    public Integer getOcorrenciaId() {
        return ocorrenciaId;
    }

    public Integer getModeradorId() {
        return moderadorId;
    }

    public AcaoModeracao getAcao() {
        return acao;
    }

    public StatusOcorrencia getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOcorrencia getStatusNovo() {
        return statusNovo;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public OffsetDateTime getDataModeracao() {
        return dataModeracao;
    }
}
