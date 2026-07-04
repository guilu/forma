# FORMA frontend image (FOR-82). Multi-stage: build the Vite bundle with Node,
# then serve the static files with an unprivileged (non-root) nginx on port 8080.

# --- Build stage ---------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies from the lockfile for reproducible builds.
COPY package.json package-lock.json ./
RUN npm ci

# The API base URL is baked in at build time (Vite inlines VITE_* vars).
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY . .
RUN npm run build

# --- Runtime stage -------------------------------------------------------
FROM nginxinc/nginx-unprivileged:alpine AS runtime

# SPA routing: unknown paths fall back to index.html.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
