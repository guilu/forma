# FOR-185 — Contexto para implementación asistida

## Lee primero

1. `AGENTS.md` y su orden obligatorio.
2. `specs/FOR-185/spec.md`, `ui.md` y `tests.md`.
3. `docs/0-landing.html` completo, solo como referencia estructural.
4. `specs/FOR-145/145d-frontend-auth-state.md` y
   `docs/plans/FOR-145d-frontend-auth-state.md`.
5. `docs/adr/ADR-006-frontend.md`, `ADR-012-authentication-and-multi-user-isolation.md`
   y `docs/ui-guidelines.md`.
6. Routing, auth, tema, tokens, navegación y componentes compartidos reales bajo
   `frontend/src/`.

## Fuente de verdad

El repositorio y FOR-145d mandan sobre el HTML generado. No copies su Tailwind,
configuración, colores, fuentes remotas, iconos remotos, imágenes, claims ni
enlaces ficticios. Traduce su composición al sistema React + CSS Modules de
FORMA.

## Restricciones que evitan regresiones

- La rama está apilada inicialmente sobre
  `feature/FOR-145d-frontend-auth-state`; el PR debe apuntar a esa rama hasta
  que se fusione #167, y después retargetearse a `main`.
- `/` se libera para la landing. El árbol completo de `AppShell` se mueve a
  `/app`; no basta con mover solo el dashboard.
- Busca rutas absolutas en navegación, tests, redirects y componentes antes de
  editar. Actualiza destinos controlados por la SPA de forma consistente.
- Conserva `pathname + search` del destino guardado. No aceptes URLs externas
  como destino de navegación.
- No montes un segundo `AuthProvider`, no llames a `fetch` desde la landing y no
  replique lógica CSRF.
- Registro sigue siendo `register -> login`; logout fallido conserva la sesión
  según la implementación de FOR-145d.
- La landing es pública incluso mientras `/auth/me` está cargando o falla. Solo
  las partes dependientes de sesión cambian de estado.
- Reutiliza `Brand`, `Button`, `TextField`, `useAuth`, `useTheme` y tokens. Una
  extracción nueva solo se justifica si evita duplicación real.
- No cambies backend, dependencias, shell ni visuales internos.

## Decisiones de contenido

| Mantener del template | Sustituir o eliminar |
|---|---|
| Navbar y anclas internas reales | Links vacíos, pricing, apps, blog, eventos y legal inexistente |
| Hero y auth card | “Versión 4.0”, demo, recordarme y password reset |
| Tres beneficios basados en módulos reales | Claims absolutos o resultados garantizados |
| Showcase con UI/CSS/assets locales | Fotos, avatares, métricas y testimonios remotos |
| CTA a registro o `/app` | Urgencia artificial y gamificación |
| Footer mínimo con navegación real | Redes sociales o páginas no disponibles |

## Puntos de revisión

1. Revisa primero el diff de rutas y pruebas de redirección.
2. Revisa después la integración del auth card y sus estados.
3. Revisa por último estructura, CSS tokenizado, responsive, a11y y SEO.

No declares éxito por parecido visual. La implementación es correcta cuando el
flujo público/protegido, los destinos y la accesibilidad están probados.

