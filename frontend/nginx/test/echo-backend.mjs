// Throwaway echo backend for the double-reverse-proxy nginx test
// (forwarded-headers.test.sh, next to this file). Stands in for the real Spring
// backend, which frontend/nginx.conf proxies to as `backend:8080` — this script
// answers any request with the headers it actually received, as JSON, so the
// test can assert exactly what X-Forwarded-Proto/-Host reached "the backend"
// after passing through the inner (and sometimes outer) nginx. No third-party
// echo image is used so the test has no external dependency beyond the
// `node:22-alpine` image already used elsewhere in this repo (frontend/Dockerfile).
import http from "node:http";

const server = http.createServer((req, res) => {
  const body = JSON.stringify({
    method: req.method,
    url: req.url,
    headers: req.headers,
  });
  res.writeHead(200, { "content-type": "application/json" });
  res.end(body);
});

server.listen(8080, "0.0.0.0", () => {
  console.log("echo backend listening on 0.0.0.0:8080");
});
