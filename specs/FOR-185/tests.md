# FOR-185 — Estrategia de pruebas

## Objetivo

Demostrar mediante TDD que la nueva raíz es pública, que el shell completo se
mueve a `/app` sin romper destinos, y que la landing integra auth, temas y
contenido real de forma accesible.

## Secuencia TDD granular

### 1. Contrato de rutas (RED → GREEN)

- Anónimo en `/` ve la landing y no `/login`.
- Anónimo en `/app` y cada subruta protegida termina en `/login`.
- El estado de navegación conserva `/app/...` y `search`.
- Autenticado en `/app` ve el dashboard; cada subruta renderiza la página actual.
- `/auth` y `/onboarding` conservan sus reglas actuales.
- Rutas desconocidas mantienen un resultado explícito en el contexto correcto.

Implementar el cambio mínimo de tabla de rutas y navegación solo tras ver fallar
estas pruebas.

### 2. Destinos de autenticación (RED → GREEN)

- Login sin destino navega a `/app`.
- Registro sin destino navega a `/app` después de `register -> login`.
- Login/registro con destino válido vuelven a pathname + query preservados.
- Usuario autenticado que visita `/login` o `/registro` termina en `/app` o en
  el destino preservado.
- Logout mantiene el comportamiento de FOR-145d y el siguiente acceso a `/app`
  queda protegido.

No reescribir tests del cliente API salvo que un contrato existente se rompa.

### 3. Landing y auth card (RED → GREEN)

- Renderiza landmarks, un solo `h1`, marca, beneficios, showcase, CTA y footer.
- Anónimo ve formulario real con email/password y enlace real a `/registro`.
- Submit llama una vez a `auth.login`, muestra pending y evita doble submit.
- Error seguro se anuncia y permite reintentar.
- Autenticado ve CTA a `/app`, no un formulario de login.
- Bootstrap loading/error no elimina el contenido público ni finge sesión.

### 4. Tema, responsive y accesibilidad (RED → GREEN)

- El control de tema usa `useTheme`, tiene nombre accesible y opera por teclado.
- `jest-axe` no detecta violaciones en landing anónima, autenticada, loading y
  error de submit.
- Prueba estructural o revisión CSS confirma que no hay colores literales ni
  URLs remotas en los nuevos módulos.
- Smoke a 320 px, tablet y escritorio: sin overflow horizontal, hero apilado y
  navegación operable.
- Con `prefers-reduced-motion`, no hay movimiento esencial.

### 5. SEO y build

- `frontend/index.html` mantiene `lang="es"`, título descriptivo y meta
  description.
- No se añaden scripts, fuentes o imágenes remotas.

## Comandos

Durante TDD, ejecutar primero los tests focalizados que se creen o modifiquen:

```bash
cd frontend
npm test -- src/App.test.tsx src/pages/LandingPage.test.tsx src/test/axe.test.ts
```

Antes de entregar:

```bash
cd frontend
npm test
npm run typecheck
npm run lint
npm run format:check
npm run build
```

Además:

```bash
git diff --check
```

## Smoke manual

1. Sin sesión, abrir `/` en temas claro y oscuro y a 320 px.
2. Abrir `/app/nutricion?dia=hoy`, autenticar y comprobar el retorno exacto.
3. Volver a `/`, comprobar CTA de usuario autenticado y entrar en `/app`.
4. Cerrar sesión e intentar de nuevo una ruta `/app/*`.
5. Recorrer navbar, formulario, selector de tema y CTA solo con teclado.
6. Confirmar en Network que la landing no descarga recursos de terceros.

## Regresiones que deben permanecer verdes

- AuthContext, RequireAuth, login, registro y Topbar de FOR-145d.
- Navegación y shell existentes con sus nuevas rutas esperadas.
- Tema claro/oscuro y pruebas globales de axe.

