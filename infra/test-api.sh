#!/bin/sh
# No set -e — run all steps regardless of errors
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
CREDS="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234"
T=$(wget -qO- --post-data="$CREDS" --header="Content-Type: application/x-www-form-urlencoded" "$KC" | sed 's/.*"access_token":"\([^"]*\)".*/\1/')
A="Authorization: Bearer $T"
B="http://127.0.0.1:8090/api/v1"
J="Content-Type: application/json"

echo "=== 1. List Documents ==="
wget -qO- --header="$A" "$B/documents?page=0&size=5" && echo ""

echo ""
echo "=== 2. Create Conversation ==="
wget -qO- --header="$A" --header="$J" \
  --post-data='{"title":"Test chat"}' \
  "$B/conversations" && echo ""

echo ""
echo "=== 3. List Conversations ==="
wget -qO- --header="$A" "$B/conversations?page=0&size=5" && echo ""

echo ""
echo "=== 4. RAG Chat ==="
wget -qO- --header="$A" --header="$J" \
  --post-data='{"question":"What is enterprise RAG and how does it work?"}' \
  "$B/chat" && echo "" || echo "[HTTP error on chat - check logs]"

echo ""
echo "=== 5. LLM Demo (direct, no RAG) ==="
wget -qO- --header="$A" --header="$J" \
  --post-data='{"prompt":"In one sentence, what is a vector database?"}' \
  "$B/llm/demo" && echo "" || echo "[HTTP error on llm/demo - check logs]"

echo ""
echo "=== Done ==="
