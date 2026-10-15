# UrbanSafe — Frontend Mobile

Aplicativo mobile do projeto **UrbanSafe**, desenvolvido para apoiar o registro, consulta e acompanhamento de ocorrências relacionadas à segurança urbana.

O aplicativo está sendo desenvolvido utilizando **React Native com Expo**, **TypeScript** e **Expo Router** para navegação.

---

## Tecnologias

- **React Native** — desenvolvimento do aplicativo mobile
- **Expo SDK 57** — ambiente de desenvolvimento e execução
- **Expo Router** — navegação baseada em arquivos
- **TypeScript** — tipagem e desenvolvimento do projeto
- **Expo Go** — execução e testes durante o desenvolvimento
- **Expo Location** — obtenção da localização do dispositivo
- **Axios** — comunicação com a API REST do backend

---

## Estrutura do projeto

```text
frontend/
├── assets/
│
├── src/
│   └── app/
│       ├── _layout.tsx
│       └── index.tsx
│       │
│       ├── (pagina)/ - Desenvolvimento Futuro
│       │   ├── mapa.tsx
│       │   ├── ocorrencias.tsx
│       │   └── perfil.tsx
│       │
│       └── (auth)/ - Desenvolvimento Futuro
│           ├── login.tsx
│           └── cadastro.tsx
│
├── app.json
├── package.json
├── package-lock.json
├── tsconfig.json
└── README.md
```

A estrutura pode ser modificada conforme o desenvolvimento das funcionalidades do UrbanSafe.

---

## Requisitos

Antes de executar o projeto, é necessário ter instalado:

- Node.js
- npm
- Expo
- Expo Go no dispositivo móvel

Para verificar a instalação do Node.js:

```bash
node --version
```

Para verificar o npm:

```bash
npm --version
```

---

## Instalação

Clone o repositório:

```bash
git clone URL_DO_REPOSITORIO
```

Entre na pasta do frontend:

```bash
cd UrbanSafe/frontend
```

Instale as dependências:

```bash
npm install
```

---

## Executando o projeto

Inicie o servidor de desenvolvimento:

```bash
npx expo start
```

Caso o PowerShell do Windows bloqueie o comando npx, utilize:

```powershell
npx.cmd expo start
```

Após iniciar, será exibido um QR Code no terminal.

---

## Expo Go

Para executar no celular:

1. Instale o Expo Go.
2. Conecte o celular e o computador à mesma rede Wi-Fi.
3. Execute o projeto.
4. Escaneie o QR Code exibido pelo Expo.

---

## Navegação

O projeto utiliza Expo Router, portanto as rotas são definidas pela estrutura de arquivos dentro de:

```text
src/app/
```

Por exemplo:

```text
src/app/index.tsx
```

corresponde à rota:

```text
/
```

Enquanto:

```text
src/app/mapa.tsx
```

corresponde à:

```text
/mapa
```

### Route Groups
Pastas com parênteses são utilizadas para organização sem aparecer na URL.
Exemplo:

```text
src/app/(auth)/login.tsx
```

corresponde à:

```text
/login
```

e não:

```text
/(auth)/login
```

---

## Localização

O UrbanSafe utiliza o Expo Location para trabalhar com a localização do dispositivo.

Instalação:

```bash
npx expo install expo-location
```

A localização poderá ser utilizada em funcionalidades como:

- Identificação da localização de uma ocorrência;
- Visualização de ocorrências próximas;
- Integração com mapas;
- Preenchimento automático da localização durante um registro.

---

## Comunicação com o Backend

A comunicação com a API REST do UrbanSafe será realizada utilizando Axios.

Instalação:

```bash
npm install axios
```

A configuração da API será centralizada para facilitar a comunicação entre o aplicativo e o backend.

Exemplo (`src/services/api.ts`):

```typescript
import axios from "axios";

export const api = axios.create({
  baseURL: "http://SEU_IP:PORTA/api",
  headers: {
    "Content-Type": "application/json",
  },
});
```

> **Nota:** Durante o desenvolvimento utilizando um dispositivo físico, `localhost` normalmente não deve ser utilizado para acessar o backend que está rodando no computador. Nesse caso, utilize o endereço IP da máquina na rede local.

---

## Verificação do projeto

Para verificar possíveis problemas nas dependências do Expo:

```bash
npx expo-doctor
```

No PowerShell do Windows, caso npx esteja bloqueado:

```powershell
npx.cmd expo-doctor
```

---

## Principais comandos

```bash
# Instalar dependências
npm install

# Iniciar o projeto
npx expo start

# Verificar o projeto
npx expo-doctor

# Executar no navegador
npx expo start --web
```

---

## Funcionalidades previstas

O frontend do UrbanSafe deverá contemplar funcionalidades como:

- [ ] Tela inicial
- [ ] Cadastro de usuário
- [ ] Login
- [ ] Autenticação
- [ ] Registro de ocorrência
- [ ] Geolocalização da ocorrência
- [ ] Visualização de ocorrências
- [ ] Mapa
- [ ] Consulta de detalhes de ocorrência
- [ ] Perfil do usuário
- [ ] Comunicação com a API REST
- [ ] Tratamento de permissões de localização
- [ ] Validação dos formulários

---

## Projeto

O frontend faz parte do projeto UrbanSafe, sendo responsável pela aplicação mobile utilizada pelos usuários.

```text
UrbanSafe
│
├── backend
│   └── API REST
│
└── frontend
    └── Aplicativo Mobile
```