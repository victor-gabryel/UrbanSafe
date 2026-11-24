# UrbanSafe

Plataforma de segurança urbana: **backend** (Java 21 · Spring Boot · JWT · PostgreSQL/PostGIS · Flyway) e
**app mobile** (React Native · Expo SDK 58 · Expo Router · TypeScript · Axios).

```
UrbanSafe/
├── backend/          API REST + banco (Docker) — roda no seu PC Windows
└── frontend-mobile/  App Expo — roda no celular via Expo Go
```

PC e celular precisam estar **na mesma rede Wi-Fi**.

## Pré-requisitos (Windows)

Java 21 · Docker Desktop (aberto) · Node.js 22.13 ou superior (exigido pelo SDK 58) · app **Expo Go** atualizado no celular (versão com suporte ao SDK 58).

---

## TERMINAL 1 — Backend

### 1. Configurar o `.env`
```powershell
cd UrbanSafe\backend
copy .env.example .env
```
Edite `.env` e preencha no mínimo:
- `POSTGRES_PASSWORD` e `DB_PASSWORD` (a mesma senha nos dois);
- `JWT_SECRET` — gere com `openssl rand -base64 48` (ou qualquer texto aleatório com 32+ caracteres);
- `MAIL_*` e `MAIL_FROM` (SMTP real, veja a nota sobre e-mail abaixo).

### 2. Subir o banco PostgreSQL/PostGIS
```powershell
docker compose up -d db
docker compose ps
```
As migrations do Flyway (V1–V6, incluindo PostGIS e os tipos de ocorrência iniciais) rodam sozinhas quando o backend inicia.

### 3. Iniciar o backend
Na mesma janela:
```powershell
. .\load-env.ps1
.\mvnw.cmd spring-boot:run
```
Teste no PC: http://localhost:8080/swagger-ui.html

### 4. Liberar a porta no firewall (uma vez, PowerShell como Administrador)
```powershell
New-NetFirewallRule -DisplayName "UrbanSafe API 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private
```
Se o Wi-Fi estiver marcado como "Público" no Windows, troque para "Particular" ou use `-Profile Any`.

---

## TERMINAL 2 — App Expo

### 1. Descobrir o IPv4 do PC
```powershell
ipconfig
```
Procure o adaptador **Wi-Fi** e copie o "Endereço IPv4" (ex.: `192.168.0.100`).

### 2. Configurar o endereço da API
```powershell
cd UrbanSafe\frontend-mobile
copy .env.example .env
```
Edite `.env`:
```
EXPO_PUBLIC_API_URL=http://192.168.0.100:8080
```
Use o **seu** IPv4. Nunca use `localhost` (no celular, `localhost` é o próprio celular).
O endereço é lido em um único arquivo (`src/config/api.ts`). Se `EXPO_PUBLIC_API_URL` ficar vazio, o app tenta usar o IP do PC que serve o Expo, na porta 8080.

### 3. Instalar dependências e alinhar versões ao SDK 58
```powershell
npm install
npx expo install --fix
npx expo-doctor
```
`expo install --fix` é **obrigatório na primeira vez**: ele grava as versões exatas de cada biblioteca para o SDK 58 (veja "Limitações" no fim).

### 4. Iniciar o Expo
```powershell
npx expo start --clear
```
Depois de mudar o `.env`, sempre reinicie com `--clear`.

### 5. Abrir no celular
- **Android:** abra o Expo Go → "Scan QR code" → leia o QR do terminal.
- **iPhone:** abra a **Câmera**, aponte para o QR e toque na notificação do Expo Go.
- Se o QR não conectar: `npx expo start --tunnel` (exige instalar `@expo/ngrok`) — mas a API continua precisando do IP local.

---

## Testando a conexão
No navegador **do celular**, abra `http://SEU_IP:8080/swagger-ui.html`. Se a página abrir, o app também conecta.

## Problemas de conexão
| Sintoma | Causa provável / solução |
|---|---|
| "Não foi possível conectar ao servidor (http://…)" | Backend desligado, IP errado ou firewall. Teste o link acima no celular. |
| Funciona no PC mas não no celular | Firewall (passo 4 do Terminal 1) ou rede pública/isolamento de clientes no roteador (Wi-Fi de visitantes). |
| IP mudou | O roteador trocou o IPv4. Rode `ipconfig` de novo, atualize o `.env` e reinicie com `--clear`. |
| Celular em 4G | Conecte ao mesmo Wi-Fi do PC. |
| Backend não sobe | `JWT_SECRET` vazio/curto, `DB_PASSWORD` diferente de `POSTGRES_PASSWORD`, Docker fechado, ou `MAIL_HOST` ausente. |
| Mapa vazio | Ainda não há ocorrências **PUBLICADAS** na área. Aproxime o mapa (máx. ~4,5° de lado) e confira filtros. |

---

## Funcionalidades do app e endpoints usados
| Tela / ação | Endpoint real do backend | Acesso |
|---|---|---|
| Mapa, lista, filtros, busca por tipo | `GET /api/mapa/ocorrencias`, `GET /api/tipos-ocorrencia` | **público** (visitante) |
| Mapa de calor | `GET /api/mapa/calor` | **público** |
| Cadastro | `POST /api/auth/cadastro` | público |
| Verificar e-mail / reenviar | `POST /api/auth/verificar-email`, `/reenviar-verificacao` | público |
| Login / refresh / logout | `POST /api/auth/login`, `/refresh`, `/logout` | público |
| Esqueci minha senha | `POST /api/auth/recuperar-senha`, `/redefinir-senha` | público |
| Perfil / editar nome | `GET /api/auth/me`, `PATCH /api/usuarios/me` | JWT |
| Denunciar | `POST /api/ocorrencias` | JWT |
| Anexar foto | `POST /api/ocorrencias/{id}/midias` (multipart) | JWT |
| Minhas ocorrências | `GET /api/ocorrencias?minhas=true` | JWT |

Tokens ficam no `expo-secure-store`; o Axios renova o access token automaticamente via refresh (uma renovação para várias requisições simultâneas) e encerra a sessão se o refresh falhar.

### Mudança feita no backend para o modo visitante
Antes, **todo** `/api/mapa/**` exigia JWT, o que impedia a navegação anônima. Agora são públicos **somente** `GET /api/mapa/ocorrencias`, `GET /api/mapa/calor` e `GET /api/tipos-ocorrencia`; eles só retornam ocorrências `PUBLICADAS` e nenhum dado pessoal. Um visitante não consegue pedir outro status (403). Todo o resto continua autenticado, e perfis USUARIO/GESTOR/ADMINISTRADOR foram mantidos.

## Perfis Gestor e Administrador

O app tem três áreas, todas com a mesma identidade visual (cores, cabeçalho azul, cartões, botões e barra de abas do Usuário).
Após o login o app abre a área do perfil da conta: `USUARIO` → abas atuais; `GESTOR` → `/gestor`; `ADMINISTRADOR` → `/admin`.
Visitante e Usuário continuam exatamente como antes; Gestor/Admin que abrirem `/(tabs)` são redirecionados para a própria área.

**Como acessar:** não há cadastro de perfil privilegiado. Entre com uma conta existente que tenha o perfil (o 1º administrador precisa ser definido no banco:
`UPDATE usuarios SET perfil_id = (SELECT id FROM perfis WHERE nome = 'ADMINISTRADOR') WHERE email = 'seu@email.com';`). Depois o administrador promove outros pelo app (aba Usuários). A mudança vale a partir do próximo login.

### Gestor (`/gestor`) — moderação
| Tela | Função | Endpoint |
|---|---|---|
| Painel | Contadores por situação (pendente/publicada/oculta/rejeitada) e atalhos | `GET /api/admin/ocorrencias?status=…&limit=1` |
| Fila | Ocorrências pendentes, filtro por tipo, paginação | `GET /api/admin/ocorrencias/pendentes` |
| Ocorrências | Todas, filtros de status e tipo | `GET /api/admin/ocorrencias` |
| Detalhe (`/moderar/[id]`) | Descrição, autor (id), fotos, relevância, aprovar / rejeitar / solicitar correção / ocultar, decisões e histórico | `GET /api/ocorrencias/{id}`, `/midias`, `/historico`, `GET`/`POST /api/ocorrencias/{id}/moderacao`, `PATCH /api/ocorrencias/{id}` (relevância) |
| Mapa | Mesmo mapa/mapa de calor do Usuário | `GET /api/mapa/*` |
| Perfil | Mesmo perfil do Usuário (nome, sair) | `GET /api/auth/me`, `PATCH /api/usuarios/me` |

As ações exibidas seguem as transições do backend (`AcaoModeracao`); justificativa obrigatória, exceto ao aprovar. O backend impede moderar a própria ocorrência.

### Administrador (`/admin`) — tudo do Gestor (exceto a aba Fila, coberta por Ocorrências ▸ Pendentes) mais:
| Tela | Função | Endpoint |
|---|---|---|
| Usuários | Listar, bloquear/reativar, alterar perfil (com confirmação) | `GET /api/admin/usuarios`, `PATCH /api/admin/usuarios/{id}/status`, `/perfil` |
| Tipos | Criar, editar, desativar/reativar tipos | `POST`/`PATCH`/`DELETE /api/tipos-ocorrencia` |
| Auditoria (pelo painel) | Registros administrativos, mais recentes primeiro | `GET /api/admin/auditoria` |

### Dependem do backend / não implementadas (nada foi simulado)
- Não existem no backend: `/api/admin/dashboard`, estatísticas, configurações e denúncias de conteúdo (previstos no escopo). O painel usa só os totais das listagens existentes.
- `GET /api/tipos-ocorrencia` lista **apenas tipos ativos**; não há como listar os inativos. O app só oferece "Reativar" para tipos desativados na sessão atual.
- Não há busca/filtro de usuários nem de auditoria no backend (somente paginação).
- O backend não devolve nome do autor nem do moderador, apenas o id.
- Vídeos/documentos anexados são listados, mas só imagens são exibidas no app.
- Gestor/Admin não usam as abas "Denunciar" e "Rotas" do Usuário.

## Limitações conhecidas (sem dados fictícios)
- **Rotas:** o backend não tem módulo de rotas. A aba "Rotas"/"Calcular rota" apenas abre o trajeto no app de mapas do celular (geocodificação do sistema); não calcula rotas "seguras".
- **Login por "usuário":** o backend autentica somente por **e-mail**. Não existem as "contas de demonstração" (admin/gestor) do protótipo do Figma.
- **Verificação de e-mail:** a conta só entra após verificar o e-mail. O backend envia um link `FRONTEND_URL/verificar-email?token=…`; no app, cole só o código (após `token=`) na tela "Verificar e-mail". Sem SMTP real configurado o e-mail não chega. **Apenas para desenvolvimento**, você pode liberar a conta pelo banco:
  ```powershell
  docker exec -it backend-db psql -U urbansafe -d urbansafe -c "UPDATE usuarios SET email_verificado = true WHERE email = 'seu@email.com';"
  ```
- **Promover perfis:** pela aba Usuários da área do Administrador (`PATCH /api/admin/usuarios/{id}/perfil`).
- **Mapa de calor:** desenhado com círculos sobre o mapa (funciona igual em Android e iOS; o componente `Heatmap` nativo só existe no Google Maps).
- **Fotos:** o backend só aceita mídia enquanto a ocorrência está `PENDENTE`. Tipos que não exigem revisão nascem `PUBLICADA` e a foto é recusada (o app avisa).
- Pastas `android/` e `ios/` foram mantidas, mas foram geradas para o SDK 57 e **não são usadas pelo Expo Go**. Se for fazer um Development Build, rode antes `npx expo prebuild --clean`.

## Testes do backend
```powershell
cd backend
.\mvnw.cmd test
```
Os testes de integração com PostGIS (`OcorrenciasPostgisIT`) só rodam com um banco **descartável** e variáveis `URBANSAFE_IT_DB_URL`, `URBANSAFE_IT_DB_USER`, `URBANSAFE_IT_DB_PASSWORD` (veja o cabeçalho da classe).
