FROM docker.io/library/node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./

# Install inside the container (Linux) so platform-specific native binaries
# (e.g. @rollup/rollup-linux-x64-musl) are resolved correctly.
# --prefer-offline uses the layer cache; --no-audit skips slow network checks.
RUN npm install --prefer-offline --no-audit --no-fund

# Pass Vite env vars at build time
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_KEYCLOAK_REALM=enterprise-rag
ARG VITE_KEYCLOAK_CLIENT_ID=rag-frontend

ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

COPY . .
RUN npm run build

FROM docker.io/library/nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
