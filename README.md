# 🥊 API de Lutadores — NanadeFight

API REST desenvolvida em **Java puro** com **MongoDB Atlas** e **criptografia RSA-OAEP assimétrica**, hospedada no Heroku.

Desenvolvida para a disciplina de **Sistemas Distribuídos** — UENP.

---

## 🌐 URL Base (Produção)

```
https://lutadores-api-22f61a69f511.herokuapp.com
```

---

## 🔐 Segurança — Criptografia RSA-OAEP

Esta API utiliza **criptografia assimétrica RSA-2048** nos dois sentidos:

| Direção | Quem criptografa | Com qual chave | Quem descriptografa |
|---|---|---|---|
| Browser → Servidor (login) | Browser | Chave pública do **servidor** | Servidor (chave privada) |
| Servidor → Browser (dados) | Servidor | Chave pública do **browser** | Browser (chave privada) |

### Fluxo obrigatório antes de usar a API

```
1. GET  /chave-publica         → obtém a chave pública RSA do servidor
2. POST /handshake             → envia sua chave pública RSA ao servidor
3. Todas as respostas virão criptografadas (header X-Content-Encrypted: true)
```

> ⚠️ **Importante para integração:** As respostas dos endpoints `/lutadores` retornam um **JSON array de chunks RSA em Base64**, não JSON puro. Você precisa descriptografar com sua chave privada antes de usar os dados. Veja o exemplo de integração JavaScript abaixo.

---

## 📡 Endpoints

### 🔑 Segurança

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/chave-publica` | Retorna a chave pública RSA do servidor |
| `POST` | `/handshake` | Registra sua chave pública (necessário antes de qualquer requisição) |

#### GET `/chave-publica`
```
Resposta 200:
{
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
}
```

#### POST `/handshake`
```
Headers: Content-Type: application/json
Body:
{
  "publicKey": "SUA_CHAVE_PUBLICA_BASE64_SPKI"
}

Resposta 200:
{
  "status": "ok",
  "mensagem": "Chave pública registrada com sucesso"
}
```

---

### 🥊 Lutadores

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/lutadores` | Lista todos os lutadores |
| `GET` | `/lutadores/{id}` | Busca lutador por ID |
| `POST` | `/lutadores?nome=X&apelido=X&categoria=X&arte=X` | Cria novo lutador |
| `PUT` | `/lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X` | Atualiza lutador |
| `DELETE` | `/lutadores/{id}` | Remove lutador |

#### Exemplo — POST criar lutador
```
POST /lutadores?nome=Anderson Silva&apelido=Spider&categoria=1&arte=3

Resposta 201 (criptografada — veja seção de integração):
["Ab3xK9mP...", "Zw2qR7..."]
```

#### Exemplo — Resposta descriptografada
```json
{
  "id": 1,
  "nome": "Anderson Silva",
  "categoria": "1",
  "apelido": "Spider",
  "arte": "3"
}
```

---

### 👤 Usuários

| Método | Rota | Descrição | Body |
|---|---|---|---|
| `POST` | `/cadastro` | Cria conta | JSON com credenciais criptografadas |
| `POST` | `/login` | Autentica usuário | JSON com credenciais criptografadas |

> O body do login/cadastro deve ser um **array de chunks RSA** criptografado com a chave pública do servidor. Veja o exemplo de integração.

---

## 🧠 Modelo de Dados

### Lutador

| Campo | Tipo | Descrição | Valores válidos |
|---|---|---|---|
| `id` | int | Identificador único (gerado automaticamente) | — |
| `nome` | String | Nome completo | Qualquer string |
| `categoria` | String | Categoria de peso | `"1"`, `"2"`, `"3"` |
| `apelido` | String | Apelido/nickname | Qualquer string |
| `arte` | String | Arte marcial | `"1"` Boxe, `"2"` Karatê, `"3"` Muay Thai |

---

## 🔗 Integração JavaScript (Web Crypto API)

Cole este módulo no seu projeto para consumir a API de forma transparente:

```javascript
const API_LUTADORES = 'https://lutadores-api-22f61a69f511.herokuapp.com';

// Par de chaves do seu sistema (gerado uma vez por sessão)
let _privateKey = null;
let _servidorPublicKey = null;

// 1. Inicializa a integração (chame isso antes de qualquer requisição)
async function initLutadoresAPI() {
  // Busca chave pública do servidor
  const res = await fetch(`${API_LUTADORES}/chave-publica`);
  const { publicKey: spkiB64 } = await res.json();
  const spkiBytes = Uint8Array.from(atob(spkiB64), c => c.charCodeAt(0));
  _servidorPublicKey = await crypto.subtle.importKey(
    'spki', spkiBytes, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt']
  );

  // Gera par de chaves do cliente
  const kp = await crypto.subtle.generateKey(
    { name: 'RSA-OAEP', modulusLength: 2048, publicExponent: new Uint8Array([1,0,1]), hash: 'SHA-256' },
    true, ['encrypt', 'decrypt']
  );
  _privateKey = kp.privateKey;

  // Envia chave pública ao servidor (handshake)
  const spki = await crypto.subtle.exportKey('spki', kp.publicKey);
  const pubB64 = btoa(String.fromCharCode(...new Uint8Array(spki)));
  await fetch(`${API_LUTADORES}/handshake`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ publicKey: pubB64 })
  });
}

// 2. Descriptografa resposta do servidor
async function decryptResponse(res) {
  if (res.headers.get('X-Content-Encrypted') !== 'true') return res.json();
  const chunks = JSON.parse(await res.text());
  const bytes = [];
  for (const chunk of chunks) {
    const cb = Uint8Array.from(atob(chunk), c => c.charCodeAt(0));
    const plain = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, _privateKey, cb);
    bytes.push(...new Uint8Array(plain));
  }
  return JSON.parse(new TextDecoder().decode(new Uint8Array(bytes)));
}

// 3. Métodos prontos para uso
const LutadoresAPI = {
  listarTodos: async () => {
    const res = await fetch(`${API_LUTADORES}/lutadores`);
    return decryptResponse(res);
  },
  buscarPorId: async (id) => {
    const res = await fetch(`${API_LUTADORES}/lutadores/${id}`);
    return decryptResponse(res);
  },
  criar: async ({ nome, apelido, categoria, arte }) => {
    const qs = new URLSearchParams({ nome, apelido, categoria, arte });
    const res = await fetch(`${API_LUTADORES}/lutadores?${qs}`, { method: 'POST' });
    return decryptResponse(res);
  },
  atualizar: async (id, campos) => {
    const qs = new URLSearchParams(campos);
    const res = await fetch(`${API_LUTADORES}/lutadores/${id}?${qs}`, { method: 'PUT' });
    return decryptResponse(res);
  },
  deletar: async (id) => {
    const res = await fetch(`${API_LUTADORES}/lutadores/${id}`, { method: 'DELETE' });
    return decryptResponse(res);
  }
};

// Exemplo de uso:
// await initLutadoresAPI();
// const lutadores = await LutadoresAPI.listarTodos();
// const novo = await LutadoresAPI.criar({ nome: 'Tyson', apelido: 'Iron', categoria: '1', arte: '1' });
```

---

## 🧪 Testando no Postman (sem criptografia)

Os endpoints de **lutadores** (GET, POST, PUT, DELETE) **não exigem** que você envie dados criptografados — apenas as **respostas** vêm criptografadas.

Para testar sem implementar a descriptografia:

```
# 1. Verificar que a API está no ar
GET https://lutadores-api-22f61a69f511.herokuapp.com/chave-publica

# 2. Criar um lutador
POST https://lutadores-api-22f61a69f511.herokuapp.com/lutadores?nome=Tyson&apelido=Iron&categoria=1&arte=1

# 3. Listar lutadores (resposta virá em chunks criptografados)
GET https://lutadores-api-22f61a69f511.herokuapp.com/lutadores
```

---

## 🏗️ Estrutura do Projeto

```
BackEnd/
├── src/main/java/com/exampl/
│   ├── Main.java                    # Servidor HTTP + registro de rotas
│   ├── config/
│   │   └── Cors.java                # Configuração de CORS
│   ├── controller/
│   │   ├── LutadorController.java   # Handlers HTTP de lutadores
│   │   ├── UsuarioController.java   # Handlers HTTP de usuários
│   │   ├── HandshakeController.java # POST /handshake
│   │   └── ChavePublicaController.java # GET /chave-publica
│   ├── routes/
│   │   ├── LutadorRoutes.java
│   │   ├── UsuarioRoutes.java
│   │   ├── HandshakeRoutes.java
│   │   └── ChavePublicaRoutes.java
│   ├── services/
│   │   ├── LutadorService.java      # Regras de negócio
│   │   └── UsuarioService.java
│   ├── repository/
│   │   ├── LutadorDAO.java          # Acesso MongoDB
│   │   └── UsuarioDAO.java
│   ├── model/
│   │   ├── Lutador.java
│   │   └── Usuario.java
│   └── util/
│       ├── SecurityUtils.java       # Criptografia RSA-OAEP
│       ├── Responses.java           # Envio de respostas (com criptografia)
│       └── Utils.java               # Helpers
├── Procfile                         # Configuração Heroku
├── system.properties                # Versão Java para Heroku
└── pom.xml

FrontEnd/
├── index.html                       # Página de login
├── pages/
│   └── tela2.html                   # Gestão de lutadores
├── scripts/
│   ├── login.js                     # Autenticação com RSA
│   └── tela2.js                     # CRUD com RSA
└── Styles/
```

---

## ⚙️ Tecnologias

| Tecnologia | Uso |
|---|---|
| ☕ Java 21 | Linguagem principal |
| 📦 Maven + Shade Plugin | Build e empacotamento |
| 🍃 MongoDB Atlas | Banco de dados na nuvem |
| 🌐 HttpServer (Java SE) | Servidor HTTP sem frameworks |
| 🔐 RSA-OAEP 2048 | Criptografia assimétrica |
| ☁️ Heroku | Hospedagem da API |
