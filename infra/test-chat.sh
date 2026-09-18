#!/bin/sh
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
T=$(wget -qO- --post-data="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234" \
  --header="Content-Type: application/x-www-form-urlencoded" "$KC" | \
  sed 's/.*"access_token":"\([^"]*\)".*/\1/')
A="Authorization: Bearer $T"
J="Content-Type: application/json"
B="http://127.0.0.1:8090/api/v1"

echo "=== RAG Chat (grounded in your docs) ==="
wget -qO- --header="$A" --header="$J" \
  --post-data='{"question":"What is RAG and how does it work?"}' \
  "$B/chat"
echo ""

echo ""
echo "=== LLM Demo (direct, no RAG) ==="
wget -qO- --header="$A" --header="$J" \
  --post-data='{"prompt":"In one sentence, what is a vector database?"}' \
  "$B/llm/demo"
echo ""
