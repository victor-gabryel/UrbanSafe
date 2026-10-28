-- Suporte geoespacial para o mapa (issue #3). Migração incremental: não altera V1-V3 e preserva os dados.
-- Requer um PostgreSQL com os binários do PostGIS instalados (ex.: imagem postgis/postgis do compose.yaml).
-- Em bancos gerenciados em que o usuário da aplicação não pode criar extensões, execute
-- "CREATE EXTENSION postgis;" previamente com um usuário privilegiado.

CREATE EXTENSION IF NOT EXISTS postgis;

-- 1) Coluna geográfica derivada de longitude/latitude (ordem PostGIS: X = longitude, Y = latitude).
--    Coluna GERADA e ARMAZENADA: o banco a preenche para as linhas existentes agora e a mantém
--    sincronizada em todo INSERT/UPDATE de latitude/longitude, sem depender da aplicação.
--    latitude e longitude continuam sendo a fonte oficial e mantêm os CHECKs da V3.
ALTER TABLE ocorrencias
    ADD COLUMN localizacao geography(Point, 4326)
    GENERATED ALWAYS AS (
        ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4326)::geography
    ) STORED;

-- 2) Índice espacial GiST usado por ST_Intersects / ST_DWithin nas consultas do mapa.
CREATE INDEX idx_ocorrencias_localizacao_gist ON ocorrencias USING GIST (localizacao);

-- 3) Exclusão lógica: a linha, o histórico e as mídias são preservados.
ALTER TABLE ocorrencias
    ADD COLUMN excluida_em TIMESTAMPTZ,
    ADD COLUMN excluida_por INTEGER REFERENCES usuarios(id) ON DELETE RESTRICT;

ALTER TABLE ocorrencias
    ADD CONSTRAINT ck_ocorrencias_exclusao
    CHECK ((excluida_em IS NULL AND excluida_por IS NULL) OR (excluida_em IS NOT NULL AND excluida_por IS NOT NULL));

-- 4) Apoio aos filtros combinados (status + paginação por data de cadastro e período).
--    O índice idx_ocorrencias_coordenadas (latitude, longitude) da V3 é mantido por compatibilidade.
CREATE INDEX idx_ocorrencias_ativas_status_cadastro
    ON ocorrencias (status, data_cadastro DESC, id DESC)
    WHERE excluida_em IS NULL;

CREATE INDEX idx_ocorrencias_ativas_data_referencia
    ON ocorrencias ((COALESCE(data_ocorrencia, data_cadastro)))
    WHERE excluida_em IS NULL;

CREATE INDEX idx_ocorrencias_excluida_por ON ocorrencias (excluida_por) WHERE excluida_por IS NOT NULL;
