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

/** Evento imutável (somente acréscimo; o banco bloqueia UPDATE e DELETE). Sem setters de propósito. */
@Entity
@Table(name = "historico_ocorrencia")
public class HistoricoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ocorrencia_id", nullable = false, updatable = false)
    private Integer ocorrenciaId;

    @Column(name = "usuario_id", updatable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, updatable = false, length = 40)
    private AcaoHistorico acao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_anterior", updatable = false, columnDefinition = "status_ocorrencia")
    private StatusOcorrencia statusAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_novo", updatable = false, columnDefinition = "status_ocorrencia")
    private StatusOcorrencia statusNovo;

    @Column(name = "descricao", nullable = false, updatable = false)
    private String descricao;

    @Column(name = "data_evento", nullable = false, updatable = false)
    private OffsetDateTime dataEvento;

    protected HistoricoOcorrencia() {
    }

    public HistoricoOcorrencia(Integer ocorrenciaId, Integer usuarioId, AcaoHistorico acao,
            StatusOcorrencia statusAnterior, StatusOcorrencia statusNovo, String descricao) {
        this.ocorrenciaId = ocorrenciaId;
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.descricao = descricao;
    }

    @PrePersist
    protected void aoCriar() {
        if (dataEvento == null) {
            dataEvento = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Integer getId() {
        return id;
    }

    public Integer getOcorrenciaId() {
        return ocorrenciaId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public AcaoHistorico getAcao() {
        return acao;
    }

    public StatusOcorrencia getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOcorrencia getStatusNovo() {
        return statusNovo;
    }

    public String getDescricao() {
        return descricao;
    }

    public OffsetDateTime getDataEvento() {
        return dataEvento;
    }
}
