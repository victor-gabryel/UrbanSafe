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

## API de autenticação

Exemplos de requisição/resposta para o Postman: [`docs/API-AUTH.md`](docs/API-AUTH.md).

## Testes

```powershell
.\mvnw.cmd test
```

`BackendApplicationTests` (contextLoads) sobe o contexto completo e precisa do PostgreSQL e das variáveis do `.env` carregadas.
