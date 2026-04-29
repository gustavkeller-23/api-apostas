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

## 📡 Requisições


| Tipo                 | Método  |  Requisição                                         |
|----------------------|---------|-----------------------------------------------------|
| 📄 Listar todos      | GET    |  /lutadores                                          |
| 🔍 Buscar por ID     | GET    | /lutadores/{id}                                      |
| ➕ Criar lutador     | POST   | /lutadores?nome=X&apelido=X&categoria=X&arte=X       |
| ✏️ Atualizar lutador | PUT    | /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X  |
| ❌ Deletar lutador   | DELETE |  /lutadores/{id}                                     |


## 🧪 Exemplos

### ✅ Resposta de sucesso

```json
{
  "id": 1,
  "nome": "Anderson",
  "categoria": 2,
  "apelido": "Spider",
  "arte": 1
}```

❌ Erro

```json
{
  "erro": "Lutador não encontrado"
}```


🚀 Como Executar
1️⃣ Inicie o MongoDB
mongod
2️⃣ Compile o projeto
mvn clean install
3️⃣ Execute a aplicação
mvn exec:java

ou rode a classe Main.java.
