# AI Products Chatbot

A full-stack chatbot application that uses **Azure AI Foundry** agents and **Azure AI Search** to answer questions about fictional products, with a **MCP (Model Context Protocol)** server for purchase logging.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        React Frontend                           │
│  ChatWindow → ChatMessage → ProductCard (Buy Button)            │
└───────────────────────┬──────────────────────────────────────────┘
                        │ HTTP (JSON)
┌───────────────────────▼──────────────────────────────────────────┐
│                   Spring Boot Backend                           │
│                                                                 │
│  ChatController          PurchaseController    McpController    │
│       │                        │                    │           │
│  AgentService            PurchaseService       McpServer        │
│  (interface)             (interface)           │                 │
│       │                        │           PurchaseLogTool      │
│  AzureAgentServiceImpl   PurchaseServiceImpl   (McpTool)        │
│       │                        │                                 │
│  SearchService           PurchaseLogRepository                  │
│  (interface)             (interface)                            │
│       │                        │                                 │
│  AzureSearchServiceImpl  InMemoryPurchaseLogRepository          │
│       │                  (swap → JPA / Cosmos DB)               │
└───────┼──────────────────────────────────────────────────────────┘
        │
┌───────▼──────────────────────────────────────────────────────────┐
│                        Azure Services                           │
│                                                                 │
│  Azure AI Foundry          Azure AI Search                      │
│  (Agents + Threads)        (Product Index)                      │
└──────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Layer | Interface | Current Impl | Swap-in Options |
|---|---|---|---|
| Product catalog | `ProductRepository` | `InMemoryProductRepository` | Spring Data JPA, Cosmos DB |
| Purchase log | `PurchaseLogRepository` | `InMemoryPurchaseLogRepository` | JPA, Cosmos DB, Azure Blob |
| AI Agent | `AgentService` | `AzureAgentServiceImpl` | OpenAI Assistants, LangChain4j |
| Search | `SearchService` | `AzureSearchServiceImpl` | Elasticsearch, Cosmos DB vector |
| Purchase flow | `PurchaseService` | `PurchaseServiceImpl` (via MCP) | Direct DB, payment gateway |

---

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, azure-ai-projects SDK, azure-search-documents SDK
- **Frontend**: React 18, Axios, react-markdown
- **Cloud**: Azure AI Foundry (Agents), Azure AI Search
- **Protocol**: MCP (Model Context Protocol) for tool-based purchase logging

---

## Project Structure

```
.
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/aiproducts/
│       ├── AiProductsApplication.java
│       ├── config/
│       │   ├── AzureAIConfig.java        # Azure SDK bean wiring
│       │   └── CorsConfig.java
│       ├── controller/
│       │   ├── ChatController.java        # POST /api/chat
│       │   ├── PurchaseController.java    # POST /api/purchase
│       │   └── McpController.java         # POST /mcp/v1
│       ├── model/
│       │   ├── Product.java
│       │   ├── PurchaseLog.java
│       │   ├── ChatRequest.java
│       │   └── ChatResponse.java
│       ├── repository/                    # DB interfaces
│       │   ├── ProductRepository.java
│       │   ├── PurchaseLogRepository.java
│       │   └── impl/
│       │       ├── InMemoryProductRepository.java    # seeded with 8 products
│       │       └── InMemoryPurchaseLogRepository.java
│       ├── service/                       # External service interfaces
│       │   ├── AgentService.java
│       │   ├── SearchService.java
│       │   ├── PurchaseService.java
│       │   └── impl/
│       │       ├── AzureAgentServiceImpl.java
│       │       ├── AzureSearchServiceImpl.java
│       │       └── PurchaseServiceImpl.java
│       └── mcp/                           # Embedded MCP server
│           ├── McpServer.java             # JSON-RPC 2.0 dispatcher
│           ├── McpTool.java               # Tool interface
│           ├── McpRequest.java
│           ├── McpResponse.java
│           └── tools/
│               └── PurchaseLogTool.java   # log_purchase tool
└── frontend/
    └── src/
        ├── App.jsx
        ├── components/
        │   ├── ChatWindow.jsx    # Conversation state + thread management
        │   ├── ChatMessage.jsx   # Bubble + product grid
        │   ├── ProductCard.jsx   # Product card + Buy button
        │   └── MessageInput.jsx  # Textarea + send button
        └── services/
            └── api.js            # Axios calls to backend
```

---

## Azure Setup

### 1. Create Azure AI Foundry Project

1. Go to [Azure AI Foundry](https://ai.azure.com)
2. Create a Hub and Project
3. Deploy a model (e.g., `gpt-4o`)
4. Note the **project endpoint** (format: `https://<hub>.services.ai.azure.com/api/projects/<project>`)

### 2. Set Up Azure AI Search

1. Create an **Azure AI Search** resource
2. Create an index named `products` with this schema:

```json
{
  "name": "products",
  "fields": [
    { "name": "id",           "type": "Edm.String",  "key": true },
    { "name": "name",         "type": "Edm.String",  "searchable": true },
    { "name": "description",  "type": "Edm.String",  "searchable": true },
    { "name": "category",     "type": "Edm.String",  "filterable": true, "facetable": true },
    { "name": "price",        "type": "Edm.Double",  "filterable": true, "sortable": true },
    { "name": "currency",     "type": "Edm.String"  },
    { "name": "availability", "type": "Edm.String",  "filterable": true },
    { "name": "rating",       "type": "Edm.Double",  "sortable": true },
    { "name": "tagline",      "type": "Edm.String",  "searchable": true },
    { "name": "imageUrl",     "type": "Edm.String"  }
  ],
  "semantic": {
    "configurations": [{
      "name": "default",
      "prioritizedFields": {
        "contentFields": [
          { "fieldName": "description" },
          { "fieldName": "name" }
        ]
      }
    }]
  }
}
```

3. Connect the Search resource to your AI Foundry project (via **Connections**)
4. Note the **connection ID** from the Foundry project

### 3. Configure & Run Backend

```bash
export AZURE_AI_PROJECT_ENDPOINT="https://<hub>.services.ai.azure.com/api/projects/<project>"
export AZURE_SEARCH_ENDPOINT="https://<search-name>.search.windows.net"
export AZURE_SEARCH_CONNECTION_ID="<connection-id-from-foundry>"
export AZURE_SEARCH_INDEX_NAME="products"
export AZURE_AGENT_MODEL="gpt-4o"

# Authentication (choose one):
# Option A - Service Principal:
export AZURE_TENANT_ID="<tenant-id>"
export AZURE_CLIENT_ID="<client-id>"
export AZURE_CLIENT_SECRET="<client-secret>"
# Option B - Azure CLI (local dev): az login

cd backend
mvn spring-boot:run
```

### 4. Run Frontend

```bash
cd frontend
npm install
npm start
# Opens http://localhost:3000
```

---

## API Reference

### Chat

```
POST /api/chat
{ "message": "Tell me about your headphones", "threadId": null }

Response:
{ "message": "...", "products": [...], "threadId": "thread_abc123", "error": false }
```

Pass `threadId` from the previous response to continue the same conversation.

### Purchase (triggers MCP log_purchase tool)

```
POST /api/purchase
{ "productId": "P004", "quantity": 1, "sessionId": "thread_abc123" }
```

### MCP Server (JSON-RPC 2.0)

```
POST /mcp/v1
{ "jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {} }

POST /mcp/v1
{ "jsonrpc": "2.0", "id": 2, "method": "tools/call",
  "params": { "name": "log_purchase",
              "arguments": { "product_id": "P004", "product_name": "SkyPod Headphones",
                             "price": "349.99", "currency": "USD", "quantity": 1 } } }
```

---

## Extending the App

### Swap the Database

1. Add `spring-boot-starter-data-jpa` to `pom.xml` and configure a datasource
2. Create `JpaPurchaseLogRepository implements PurchaseLogRepository` + `@Primary`
3. Annotate `PurchaseLog` with `@Entity`, `@Id`, etc.
4. No changes to `PurchaseServiceImpl` or any other layer

### Add a New MCP Tool

1. Create a class: `public class MyTool implements McpTool`
2. Add `@Component` — Spring auto-registers it in `McpServer`
3. Implement `getName()`, `getDescription()`, `getInputSchema()`, `execute()`

### Use a Different AI Provider

Implement `AgentService` with any provider (OpenAI, Gemini, LangChain4j) and swap the
`@Primary` bean — the controllers and frontend remain unchanged.
