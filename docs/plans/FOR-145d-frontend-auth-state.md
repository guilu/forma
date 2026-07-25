# FOR-145d frontend auth state implementation plan

## Objetivo

Entregar la parte frontend de FOR-145d sin tocar backend productivo: estado de sesión, login/registro/logout, guard de rutas, bootstrap del usuario actual y cliente API con cookies + CSRF según ADR-012.

## Alcance técnico

- Consumir `POST /api/v1/auth/register`
- Consumir `POST /api/v1/auth/login`
- Consumir `GET /api/v1/auth/me`
- Consumir `POST /api/v1/auth/logout`
- Mantener `/auth` público para el callback de Withings
- Proteger el árbol actual de `AppShell`

## Archivos probables

### Crear

- `frontend/src/api/auth.ts`
- `frontend/src/api/auth.test.ts`
- `frontend/src/auth/AuthContext.tsx`
- `frontend/src/auth/AuthContext.test.tsx`
- `frontend/src/auth/RequireAuth.tsx`
- `frontend/src/auth/RequireAuth.test.tsx`
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/LoginPage.test.tsx`
- `frontend/src/pages/LoginPage.module.css`
- `frontend/src/pages/RegisterPage.tsx`
- `frontend/src/pages/RegisterPage.test.tsx`
- `frontend/src/pages/RegisterPage.module.css`

### Modificar

- `frontend/src/api/client.ts`
- `frontend/src/api/client.test.ts`
- `frontend/src/App.tsx`
- `frontend/src/App.test.tsx`
- `frontend/src/app/routes.tsx`
- `frontend/src/layout/Topbar.tsx`
- `frontend/src/layout/Topbar.module.css`
- `frontend/src/layout/shell.test.tsx`
- `frontend/src/main.tsx` solo si hiciera falta ajustar el punto de montaje del provider

## Decisiones de implementación

### Cliente API y CSRF

Implementar el comportamiento en `frontend/src/api/client.ts`, no en cada feature.

Reglas exactas:

1. Resolver `credentials` así:
   - `same-origin` cuando `baseUrl` sea `''`, relativo, o del mismo origen que `window.location.origin`
   - `include` cuando `baseUrl` apunte a otro origen
2. Antes de cualquier `POST`, `PUT`, `PATCH` o `DELETE`:
   - leer `document.cookie`
   - si no existe `XSRF-TOKEN`, hacer `GET /actuator/health` con las mismas `credentials` para primar la cookie
   - volver a leer `document.cookie`
   - enviar `X-XSRF-TOKEN: <valor-cookie>` si el token ya existe
   - si después del priming el token sigue ausente o no es legible, abortar sin enviar la petición insegura
3. `GET`, `HEAD` y `OPTIONS` no envían `X-XSRF-TOKEN`.
4. `requestBlob` también debe enviar cookies de sesión.

Nota importante:

- Para `login` y `register`, el primer POST probablemente ocurrirá sin sesión, pero igualmente necesita CSRF; por eso el priming debe usar `GET /actuator/health` y no `GET /api/v1/auth/me`.
- `GET /api/v1/auth/me` queda para bootstrap y refresco del usuario autenticado.

### Estado de auth

Mantenerlo mínimo:

- `status`: `loading | authenticated | anonymous`
- `user`: `{ id, email } | null`
- métodos: `login`, `register`, `logout`, `refreshCurrentUser`

El contrato backend real separa creación de cuenta y sesión: `/register` crea y devuelve la cuenta, pero no establece `SecurityContext` ni `JSESSIONID`. Por tanto, el método frontend `register` debe encadenar `register -> login` con las mismas credenciales y solo marcar `authenticated` tras el login correcto.

Evitar persistencia local de credenciales o user cache duradera. La fuente de verdad es la cookie de sesión + `GET /api/v1/auth/me`.

### Guard de rutas

- Crear un wrapper tipo `RequireAuth` para el árbol protegido.
- Rutas públicas nuevas: `/login`, `/registro`.
- Si llega una persona anónima a una ruta protegida, redirigir a `/login` preservando el destino intentado.
- Si la sesión ya está autenticada y la persona entra en `/login` o `/registro`, redirigir a `/` o al destino preservado.

### Topbar

- Sustituir `Diego` por el `email` real devuelto por `/api/v1/auth/me`.
- Añadir una acción de `Cerrar sesión` accesible.
- Mantener toggle de tema y campana tal como están.

## TDD por tareas pequeñas

1. Endurecer el cliente API para cookies + CSRF.
   - Empezar por tests en `frontend/src/api/client.test.ts`.
   - Cubrir `credentials`, priming `GET /actuator/health`, lectura de `XSRF-TOKEN`, envío de `X-XSRF-TOKEN`, aborto seguro si el token sigue inaccesible y `requestBlob`.
   - Después implementar en `frontend/src/api/client.ts`.

2. Añadir el módulo de API de auth.
   - Crear `frontend/src/api/auth.test.ts`.
   - Cubrir `register`, `login`, `me`, `logout` sobre el `ApiClient` compartido.
   - Implementar `frontend/src/api/auth.ts` con funciones finas, sin lógica de UI.

3. Añadir el provider de auth.
   - Crear `frontend/src/auth/AuthContext.test.tsx`.
   - Casos: bootstrap 200, bootstrap 401, bootstrap error no-401, login, register seguido de login explícito, y logout.
   - Implementar `frontend/src/auth/AuthContext.tsx` con estado y API pública mínima.

4. Añadir el guard de rutas.
   - Crear `frontend/src/auth/RequireAuth.test.tsx` o cubrirlo en `App.test.tsx` si queda más simple.
   - Casos: ruta protegida anónima redirige a `/login`; auth pages redirigen si ya hay sesión; se conserva destino.
   - Implementar el guard y conectarlo en `frontend/src/app/routes.tsx`.

5. Añadir UI de login.
   - Crear `frontend/src/pages/LoginPage.test.tsx`.
   - Casos: render en español, submit correcto, error backend, navegación a registro, redirect al destino preservado.
   - Implementar `frontend/src/pages/LoginPage.tsx` y su CSS.

6. Añadir UI de registro.
   - Crear `frontend/src/pages/RegisterPage.test.tsx`.
   - Casos: render en español, validación de confirmación de contraseña, submit correcto, errores backend, navegación a login.
   - Implementar `frontend/src/pages/RegisterPage.tsx` y su CSS.

7. Integrar el provider en la app.
   - Ajustar `frontend/src/App.tsx` y solo tocar `frontend/src/main.tsx` si el árbol real lo necesita.
   - Añadir cobertura en `frontend/src/App.test.tsx` para bootstrap + redirecciones de rutas.

8. Actualizar Topbar.
   - Ampliar `frontend/src/layout/shell.test.tsx`.
   - Reemplazar la aserción de `Diego` por email real y probar `Cerrar sesión`.
   - Implementar cambios mínimos en `frontend/src/layout/Topbar.tsx` y `Topbar.module.css`.

9. Smoke final del flujo.
   - Confirmar login -> ruta protegida -> logout -> redirect a login.
   - Confirmar que un POST autenticado envía `X-XSRF-TOKEN`.

## Comandos de test y build

### Frontend unit tests

```bash
cd frontend && npm test
```

### Frontend targeted tests durante TDD

```bash
cd frontend && npm test -- src/api/client.test.ts src/api/auth.test.ts src/auth/AuthContext.test.tsx src/App.test.tsx src/layout/shell.test.tsx src/pages/LoginPage.test.tsx src/pages/RegisterPage.test.tsx
```

### Typecheck

```bash
cd frontend && npm run typecheck
```

### Lint

```bash
cd frontend && npm run lint
```

### Build

```bash
cd frontend && npm run build
```

## Docker y smoke checks

Usar el entorno real de `compose.yaml`.

### Levantar entorno

```bash
docker compose up --build -d
```

### Ver estado

```bash
docker compose ps
```

### Smoke backend CSRF/session

Usar los puertos operativos de Forma en `.env` local (`BACKEND_PORT=18080`, `FRONTEND_PORT=3002`) o los equivalentes que declare `docker compose ps`.

```bash
curl -i http://127.0.0.1:18080/actuator/health
```

Esperado:

- respuesta `200`
- cookie `XSRF-TOKEN` presente, si el backend escribe el token en esa petición de priming

### Smoke SPA manual

1. Abrir `http://127.0.0.1:3002/login`.
2. Registrar una cuenta nueva.
3. Confirmar redirect al destino protegido o a `/`.
4. Recargar la página y verificar que la sesión se restaura vía `/api/v1/auth/me`.
5. Cerrar sesión y confirmar redirect a `/login`.
6. Intentar abrir una ruta protegida directamente y confirmar redirect a `/login`.

### Verificación opcional en DevTools

- En `Application/Cookies`, confirmar `JSESSIONID` y `XSRF-TOKEN`.
- En `Network`, confirmar:
  - `POST /api/v1/auth/login` incluye `X-XSRF-TOKEN`
  - requests protegidas incluyen cookies
  - `GET /api/v1/auth/me` devuelve `200` tras recarga con sesión activa

## Checklist de evidencia para PR/Jira

- Captura o descripción del flujo `registro -> login explícito -> shell protegida`.
- Captura o descripción del flujo `logout -> redirect a /login`.
- Evidencia de que el Topbar ya no muestra `Diego` fijo.
- Evidencia de tests frontend en verde.
- Evidencia de `npm run typecheck` y `npm run build` en verde.
- Evidencia del smoke con Docker Compose.
- Enlace Jira: `FOR-145`.
- PR title esperado: `FOR-145d ...`

## Fuera de alcance

- Cambios backend de auth.
- Reseteo de contraseña o verificación de email.
- Prueba exhaustiva de aislamiento cross-user en los 16 dominios; eso queda para FOR-145e.
