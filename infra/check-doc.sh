#!/bin/sh
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
T=$(wget -qO- --post-data="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234" \
  --header="Content-Type: application/x-www-form-urlencoded" "$KC" | \
  sed 's/.*"access_token":"\([^"]*\)".*/\1/')

DOC_ID="${1:-f65f62d8-cd01-4231-8656-5dc434500308}"
echo "Checking document: $DOC_ID"
wget -qO- --header="Authorization: Bearer $T" \
  "http://127.0.0.1:8090/api/v1/documents/$DOC_ID"
echo ""
echo "All documents:"
wget -qO- --header="Authorization: Bearer $T" \
  "http://127.0.0.1:8090/api/v1/documents?page=0&size=10" | \
  sed 's/,/\n/g' | grep -E '"id"|"title"|"status"'
