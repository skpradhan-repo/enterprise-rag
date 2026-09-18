#!/bin/sh
# Get token from Keycloak internal URL
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
T=$(wget -qO- --post-data="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234" \
  --header="Content-Type: application/x-www-form-urlencoded" "$KC" | \
  sed 's/.*"access_token":"\([^"]*\)".*/\1/')
A="Authorization: Bearer $T"
J="Content-Type: application/json"
PROXY="http://127.0.0.1"

echo "=== GET /api/v1/documents (via nginx -> app) ==="
wget -qO- --timeout=10 --header="$A" "$PROXY/api/v1/documents?page=0&size=1" && echo ""

echo ""
echo "=== POST /api/v1/rag/query (via nginx -> app -> ollama) ==="
wget -qO- --timeout=120 --header="$A" --header="$J" \
  --post-data='{"question":"What is RAG?"}' \
  "$PROXY/api/v1/rag/query" && echo ""
