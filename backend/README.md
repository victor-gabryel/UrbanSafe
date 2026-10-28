# UrbanSafe — Backend

## Primeira inicialização

### Pré-requisitos

- Docker Desktop instalado e em execução.
- Java 21 instalado.
- Arquivo `.env` configurado na pasta `backend`.
- Script `load-env.ps1` presente na pasta `backend`.

### 1. Inicie o banco de dados

Abra o PowerShell na pasta `backend` e execute:

```powershell
docker compose up -d db
```

Esse comando inicia apenas o banco de dados PostgreSQL/PostGIS em um contêiner Docker.

Para verificar se o contêiner está em execução:

```powershell
docker compose ps
```

### 2. Carregue as variáveis do `.env`

Na mesma janela do PowerShell, execute:

```powershell
. .\load-env.ps1
```

O comando carrega as variáveis do arquivo `.env` na sessão atual do PowerShell, permitindo que o Spring Boot as utilize ao iniciar.

**Importante:** execute esse comando na mesma janela do PowerShell em que você iniciará o backend. Se abrir outra janela ou reiniciar o terminal, carregue as variáveis novamente.

### 3. Inicie o backend

Ainda na mesma janela do PowerShell, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

O backend será iniciado usando as variáveis carregadas do `.env`.

Se preferir iniciar pelo Spring Boot Dashboard do VS Code, configure as variáveis de ambiente na configuração de execução do VS Code. As variáveis carregadas em outro terminal não são automaticamente compartilhadas com o Dashboard.

### Observações

- Quando o backend roda diretamente no Windows, `DB_URL` normalmente deve usar `localhost`, por exemplo: `jdbc:postgresql://localhost:5432/urbansafe`.
- Quando o backend roda dentro do Docker Compose, a URL deve usar o nome do serviço do banco, por exemplo: `jdbc:postgresql://db:5432/urbansafe`.
- Não compartilhe nem versione o arquivo `.env`; ele pode conter senhas. Mantenha-o no `.gitignore`.

## Variáveis de ambiente

Copie `.env.example` para `.env` e preencha. Obrigatórias: `DB_PASSWORD`, `MAIL_*`, `MAIL_FROM` e **`JWT_SECRET`** (mínimo 32 bytes; gere com `openssl rand -base64 48`). A aplicação não sobe sem `JWT_SECRET`.

Opcionais: `JWT_ACCESS_EXPIRATION_MINUTES` (15), `JWT_REFRESH_EXPIRATION_DAYS` (7), `PASSWORD_RESET_EXPIRATION_MINUTES` (60), `FRONTEND_URL` (também define a origem liberada no CORS), `LOG_LEVEL_SECURITY` (INFO).

Verificação de e-mail: `EMAIL_VERIFICATION_EXPIRATION_HOURS` (24), `EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS` (60), `EMAIL_VERIFICATION_RESEND_MAX_PER_WINDOW` (5) e `EMAIL_VERIFICATION_RESEND_WINDOW_MINUTES` (15). SMTP: `MAIL_STARTTLS` (true) e `MAIL_TIMEOUT_MS` (10000). O link enviado usa `FRONTEND_URL` + `/verificar-email?token=...`.

Swagger: `http://localhost:8080/swagger-ui.html`. Use `SWAGGER_ENABLED=false` em produção. Atrás de proxy reverso, configure `server.forward-headers-strategy` para o limite de reenvio enxergar o IP real.

## API de autenticação

Exemplos de requisição/resposta para o Postman: [`docs/API-AUTH.md`](docs/API-AUTH.md).

## Testes

```powershell
.\mvnw.cmd test
```

`BackendApplicationTests` (contextLoads) sobe o contexto completo e precisa do PostgreSQL e das variáveis do `.env` carregadas.

## Banco de dados: PostgreSQL + PostGIS

O `compose.yaml` usa a imagem `postgis/postgis:16-3.4`. Para banco próprio, instale o PostGIS e garanta que o
usuário da aplicação possa executar `CREATE EXTENSION postgis;` (ou crie a extensão antes com um usuário privilegiado).

Migrations Flyway (executadas automaticamente ao iniciar a aplicação; **nunca edite uma já aplicada**):

| Migration | Conteúdo |
|---|---|
| V1–V3 | Usuários/autenticação, verificação de e-mail, tipos e ocorrências (já existentes) |
| V4 | `CREATE EXTENSION postgis`; coluna `ocorrencias.localizacao geography(Point,4326)` **gerada** a partir de `longitude`/`latitude` (ordem PostGIS: X=longitude, Y=latitude); índice **GiST** `idx_ocorrencias_localizacao_gist`; exclusão lógica (`excluida_em`, `excluida_por`); índices parciais por status/data |
| V5 | `midias_ocorrencia`, `historico_ocorrencia`, `moderacoes` (histórico e moderação **imutáveis**: gatilhos bloqueiam UPDATE/DELETE) |
| V6 | `auditoria_administrativa` (imutável) |

Segurança da migração: V4 é incremental. As linhas existentes recebem `localizacao` automaticamente (coluna gerada),
`latitude`/`longitude` continuam como fonte oficial com seus `CHECK`s (−90..90 e −180..180) e nada é apagado.
Em banco novo, V1→V6 rodam em sequência. As migrations rodam automaticamente na inicialização da aplicação (não há comando separado).

## Perfis e autorização

Existem **exatamente três** perfis funcionais, aplicados no backend (Spring Security + JWT) e revalidados nos serviços:

| Perfil funcional | Nome no banco/JWT | Observação |
|---|---|---|
| Usuário comum | `USUARIO` | `USUARIO_COMUM` é aceito como sinônimo apenas na entrada de `PATCH /api/admin/usuarios/{id}/perfil`; o nome persistido continua `USUARIO` (não há quarto perfil) |
| Gestor | `GESTOR` | analisa e modera |
| Administrador | `ADMINISTRADOR` | gerencia perfis, tipos e auditoria |

Não há conta administrativa criada por migration (nenhuma credencial fixa). Para o **primeiro administrador**: cadastre
e verifique a conta normalmente e promova-a uma única vez por SQL:

```sql
UPDATE usuarios SET perfil_id = (SELECT id FROM perfis WHERE nome = 'ADMINISTRADOR') WHERE email = 'seu-email@exemplo.com';
```

Depois disso, o administrador promove gestores pela API. Ninguém altera o próprio perfil.

Regras principais: `401` sem JWT válido; `403` autenticado sem permissão. Relatos não publicados de outra pessoa respondem `404`.

| Ação | USUARIO | GESTOR | ADMINISTRADOR |
|---|---|---|---|
| Registrar ocorrência, consultar publicadas/mapa/calor | sim | sim | sim |
| Ver ocorrências não publicadas | só as próprias | todas | todas |
| Editar conteúdo do relato | só o próprio, enquanto `PENDENTE` | só o próprio, enquanto `PENDENTE` | só o próprio, enquanto `PENDENTE` |
| Corrigir tipo/relevância de relato alheio | não | sim | sim |
| Excluir (lógico) ocorrência | só a própria, `PENDENTE`/`REJEITADA` | só a própria (idem) | qualquer |
| Adicionar mídia | só autor, `PENDENTE` | só autor, `PENDENTE` | só autor, `PENDENTE` |
| Remover mídia | só autor, `PENDENTE` | qualquer | qualquer |
| Histórico da ocorrência | só autor | sim | sim |
| Moderar / consultar moderação | não | sim (não a própria) | sim (não a própria) |
| Criar/editar/desativar tipos | não | não | sim |
| Alterar perfil de outro usuário, ver auditoria | não | não | sim |

## Endpoints da issue #3

Detalhes e exemplos em [`docs/API-OCORRENCIAS.md`](docs/API-OCORRENCIAS.md) e no Swagger.

| Método | Rota | Perfis |
|---|---|---|
| GET | `/api/tipos-ocorrencia`, `/api/tipos-ocorrencia/{id}` | autenticados |
| POST / PATCH / DELETE | `/api/tipos-ocorrencia[/{id}]` | ADMINISTRADOR |
| GET | `/api/ocorrencias` (`page`, `limit`, `bbox`, `tipo`, `dataInicio`, `dataFim`, `status`, `minhas`) | autenticados |
| POST | `/api/ocorrencias` | autenticados |
| GET / PATCH / DELETE | `/api/ocorrencias/{id}` | autenticados (regras acima) |
| GET | `/api/ocorrencias/{id}/historico` | autor, GESTOR, ADMINISTRADOR |
| GET / POST | `/api/ocorrencias/{id}/midias` | autenticados / autor |
| DELETE | `/api/ocorrencias/{id}/midias/{midiaId}` | autor (PENDENTE), GESTOR, ADMINISTRADOR |
| GET | `/api/ocorrencias/{id}/midias/{midiaId}/arquivo` | conforme visibilidade da ocorrência |
| POST / GET | `/api/ocorrencias/{id}/moderacao` | GESTOR, ADMINISTRADOR |
| GET | `/api/mapa/ocorrencias`, `/api/mapa/calor` | autenticados |
| PATCH | `/api/admin/usuarios/{id}/perfil` | ADMINISTRADOR |
| GET | `/api/admin/auditoria` | ADMINISTRADOR |

Exemplo: `GET /api/ocorrencias?bbox=-34.95,-8.10,-34.85,-8.00&tipo=2&status=PUBLICADA&dataInicio=2026-10-01T00:00:00Z&page=1&limit=20`
(`bbox` = `minLon,minLat,maxLon,maxLat`; `+` em datas deve ser enviado como `%2B`, ou use `Z`).

### Decisões e lacunas dos requisitos (documentadas)

- **Fluxo de moderação** (o documento só define status e ações): `APROVAR` (PENDENTE/OCULTA/REJEITADA→PUBLICADA), `REJEITAR`
  (PENDENTE/PUBLICADA/OCULTA→REJEITADA), `SOLICITAR_CORRECAO` (PENDENTE/PUBLICADA→PENDENTE), `OCULTAR_TEMPORARIAMENTE`
  (PENDENTE/PUBLICADA→OCULTA). Justificativa obrigatória exceto em `APROVAR`. O moderador não modera a própria ocorrência.
- **Status inicial**: `PENDENTE` quando o tipo tem `exige_revisao = true` (todos os tipos do catálogo inicial); caso contrário `PUBLICADA`.
- **Mapa e mapa de calor** exigem JWT (nenhuma rota de dados é pública) e só retornam `PUBLICADA`.
- **Filtro de período**: `dataInicio`/`dataFim` (ISO-8601) sobre `COALESCE(data_ocorrencia, data_cadastro)`; o documento não fixa o formato de `periodo`.
- **Paginação**: `page` começa em 1; `limit` de 1 a 100.
- **Mapa de calor**: grade (`ST_SnapToGrid`) com `celula` em graus (0,0005–0,05; padrão 0,005 ≈ 550 m), no máximo 2000 células; a resposta traz o aviso de que densidade não é probabilidade de crime.
- **Limites do bbox**: lado máximo de 5° (configurável); não há suporte a áreas que cruzam o antimeridiano.
- **Histórico**: acrescentada a coluna `acao` (tipo do evento); `data_evento` é a data oficial (sem `criado_em`). `moderacoes` ganhou `status_anterior`/`status_novo`.
- **Mídias**: formatos detectados pelo conteúdo — JPEG, PNG, WebP (≤10 MB), MP4 (≤50 MB), MP3/WAV (≤10 MB), PDF (≤10 MB); até 10 por ocorrência. Armazenamento em disco (`STORAGE_PATH`) atrás da interface `ArmazenamentoArquivos` (trocável por S3-compatível). Remoção é lógica e o arquivo é mantido (retenção/auditoria). Como não há varredura antivírus, PDFs/vídeos devem ser tratados como não confiáveis pelos clientes.
- **APIs externas**: a issue #3 não exige integração externa no backend (mapa base, geocodificação e rotas ficam no aplicativo/etapas futuras). **Nada foi integrado**; ao adotar Nominatim/OSRM, confirme os termos de uso e os modos de transporte (caminhada/bicicleta) antes de declarar suporte.

## Variáveis de ambiente adicionais (todas opcionais)

`OCORRENCIAS_LIMITE_MAXIMO` (100), `MAPA_MAX_RESULTADOS` (500), `MAPA_MAX_LADO_BBOX_GRAUS` (5.0), `MAPA_CALOR_MAX_CELULAS` (2000),
`MAPA_CALOR_CELULA_MIN_GRAUS`/`_MAX_GRAUS`/`_PADRAO_GRAUS`, `STORAGE_PATH` (`./storage`), `MIDIAS_MAX_POR_OCORRENCIA` (10),
`MIDIAS_MAX_BYTES_IMAGEM|VIDEO|AUDIO|DOCUMENTO`, `UPLOAD_MAX_FILE_SIZE` (50MB), `UPLOAD_MAX_REQUEST_SIZE` (55MB).

## Swagger com JWT

1. Abra `http://localhost:8080/swagger-ui.html`. 2. Faça login em `POST /api/auth/login` e copie o `accessToken`.
3. Clique em **Authorize** e cole o token (sem o prefixo `Bearer`). Cada operação descreve os perfis autorizados e os códigos HTTP.
O Swagger continua sendo a única documentação pública; todas as rotas de dados exigem JWT.

## Testes (ampliado)

- `.\mvnw.cmd test` executa os testes unitários (serviços, validadores, bbox, fluxo de moderação, mídias, perfis).
- `OcorrenciasPostgisIT` (integração com PostgreSQL/PostGIS real, matriz 401/403 por perfil, bbox, mapa, calor, mídias, histórico imutável)
  só roda se `URBANSAFE_IT_DB_URL` estiver definida (usa um banco **descartável**, p.ex. subido com `docker compose up -d db`):

```powershell
$env:URBANSAFE_IT_DB_URL="jdbc:postgresql://localhost:5432/urbansafe_it"
$env:URBANSAFE_IT_DB_USER="urbansafe"; $env:URBANSAFE_IT_DB_PASSWORD="sua-senha"
.\mvnw.cmd test -Dtest=OcorrenciasPostgisIT
```

## Limitações conhecidas

Sem varredura antivírus de arquivos; armazenamento local em disco por padrão; gestão de usuários limitada à troca de perfil
(listar/bloquear usuários e demais rotas `/api/admin/**` do documento ficam para etapas futuras); denúncias, confirmações, rotas,
favoritos e notificações (listados no documento) não fazem parte da issue #3.
