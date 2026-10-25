-- Reforço da verificação de e-mail (issue #3). Não altera a V1.

-- 1) Normaliza dados existentes antes de criar a regra: tokens pendentes de contas já verificadas
--    e tokens pendentes antigos (mantém só o mais recente por usuário) passam a "utilizados".
UPDATE tokens_verificacao_email t
   SET utilizado_em = CURRENT_TIMESTAMP
 WHERE t.utilizado_em IS NULL
   AND (
        EXISTS (SELECT 1 FROM usuarios u WHERE u.id = t.usuario_id AND u.email_verificado)
        OR t.id <> (SELECT MAX(x.id)
                      FROM tokens_verificacao_email x
                     WHERE x.usuario_id = t.usuario_id
                       AND x.utilizado_em IS NULL)
   );

-- 2) No máximo um token pendente por usuário (novo envio invalida o anterior na aplicação).
CREATE UNIQUE INDEX uq_tokens_verificacao_pendente_usuario
    ON tokens_verificacao_email (usuario_id)
    WHERE utilizado_em IS NULL;

-- 3) Apoio à limpeza de expirados e ao intervalo mínimo entre reenvios.
CREATE INDEX idx_tokens_verificacao_expira_em
    ON tokens_verificacao_email (expira_em);

CREATE INDEX idx_tokens_verificacao_usuario_criado_em
    ON tokens_verificacao_email (usuario_id, criado_em DESC);

-- 4) Integridade do conteúdo: hash SHA-256 em hexadecimal minúsculo e validade posterior à criação.
ALTER TABLE tokens_verificacao_email
    ADD CONSTRAINT ck_tokens_verificacao_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_tokens_verificacao_validade CHECK (expira_em > criado_em);

-- A FK usuario_id -> usuarios(id) permanece sem CASCADE (ON DELETE NO ACTION): contas são
-- desativadas (ativo = false), não excluídas, preservando registros de auditoria.
