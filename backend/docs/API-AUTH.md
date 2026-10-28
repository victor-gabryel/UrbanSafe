# UrbanSafe — Autenticação e Usuários (exemplos para Postman)



Base URL local: `http://localhost:8080`. Rotas protegidas exigem `Authorization: Bearer {{accessToken}}`.

Todo erro segue o formato:



```json

{ "timestamp": "2026-10-21T15:00:00Z", "status": 400, "erro": "Bad Request",

  "mensagem": "Dados inválidos.", "caminho": "/api/auth/cadastro",

  "campos": [ { "campo": "email", "mensagem": "Informe um e-mail válido" } ] }

```



Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs` (desligue em produção com `SWAGGER_ENABLED=false`).



## Fluxo de teste (ordem)

1. Cadastro → 2. Verificar e-mail (token chega por e-mail) → 3. Login → 4. `GET /api/auth/me` →

5. `PATCH /api/usuarios/me` → 6. Refresh → 7. Logout → 8. Recuperar/redefinir senha → 9. `DELETE /api/usuarios/me`.



---

### 1. Cadastro — `POST /api/auth/cadastro` (público)

Headers: `Content-Type: application/json`

```json

{ "nome": "Maria Silva", "email": "maria@exemplo.com", "senha": "SenhaForte123" }

```

**201**

```json

{ "id": 1, "nome": "Maria Silva", "email": "maria@exemplo.com", "emailVerificado": false, "dataCadastro": "2026-10-21T15:00:00Z" }

```

**409** e-mail repetido: `"mensagem": "Este e-mail já está cadastrado."` · **400** dados inválidos (lista em `campos`).

Qualquer campo `perfil` enviado é ignorado: o papel é sempre `USUARIO`.



### 2. Verificar e-mail — `POST /api/auth/verificar-email` (público)

```json

{ "token": "TOKEN_RECEBIDO_POR_EMAIL" }

```

**200** (texto puro, contrato anterior preservado): `E-mail verificado com sucesso.` · **400** token inválido/usado/expirado.



### 2b. Reenviar verificação — `POST /api/auth/reenviar-verificacao` (público)

```json

{ "email": "maria@exemplo.com" }

```

**202** (sempre, exista ou não o e-mail, esteja ou não verificado):

`{ "mensagem": "Se o e-mail estiver cadastrado e pendente de verificação, enviaremos um novo link." }`

**400** e-mail em formato inválido · **429** limite por IP excedido (padrão 5 pedidos/15 min).

Cada conta só recebe um novo e-mail após o intervalo mínimo (padrão 60 s; dentro dele a resposta continua 202, sem e-mail).

Emitir um novo link invalida o anterior (só existe um token pendente por conta). O link vale 24 h (configurável).



### 3. Login — `POST /api/auth/login` (público)

```json

{ "email": "maria@exemplo.com", "senha": "SenhaForte123" }

```

**200**

```json

{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900, "refreshToken": "q1w2e3...",

  "usuario": { "id": 1, "nome": "Maria Silva", "email": "maria@exemplo.com", "perfil": "USUARIO",

               "emailVerificado": true, "dataCadastro": "2026-10-21T15:00:00Z" } }

```

**401** `"E-mail ou senha inválidos."` (mesma mensagem para e-mail inexistente ou senha errada) ·

**403** `"Esta conta está desativada."` ou `"E-mail ainda não verificado..."` (só após senha correta).



### 4. Usuário autenticado — `GET /api/auth/me` e `GET /api/usuarios/me`

Header `Authorization: Bearer {{accessToken}}` → **200** objeto `usuario` acima.

Sem header → **401** `"Autenticação necessária. Informe um token de acesso válido."`;

token expirado → **401** `"Token de acesso expirado."`; adulterado → **401** `"Token de acesso inválido."`.



### 5. Atualizar perfil — `PATCH /api/usuarios/me`

```json

{ "nome": "Maria S. Oliveira" }

```

**200** objeto `usuario` atualizado · **400** nome vazio. Apenas `nome` é editável (e-mail, perfil, ativo são ignorados).



### 5b. Consultar por id — `GET /api/usuarios/{id}`

Próprio id ou perfil `ADMINISTRADOR` → **200**; outro usuário sem ser admin → **403**; id inexistente (admin) → **404**.



### 6. Refresh — `POST /api/auth/refresh` (público)

```json

{ "refreshToken": "q1w2e3..." }

```

**200** mesmo formato do login, com **novo** `refreshToken` (o anterior é revogado).

**401** `"Refresh token inválido, expirado ou revogado."`. Reapresentar um token já rotacionado revoga todas as sessões do usuário.



### 7. Logout — `POST /api/auth/logout` (público, exige o refresh token no corpo)

```json

{ "refreshToken": "q1w2e3..." }

```

**204** sem corpo (idempotente). O access token já emitido continua válido até expirar (≤ 15 min) — JWT stateless não pode ser invalidado individualmente.



### 8. Recuperar senha — `POST /api/auth/recuperar-senha` (público)

```json

{ "email": "maria@exemplo.com" }

```

**202** sempre: `{ "mensagem": "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha." }`



### 8b. Redefinir senha — `POST /api/auth/redefinir-senha` (público)

```json

{ "token": "TOKEN_DO_EMAIL", "novaSenha": "NovaSenha456" }

```

**200** `{ "mensagem": "Senha redefinida com sucesso. Faça login com a nova senha." }` (todas as sessões são revogadas) ·

**400** `"Token de redefinição inválido, expirado ou já utilizado."`



### 9. Desativar conta — `DELETE /api/usuarios/me`

**204**. Conta fica `ativo=false` (linha preservada), sessões revogadas; login passa a dar 403 e o access token atual deixa de funcionar na próxima requisição.



---

## Não implementado (dependem de módulos que ainda não existem)

`GET /api/usuarios/me/ocorrencias`, `GET /api/usuarios/me/rotas`, `GET|PATCH /api/usuarios/me/preferencias`:

as tabelas `tipos_ocorrencia` e `ocorrencias` já existem (migration V3), mas ainda não há entidades/endpoints de

ocorrências; rotas e preferências continuam sem tabela/módulo (issues 4+ da épica #3).