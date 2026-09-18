# LLM Client / Server Architecture

## Concept

> **Spring AI application = LLM Client**
> **Ollama = LLM Server**
> **llama3.2 = Local Model (inference engine)**

The model is never embedded inside the Spring Boot process. It runs as a separate server process (Ollama) and is accessed over HTTP.

## Architecture

```mermaid
sequenceDiagram
    participant React
    participant SpringBoot as Spring Boot<br/>(LLM Client)
    participant SpringAI as Spring AI<br/>ChatClient
    participant Ollama as Ollama<br/>(LLM Server)
    participant Model as llama3.2<br/>(Local Model)

    React->>SpringBoot: POST /api/v1/llm/demo {prompt}
    SpringBoot->>SpringAI: chatClient.prompt().user(prompt).call()
    SpringAI->>Ollama: HTTP POST /api/chat {model, messages}
    Ollama->>Model: Run inference
    Model-->>Ollama: Token stream
    Ollama-->>SpringAI: ChatCompletion response
    SpringAI-->>SpringBoot: content()
    SpringBoot-->>React: {response, model, provider, latencyMs}
```

## Configuration

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: ${LLM_MODEL:llama3.2}
      embedding:
        model: ${EMBEDDING_MODEL:nomic-embed-text}
```

## Model Abstraction

Business logic **never** depends on `OllamaChatModel` directly. It depends on:
- `ChatClient` — the Spring AI abstraction for chat
- `EmbeddingModel` — the Spring AI abstraction for embeddings
- `VectorStore` — the Spring AI abstraction for vector search

Switching from Ollama to another provider requires only a configuration change — no code changes in business logic.

## Starting Ollama

```bash
# Start Ollama server (or use Docker Compose)
ollama serve

# Pull required models
ollama pull llama3.2
ollama pull nomic-embed-text

# Verify
curl http://localhost:11434/api/tags
```
