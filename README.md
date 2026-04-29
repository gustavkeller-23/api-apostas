# 🥊 API de Lutadores

Uma API REST simples desenvolvida em Java puro (sem frameworks pesados), utilizando MongoDB para persistência de dados.

---

## 📌 Visão Geral

Este projeto implementa uma API para gerenciamento de lutadores, permitindo operações completas de:

- ✅ Criar
- 📄 Listar
- 🔍 Buscar por ID
- ✏️ Atualizar
- ❌ Deletar

---

## 🏗️ Estrutura do Projeto


com.exampl
│
├── Main.java # Servidor HTTP e rotas
├── Lutador.java # Modelo (entidade)
└── LutadorDAO.java # Acesso ao MongoDB


---

## ⚙️ Tecnologias Utilizadas

- ☕ Java 17+
- 📦 Maven
- 🍃 MongoDB
- 🌐 HTTP Server nativo do Java (`HttpServer`)

---

## 🧠 Modelo de Dados

### 📄 Lutador

| Campo      | Tipo   | Descrição                |
|------------|--------|--------------------------|
| id         | int    | Identificador único      |
| nome       | String | Nome do lutador          |
| categoria  | int    | Categoria (peso/classe)  |
| apelido    | String | Apelido                  |
| arte       | int    | Arte marcial             |

---

## 🗄️ Banco de Dados

- **URL:** `mongodb://localhost:27017`
- **Database:** `lutadoresDB`
- **Collection:** `lutadores`

---

## 🌐 API REST

### 🔗 Base URL


http://localhost:8080


---

## 📡 Endpoints

### 📄 Listar todos


GET /lutadores


---

### 🔍 Buscar por ID


GET /lutadores/{id}


---

### ➕ Criar lutador


POST /lutadores?nome=X&apelido=X&categoria=X&arte=X


📌 Exemplo:

POST /lutadores?nome=Anderson&apelido=Spider&categoria=2&arte=1


---

### ✏️ Atualizar lutador


PUT /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X


📌 Atualiza apenas os campos enviados

---

### ❌ Deletar lutador


DELETE /lutadores/{id}


---

## 🧪 Exemplos

### ✅ Resposta de sucesso

```json
{
  "id": 1,
  "nome": "Anderson",
  "categoria": 2,
  "apelido": "Spider",
  "arte": 1
}

❌ Erro
{
  "erro": "Lutador não encontrado"
}

🚀 Como Executar
1️⃣ Inicie o MongoDB
mongod
2️⃣ Compile o projeto
mvn clean install
3️⃣ Execute a aplicação
mvn exec:java

ou rode a classe Main.java.

🔐 CORS

A API permite acesso de qualquer origem:

Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
⚠️ Observações
O ID é gerado manualmente (incremental)
Pode haver problemas em ambientes concorrentes
Serialização JSON é feita manualmente
📈 Melhorias Futuras
🚀 Migrar para Spring Boot
🔐 Implementar autenticação (JWT)
📦 Usar Jackson/Gson
🔢 Melhorar geração de ID (UUID/ObjectId)
📄 Adicionar validações
📊 Paginação de resultados
🎯 Objetivo

Projeto ideal para aprendizado de:

APIs REST
Integração com MongoDB
HTTP puro em Java
👨‍💻 Autor

Desenvolvido por você 🚀

⭐ Contribuição

Sinta-se livre para contribuir com melhorias!


---

Se quiser, posso dar um upgrade ainda maior:
- :contentReference[oaicite:0]{index=0}
- :contentReference[oaicite:1]{index=1}
- :contentReference[oaicite:2]{index=2}
- :contentReference[oaicite:3]{index=3}

Só falar 👍