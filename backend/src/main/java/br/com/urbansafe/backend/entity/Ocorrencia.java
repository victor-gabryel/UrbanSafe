package br.com.urbansafe.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Relato georreferenciado. A coluna geográfica "localizacao" é GERADA pelo banco a partir de
 * longitude/latitude e por isso NÃO é mapeada aqui: as consultas espaciais rodam em SQL nativo
 * (OcorrenciaConsultaRepository), nunca em memória.
 */
@Entity
@Table(name = "ocorrencias")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Autor. Sempre definido a partir da identidade autenticada, nunca do corpo da requisição. */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_ocorrencia_id", nullable = false)
    private TipoOcorrencia tipo;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "endereco")
    private String endereco;

    @Column(name = "data_ocorrencia")
    private OffsetDateTime dataOcorrencia;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private OffsetDateTime dataCadastro;

    @Column(name = "data_atualizacao", nullable = false)
    private OffsetDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "status_ocorrencia")
    private StatusOcorrencia status = StatusOcorrencia.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "relevancia", nullable = false, length = 10)
    private Relevancia relevancia = Relevancia.MEDIA;

    @Column(name = "anonimato", nullable = false)
    private boolean anonimato = false;

    @Column(name = "excluida_em")
    private OffsetDateTime excluidaEm;

    @Column(name = "excluida_por")
    private Integer excluidaPor;

    public Ocorrencia() {
    }

    @PrePersist
    protected void aoCriar() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        if (dataCadastro == null) {
            dataCadastro = agora;
        }
        dataAtualizacao = agora;
    }

    @PreUpdate
    protected void aoAtualizar() {
        dataAtualizacao = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isExcluida() {
        return excluidaEm != null;
    }

    public void excluirLogicamente(Integer usuarioId) {
        this.excluidaEm = OffsetDateTime.now(ZoneOffset.UTC);
        this.excluidaPor = usuarioId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public TipoOcorrencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoOcorrencia tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public OffsetDateTime getDataOcorrencia() {
        return dataOcorrencia;
    }

    public void setDataOcorrencia(OffsetDateTime dataOcorrencia) {
        this.dataOcorrencia = dataOcorrencia;
    }

    public OffsetDateTime getDataCadastro() {
        return dataCadastro;
    }

    public OffsetDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public Relevancia getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(Relevancia relevancia) {
        this.relevancia = relevancia;
    }

    public boolean isAnonimato() {
        return anonimato;
    }

    public void setAnonimato(boolean anonimato) {
        this.anonimato = anonimato;
    }

    public OffsetDateTime getExcluidaEm() {
        return excluidaEm;
    }

    public Integer getExcluidaPor() {
        return excluidaPor;
    }
}
