#!/bin/sh
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
T=$(wget -qO- --post-data="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234" \
  --header="Content-Type: application/x-www-form-urlencoded" "$KC" | \
  sed 's/.*"access_token":"\([^"]*\)".*/\1/')

echo "=== Test through nginx proxy (browser path) ==="
echo "Token iss check: $(echo $T | cut -d. -f2 | tr '_-' '/+' | awk 'length%4==2{$0=$0"=="} length%4==3{$0=$0"="} 1' | base64 -d 2>/dev/null | sed 's/.*"iss":"\([^"]*\)".*/\1/')"

echo ""
echo "GET /api/v1/documents:"
wget -qO- --timeout=10 --header="Authorization: Bearer $T" \
  "http://127.0.0.1/api/v1/documents?page=0&size=1" && echo ""

echo ""
echo "POST /api/v1/rag/query (waits for LLM ~30-60s):"
wget -qO- --timeout=120 --header="Authorization: Bearer $T" \
  --header="Content-Type: application/json" \
  --post-data='{"question":"What is RAG?"}' \
  "http://127.0.0.1/api/v1/rag/query" && echo ""
