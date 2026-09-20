#!/usr/bin/env bash
#
# Double-reverse-proxy coverage for frontend/nginx.conf.
#
# Production bug this guards: FORMA's Google login builds its OAuth
# redirect_uri from X-Forwarded-Proto/-Host (server.forward-headers-strategy=
# framework, docs/adr/ADR-014-google-login.md point 7). A real deployment sits
# behind TWO proxies:
#
#   browser --https--> OUTER proxy --http--> INNER nginx        --http--> backend
#                       (public TLS,          (this container's
#                        edge of the           frontend/nginx.conf)
#                        network)
#
# The OUTER proxy already sets X-Forwarded-Proto/-Host correctly. The INNER
# nginx (this repo's frontend/nginx.conf) used to blindly OVERWRITE them with
# its own view of the request ($scheme — always "http", since the outer
# terminates TLS and talks plain http onward — and $host, which nginx
# normalizes WITHOUT a port even when the outer's Host carried one). That
# produced wrong redirect_uri values in real deployments: `http://` instead of
# `https://` in prod, and a stripped `127.0.0.1` with no port when reached
# through a published local port (FOR-145d, e.g. :3002).
#
# This script runs the REAL frontend/nginx.conf, unmodified, in the same base
# image as frontend/Dockerfile's runtime stage (nginxinc/nginx-unprivileged:
# alpine), fronted by a throwaway OUTER nginx (outer.conf, next to this file)
# that reproduces the topology above, both proxying to a tiny Node echo
# backend (echo-backend.mjs) that reports exactly which headers it received —
# standing in for the real Spring backend so the test can assert on the wire
# format instead of trusting Spring's own header handling (that half of the
# contract is pinned separately, in
# backend/src/test/java/.../config/GoogleOAuth2ConfiguredIntegrationTest.java).
#
# Requires Docker, which is not available on every developer machine — this is
# why the check lives here rather than in a local pre-commit gate, and is
# wired into CI instead (.github/workflows/ci.yml, job "nginx").
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

RUN_ID="forma-nginx-test-$$"
NETWORK="${RUN_ID}-net"
BACKEND_CONTAINER="${RUN_ID}-backend"
INNER_CONTAINER="${RUN_ID}-inner"
OUTER_CONTAINER="${RUN_ID}-outer"
INNER_PORT=18080
OUTER_PORT=18081

FAILURES=0

cleanup() {
  docker rm -f "${BACKEND_CONTAINER}" "${INNER_CONTAINER}" "${OUTER_CONTAINER}" >/dev/null 2>&1 || true
  docker network rm "${NETWORK}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> Creating isolated Docker network"
docker network create "${NETWORK}" >/dev/null

echo "==> Starting echo backend (stands in for the Spring backend, dns alias 'backend')"
docker run -d --name "${BACKEND_CONTAINER}" --network "${NETWORK}" --network-alias backend \
  -v "${SCRIPT_DIR}/echo-backend.mjs:/echo-backend.mjs:ro" \
  node:22-alpine node /echo-backend.mjs >/dev/null

echo "==> Starting INNER nginx (the real frontend/nginx.conf, dns alias 'inner')"
docker run -d --name "${INNER_CONTAINER}" --network "${NETWORK}" --network-alias inner \
  -p "${INNER_PORT}:8080" \
  -v "${REPO_ROOT}/frontend/nginx.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginxinc/nginx-unprivileged:alpine >/dev/null

echo "==> Starting OUTER nginx (simulates the public TLS-terminating proxy)"
docker run -d --name "${OUTER_CONTAINER}" --network "${NETWORK}" \
  -p "${OUTER_PORT}:80" \
  -v "${SCRIPT_DIR}/outer.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:alpine >/dev/null

wait_ready() {
  local url="$1" label="$2" container="$3"
  local attempt
  for attempt in $(seq 1 30); do
    if curl -sf -o /dev/null "${url}"; then
      return 0
    fi
    sleep 1
  done
  echo "FAIL: ${label} never became ready at ${url}" >&2
  docker logs "${container}" >&2 || true
  exit 1
}

echo "==> Waiting for containers to answer"
wait_ready "http://127.0.0.1:${INNER_PORT}/actuator/health" "inner nginx" "${INNER_CONTAINER}"
wait_ready "http://127.0.0.1:${OUTER_PORT}/actuator/health" "outer nginx" "${OUTER_CONTAINER}"

# Reads one header from an echo-backend JSON response. Missing headers read as
# the literal string "<missing>" so a diff still prints something readable
# instead of jq's null.
read_header() {
  local response="$1" header="$2"
  echo "${response}" | jq -r --arg h "${header}" '.headers[$h] // "<missing>"'
}

assert_header() {
  local description="$1" response="$2" header="$3" expected="$4"
  local actual
  actual="$(read_header "${response}" "${header}")"
  if [ "${actual}" != "${expected}" ]; then
    echo "FAIL: ${description}"
    echo "  header:   ${header}"
    echo "  expected: ${expected}"
    echo "  actual:   ${actual}"
    FAILURES=$((FAILURES + 1))
  else
    echo "OK:   ${description} (${header} = ${actual})"
  fi
}

echo
echo "==> (a) Through outer, public prod hostname (Host: forma.backendtothefuture.com)"
response_a="$(curl -s -H 'Host: forma.backendtothefuture.com' "http://127.0.0.1:${OUTER_PORT}/api/v1/auth/me")"
assert_header "prod hostname: proto preserved as https" "${response_a}" "x-forwarded-proto" "https"
assert_header "prod hostname: host preserved" "${response_a}" "x-forwarded-host" "forma.backendtothefuture.com"

echo
echo "==> (b) Through outer, published local port (Host: localhost:3002, FOR-145d)"
response_b="$(curl -s -H 'Host: localhost:3002' "http://127.0.0.1:${OUTER_PORT}/api/v1/auth/me")"
assert_header "published port: host keeps its port" "${response_b}" "x-forwarded-host" "localhost:3002"
assert_header "published port: proto still https (outer decides it)" "${response_b}" "x-forwarded-proto" "https"

echo
echo "==> (c) Direct to inner nginx, no outer proxy in front (Host: localhost:3000)"
response_c="$(curl -s -H 'Host: localhost:3000' "http://127.0.0.1:${INNER_PORT}/api/v1/auth/me")"
assert_header "no outer: falls back to inner's own http scheme" "${response_c}" "x-forwarded-proto" "http"
assert_header "no outer: falls back to inner's own Host (port kept)" "${response_c}" "x-forwarded-host" "localhost:3000"

echo
echo "==> (d) /actuator/health also preserves X-Forwarded-Proto"
response_d="$(curl -s -H 'Host: forma.backendtothefuture.com' "http://127.0.0.1:${OUTER_PORT}/actuator/health")"
assert_header "actuator/health: proto preserved as https" "${response_d}" "x-forwarded-proto" "https"

echo
if [ "${FAILURES}" -gt 0 ]; then
  echo "${FAILURES} assertion(s) failed." >&2
  exit 1
fi

echo "All double-proxy forwarded-header assertions passed."
