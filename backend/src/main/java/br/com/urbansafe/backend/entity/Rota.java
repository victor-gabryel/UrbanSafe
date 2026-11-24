package br.com.urbansafe.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Consulta de rota gravada para o histórico do usuário autenticado (tabela "rotas", seção 12.1.7).
 * A geometria do percurso não é persistida: ela é sempre recalculada pelo serviço de roteamento.
 */
@Entity
@Table(name = "rotas")
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "origem", length = 255)
    private String origem;

    @Column(name = "destino", length = 255)
    private String destino;

    @Column(name = "origem_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal origemLatitude;

    @Column(name = "origem_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal origemLongitude;

    @Column(name = "destino_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinoLatitude;

    @Column(name = "destino_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinoLongitude;

    /** Distância em metros. */
    @Column(name = "distancia", nullable = false, precision = 10, scale = 2)
    private BigDecimal distancia;

    /** Tempo estimado em segundos. */
    @Column(name = "tempo_estimado", nullable = false)
    private Integer tempoEstimado;

    /** Indicador informativo de 0 a 100; nulo quando não foi calculado. */
    @Column(name = "indice_exposicao", precision = 5, scale = 2)
    private BigDecimal indiceExposicao;

    @Column(name = "data_calculo", nullable = false, updatable = false)
    private OffsetDateTime dataCalculo;

    public Rota() {
    }

    @PrePersist
    protected void aoCriar() {
        if (dataCalculo == null) {
            dataCalculo = OffsetDateTime.now(ZoneOffset.UTC);
        }
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

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public BigDecimal getOrigemLatitude() {
        return origemLatitude;
    }

    public void setOrigemLatitude(BigDecimal origemLatitude) {
        this.origemLatitude = origemLatitude;
    }

    public BigDecimal getOrigemLongitude() {
        return origemLongitude;
    }

    public void setOrigemLongitude(BigDecimal origemLongitude) {
        this.origemLongitude = origemLongitude;
    }

    public BigDecimal getDestinoLatitude() {
        return destinoLatitude;
    }

    public void setDestinoLatitude(BigDecimal destinoLatitude) {
        this.destinoLatitude = destinoLatitude;
    }

    public BigDecimal getDestinoLongitude() {
        return destinoLongitude;
    }

    public void setDestinoLongitude(BigDecimal destinoLongitude) {
        this.destinoLongitude = destinoLongitude;
    }

    public BigDecimal getDistancia() {
        return distancia;
    }

    public void setDistancia(BigDecimal distancia) {
        this.distancia = distancia;
    }

    public Integer getTempoEstimado() {
        return tempoEstimado;
    }

    public void setTempoEstimado(Integer tempoEstimado) {
        this.tempoEstimado = tempoEstimado;
    }

    public BigDecimal getIndiceExposicao() {
        return indiceExposicao;
    }

    public void setIndiceExposicao(BigDecimal indiceExposicao) {
        this.indiceExposicao = indiceExposicao;
    }

    public OffsetDateTime getDataCalculo() {
        return dataCalculo;
    }
}
