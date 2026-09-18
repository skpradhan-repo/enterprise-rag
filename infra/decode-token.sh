#!/bin/sh
KC="http://keycloak:8080/realms/enterprise-rag/protocol/openid-connect/token"
CREDS="client_id=rag-app&grant_type=password&username=admin%40acme.com&password=Admin%401234"
T=$(wget -qO- --post-data="$CREDS" --header="Content-Type: application/x-www-form-urlencoded" "$KC" | sed 's/.*"access_token":"\([^"]*\)".*/\1/')
echo "Token length: ${#T}"
PAYLOAD=$(echo "$T" | cut -d. -f2 | tr '_-' '/+')
echo "$PAYLOAD==" | base64 -d 2>/dev/null
