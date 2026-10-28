# UrbanSafe — Ocorrências, mapa e moderação (exemplos)

Base: `http://localhost:8080`. Todas as rotas exigem `Authorization: Bearer {{accessToken}}` (obtido em `POST /api/auth/login`).
Erros seguem o formato padrão do projeto (`timestamp`, `status`, `erro`, `mensagem`, `caminho`, `campos`).

## Tipos de ocorrência
- `GET /api/tipos-ocorrencia` → lista ativos. `GET /api/tipos-ocorrencia/{id}`.
- `POST /api/tipos-ocorrencia` (ADMINISTRADOR) `{ "nome": "Vandalismo", "categoria": "Patrimônio", "exigeRevisao": true }` → **201**; nome repetido (sem diferenciar caixa) → **409**.
- `PATCH /api/tipos-ocorrencia/{id}` (ADMINISTRADOR) `{ "categoria": "Outra" }` — campos ausentes não mudam; `{ "ativo": true }` reativa.
- `DELETE /api/tipos-ocorrencia/{id}` (ADMINISTRADOR) → **204**, desativação lógica.

## Ocorrências
**Criar** `POST /api/ocorrencias`
```json
{ "tipoOcorrenciaId": 1, "titulo": "Celular furtado no ponto", "descricao": "Descrição objetiva.",
  "latitude": -8.0476, "longitude": -34.8770, "endereco": "Av. Exemplo", "dataOcorrencia": "2026-10-20T18:30:00-03:00", "anonimato": false }
```
**201** (`status` = `PENDENTE`, `autorId` = usuário do JWT; `usuarioId`/`status` enviados no corpo são ignorados).
Latitude fora de −90..90 ou longitude fora de −180..180 → **400** com `campos`.

**Listar** `GET /api/ocorrencias?bbox=-34.95,-8.10,-34.85,-8.00&tipo=1&status=PUBLICADA&dataInicio=2026-10-01T00:00:00Z&dataFim=2026-10-31T23:59:59Z&page=1&limit=20&minhas=false`
```json
{ "itens": [ { "id": 42, "tipo": {"id":1,"nome":"Furto","categoria":"Patrimônio"}, "titulo": "...", "latitude": -8.0476, "longitude": -34.877,
               "status": "PUBLICADA", "relevancia": "MEDIA", "anonimato": false, "autorId": null } ],
  "pagina": 1, "limite": 20, "totalItens": 1, "totalPaginas": 1 }
```
`autorId` só aparece para o autor, GESTOR e ADMINISTRADOR.

**Editar** `PATCH /api/ocorrencias/{id}` `{ "titulo": "Novo título" }` (autor, `PENDENTE`); **Excluir** `DELETE` → **204** (lógico).

## Moderação (GESTOR/ADMINISTRADOR)
`POST /api/ocorrencias/{id}/moderacao` `{ "acao": "REJEITAR", "justificativa": "Sem dados suficientes." }` → **201**
```json
{ "id": 1, "ocorrenciaId": 42, "moderadorId": 20, "acao": "REJEITAR", "statusAnterior": "PENDENTE", "statusNovo": "REJEITADA",
  "justificativa": "Sem dados suficientes.", "dataModeracao": "2026-10-27T12:00:00Z" }
```
Sem justificativa onde obrigatória → **400**; transição inválida → **409**; usuário comum → **403**.
`GET /api/ocorrencias/{id}/moderacao` lista as decisões.

## Mídias
`POST /api/ocorrencias/{id}/midias` (multipart: `arquivo`, `descricao` opcional) → **201** | **415** formato não permitido | **413** acima do limite | **409** ocorrência não `PENDENTE`.
`GET /api/ocorrencias/{id}/midias`; `GET .../midias/{midiaId}/arquivo` (download autenticado); `DELETE .../midias/{midiaId}` → **204**.

## Histórico
`GET /api/ocorrencias/{id}/historico` → `[ { "acao": "CRIACAO", "statusNovo": "PENDENTE", "descricao": "Ocorrência registrada.", "dataEvento": "...", "usuarioId": 10 } ]`
Ações: `CRIACAO`, `ATUALIZACAO`, `MUDANCA_STATUS` (reservada), `MODERACAO`, `EXCLUSAO`, `MIDIA_ADICIONADA`, `MIDIA_REMOVIDA`.

## Mapa
`GET /api/mapa/ocorrencias?bbox=...&tipo=&dataInicio=&dataFim=&limit=500` → `{ "itens": [ {"id","tipoId","tipoNome","titulo","latitude","longitude","status","relevancia","dataOcorrencia"} ], "limite": 500, "truncado": false }`

`GET /api/mapa/calor?bbox=...&celula=0.005`
```json
{ "celulaGraus": 0.005, "totalOcorrencias": 17, "truncado": false,
  "pontos": [ { "latitude": -8.045, "longitude": -34.875, "quantidade": 9, "intensidade": 1.0 } ],
  "aviso": "A densidade reflete apenas os relatos registrados ... não representa a probabilidade real de crime ..." }
```

## Administração (ADMINISTRADOR)
`PATCH /api/admin/usuarios/{id}/perfil` `{ "perfil": "GESTOR" }` · `GET /api/admin/auditoria?page=1&limit=20`
