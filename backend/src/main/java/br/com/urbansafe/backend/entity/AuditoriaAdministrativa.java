package br.com.urbansafe.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Registro imutável de operação administrativa relevante. */
@Entity
@Table(name = "auditoria_administrativa")
public class AuditoriaAdministrativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Column(name = "acao", nullable = false, updatable = false, length = 60)
    private String acao;

    @Column(name = "entidade", nullable = false, updatable = false, length = 60)
    private String entidade;

    @Column(name = "entidade_id", updatable = false)
    private Integer entidadeId;

    @Column(name = "detalhes", updatable = false)
    private String detalhes;

    @Column(name = "data_evento", nullable = false, updatable = false)
    private OffsetDateTime dataEvento;

    protected AuditoriaAdministrativa() {
    }

    public AuditoriaAdministrativa(Integer usuarioId, String acao, String entidade, Integer entidadeId,
            String detalhes) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes;
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

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public Integer getEntidadeId() {
        return entidadeId;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public OffsetDateTime getDataEvento() {
        return dataEvento;
    }
}
