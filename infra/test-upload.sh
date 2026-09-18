#!/bin/sh
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
T=$(wget -qO- --post-data="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234" \
  --header="Content-Type: application/x-www-form-urlencoded" "$KC" | \
  sed 's/.*"access_token":"\([^"]*\)".*/\1/')
echo "Token length: ${#T}"

cat > /tmp/test.txt << 'DOCEOF'
Enterprise RAG Guide: RAG (Retrieval Augmented Generation) combines vector search with LLM generation to produce grounded answers from your own documents.
DOCEOF

BOUNDARY="boundary$(date +%s)"
BODY="/tmp/upload_body.bin"

{
  printf -- "--%s\r\n" "$BOUNDARY"
  printf -- "Content-Disposition: form-data; name=\"title\"\r\n\r\n"
  printf -- "RAG Guide\r\n"
  printf -- "--%s\r\n" "$BOUNDARY"
  printf -- "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
  printf -- "Content-Type: text/plain\r\n\r\n"
  cat /tmp/test.txt
  printf -- "\r\n--%s--\r\n" "$BOUNDARY"
} > "$BODY"

echo "Sending upload..."
wget -O- --server-response \
  --header="Authorization: Bearer $T" \
  --header="Content-Type: multipart/form-data; boundary=$BOUNDARY" \
  --post-file="$BODY" \
  "http://127.0.0.1:8090/api/v1/documents" 2>&1
