package br.com.urbansafe.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import br.com.urbansafe.backend.dto.FiltroOcorrencia;
import br.com.urbansafe.backend.dto.MapaOcorrenciaResponse;
import br.com.urbansafe.backend.dto.OcorrenciaResponse;
import br.com.urbansafe.backend.dto.PontoCalorResponse;
import br.com.urbansafe.backend.entity.Relevancia;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.util.BoundingBox;

/**
 * Consultas de leitura que dependem do PostGIS. Todo filtro espacial é executado NO BANCO
 * (ST_Intersects + ST_MakeEnvelope sobre a coluna geography indexada por GiST); nada é filtrado em Java.
 *
 * <p>Compatibilidade geography/geometry: o envelope é criado como geometry (SRID 4326) e convertido
 * explicitamente com CAST(... AS geography) para que o operador de interseção use o índice GiST de
 * {@code ocorrencias.localizacao}. A agregação do mapa de calor converte para geometry explicitamente
 * apenas para usar ST_SnapToGrid.
 */
@Repository
public class OcorrenciaConsultaRepository {

    /**
     * Quem consulta e em qual contexto.
     *
     * @param mapa true nas rotas /api/mapa/**: dados públicos, só PUBLICADA (a equipe pode escolher outro status)
     */
    public record Escopo(Integer visualizadorId, boolean equipe, boolean mapa) {
    }

    private static final String ORIGEM = " FROM ocorrencias o JOIN tipos_ocorrencia t ON t.id = o.tipo_ocorrencia_id ";

    private final NamedParameterJdbcTemplate jdbc;

    public OcorrenciaConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OcorrenciaResponse> listar(FiltroOcorrencia filtro, Escopo escopo, int limite, long deslocamento) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = condicoes(filtro, escopo, p);
        p.addValue("limite", limite);
        p.addValue("deslocamento", deslocamento);

        String sql = "SELECT o.id, o.usuario_id, o.tipo_ocorrencia_id, t.nome AS tipo_nome, t.categoria AS tipo_categoria,"
                + " o.titulo, o.descricao, o.latitude, o.longitude, o.endereco, o.data_ocorrencia,"
                + " o.data_cadastro, o.data_atualizacao, o.status, o.relevancia, o.anonimato"
                + ORIGEM + "WHERE " + where
                + " ORDER BY o.data_cadastro DESC, o.id DESC LIMIT :limite OFFSET :deslocamento";

        return jdbc.query(sql, p, mapeadorOcorrencia(escopo));
    }

    public long contar(FiltroOcorrencia filtro, Escopo escopo) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = condicoes(filtro, escopo, p);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + ORIGEM + "WHERE " + where, p, Long.class);
        return total == null ? 0L : total;
    }

    public List<MapaOcorrenciaResponse> marcadores(FiltroOcorrencia filtro, Escopo escopo, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = condicoes(filtro, escopo, p);
        p.addValue("limite", limite);

        String sql = "SELECT o.id, o.tipo_ocorrencia_id, t.nome AS tipo_nome, o.titulo, o.latitude, o.longitude,"
                + " o.status, o.relevancia, o.data_ocorrencia"
                + ORIGEM + "WHERE " + where
                + " ORDER BY COALESCE(o.data_ocorrencia, o.data_cadastro) DESC, o.id DESC LIMIT :limite";

        return jdbc.query(sql, p, (rs, n) -> new MapaOcorrenciaResponse(
                rs.getInt("id"),
                rs.getInt("tipo_ocorrencia_id"),
                rs.getString("tipo_nome"),
                rs.getString("titulo"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                StatusOcorrencia.valueOf(rs.getString("status")),
                Relevancia.valueOf(rs.getString("relevancia")),
                rs.getObject("data_ocorrencia", OffsetDateTime.class)));
    }

    /** Agrega por célula de grade (ST_SnapToGrid). Retorna no máximo {@code limite} células, as mais densas primeiro. */
    public List<PontoCalorResponse> calor(FiltroOcorrencia filtro, Escopo escopo, double celulaGraus, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = condicoes(filtro, escopo, p);
        p.addValue("celula", celulaGraus);
        p.addValue("limite", limite);

        String sql = "SELECT ST_Y(c.centro) AS lat, ST_X(c.centro) AS lon, c.qtd FROM ("
                + " SELECT ST_SnapToGrid(CAST(o.localizacao AS geometry), :celula) AS centro, COUNT(*) AS qtd"
                + ORIGEM + "WHERE " + where
                + " GROUP BY 1) c"
                + " ORDER BY c.qtd DESC, lat, lon LIMIT :limite";

        // A intensidade (0-1) é calculada pelo serviço, que conhece o maior valor retornado.
        return jdbc.query(sql, p, (rs, n) -> new PontoCalorResponse(
                rs.getDouble("lat"), rs.getDouble("lon"), rs.getLong("qtd"), 0.0));
    }

    // ---------------------------------------------------------------------------------------------

    private String condicoes(FiltroOcorrencia f, Escopo e, MapSqlParameterSource p) {
        List<String> c = new ArrayList<>();
        c.add("o.excluida_em IS NULL");

        if (e.mapa()) {
            // Mapa público: somente PUBLICADA; GESTOR/ADMINISTRADOR podem pedir outro status explicitamente.
            StatusOcorrencia alvo = (e.equipe() && f.status() != null) ? f.status() : StatusOcorrencia.PUBLICADA;
            c.add("o.status = CAST(:status AS status_ocorrencia)");
            p.addValue("status", alvo.name());
        } else {
            if (!e.equipe()) {
                // Usuário comum: relatos publicados + os próprios (em qualquer status).
                c.add("(o.status = CAST('PUBLICADA' AS status_ocorrencia) OR o.usuario_id = :visualizadorId)");
                p.addValue("visualizadorId", e.visualizadorId());
            }
            if (f.status() != null) {
                c.add("o.status = CAST(:status AS status_ocorrencia)");
                p.addValue("status", f.status().name());
            }
            if (f.apenasMinhas()) {
                c.add("o.usuario_id = :minhasId");
                p.addValue("minhasId", e.visualizadorId());
            }
        }

        if (f.tipoId() != null) {
            c.add("o.tipo_ocorrencia_id = :tipoId");
            p.addValue("tipoId", f.tipoId());
        }
        if (f.dataInicio() != null) {
            c.add("COALESCE(o.data_ocorrencia, o.data_cadastro) >= :dataInicio");
            p.addValue("dataInicio", f.dataInicio());
        }
        if (f.dataFim() != null) {
            c.add("COALESCE(o.data_ocorrencia, o.data_cadastro) <= :dataFim");
            p.addValue("dataFim", f.dataFim());
        }
        if (f.bbox() != null) {
            BoundingBox b = f.bbox();
            c.add("ST_Intersects(o.localizacao, CAST(ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326) AS geography))");
            p.addValue("minLon", b.minLon());
            p.addValue("minLat", b.minLat());
            p.addValue("maxLon", b.maxLon());
            p.addValue("maxLat", b.maxLat());
        }
        return String.join(" AND ", c);
    }

    private RowMapper<OcorrenciaResponse> mapeadorOcorrencia(Escopo escopo) {
        return (ResultSet rs, int n) -> mapear(rs, escopo);
    }

    private OcorrenciaResponse mapear(ResultSet rs, Escopo escopo) throws SQLException {
        int autor = rs.getInt("usuario_id");
        boolean mostrarAutor = escopo.equipe()
                || (escopo.visualizadorId() != null && escopo.visualizadorId() == autor);

        return new OcorrenciaResponse(
                rs.getInt("id"),
                new OcorrenciaResponse.TipoResumo(
                        rs.getInt("tipo_ocorrencia_id"), rs.getString("tipo_nome"), rs.getString("tipo_categoria")),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getString("endereco"),
                rs.getObject("data_ocorrencia", OffsetDateTime.class),
                rs.getObject("data_cadastro", OffsetDateTime.class),
                rs.getObject("data_atualizacao", OffsetDateTime.class),
                StatusOcorrencia.valueOf(rs.getString("status")),
                Relevancia.valueOf(rs.getString("relevancia")),
                rs.getBoolean("anonimato"),
                mostrarAutor ? autor : null);
    }
}
