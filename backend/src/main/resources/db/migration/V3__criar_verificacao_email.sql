CREATE TABLE codigos_verificacao_email (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    codigo_hash VARCHAR(100) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_codigo_verificacao_usuario
    ON codigos_verificacao_email(usuario_id);