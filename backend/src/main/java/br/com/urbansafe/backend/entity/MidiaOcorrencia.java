package br.com.urbansafe.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "midias_ocorrencia")
public class MidiaOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ocorrencia_id", nullable = false, updatable = false)
    private Integer ocorrenciaId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_midia")
    private TipoMidia tipo;

    /** Chave interna no armazenamento (não é URL pública). */
    @Column(name = "url", nullable = false)
    private String chaveArmazenamento;

    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    @Column(name = "descricao")
    private String descricao;

    /** MIME detectado pelo conteúdo do arquivo (não o informado pelo cliente). */
    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "data_envio", nullable = false, updatable = false)
    private OffsetDateTime dataEnvio;

    @Column(name = "removida_em")
    private OffsetDateTime removidaEm;

    @Column(name = "removida_por")
    private Integer removidaPor;

    public MidiaOcorrencia() {
    }

    @PrePersist
    protected void aoCriar() {
        if (dataEnvio == null) {
            dataEnvio = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public void remover(Integer usuarioId) {
        this.removidaEm = OffsetDateTime.now(ZoneOffset.UTC);
        this.removidaPor = usuarioId;
    }

    public boolean isRemovida() {
        return removidaEm != null;
    }

    public Integer getId() {
        return id;
    }

    public Integer getOcorrenciaId() {
        return ocorrenciaId;
    }

    public void setOcorrenciaId(Integer ocorrenciaId) {
        this.ocorrenciaId = ocorrenciaId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public TipoMidia getTipo() {
        return tipo;
    }

    public void setTipo(TipoMidia tipo) {
        this.tipo = tipo;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public void setChaveArmazenamento(String chaveArmazenamento) {
        this.chaveArmazenamento = chaveArmazenamento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public void setTipoConteudo(String tipoConteudo) {
        this.tipoConteudo = tipoConteudo;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public OffsetDateTime getDataEnvio() {
        return dataEnvio;
    }

    public OffsetDateTime getRemovidaEm() {
        return removidaEm;
    }

    public Integer getRemovidaPor() {
        return removidaPor;
    }
}
