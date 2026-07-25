# FOR-185 — Landing pública con acceso a la aplicación

## Resultado

`/` pasa a ser una landing pública de FORMA y el dashboard autenticado pasa a
`/app`. La landing conserva la estructura útil de `docs/0-landing.html`, pero
usa exclusivamente el diseño, los componentes y los flujos reales del
frontend.

## Valor de negocio

Una persona puede entender qué resuelve FORMA antes de autenticarse y acceder
desde la misma experiencia a los flujos reales de inicio de sesión y registro.
Quien ya usa la aplicación mantiene acceso directo y seguro a todas sus rutas.

## Dependencia y entrega

- FOR-185 depende del estado de autenticación frontend de FOR-145d.
- La rama inicial es `feature/FOR-185-public-landing`, apilada sobre
  `feature/FOR-145d-frontend-auth-state`.
- El PR se abre inicialmente contra `feature/FOR-145d-frontend-auth-state`.
- Después de fusionar el PR #167, se actualiza la base de FOR-185 a `main` y se
  confirma que el diff solo contiene FOR-185.

## Alcance funcional

### Rutas

| Ruta | Acceso | Resultado |
|---|---|---|
| `/` | Público | Landing, sin redirección automática a login. |
| `/app` | Protegido | Dashboard existente. |
| `/app/mediciones` | Protegido | Mediciones existentes. |
| `/app/entrenamiento` | Protegido | Entrenamiento existente. |
| `/app/nutricion` | Protegido | Nutrición existente. |
| `/app/lista-compra` | Protegido | Lista de compra existente. |
| `/app/progreso` | Protegido | Progreso existente. |
| `/app/objetivos` | Protegido | Objetivos existentes. |
| `/app/ajustes` | Protegido | Ajustes existentes. |
| `/app/ajustes/integraciones` | Protegido | Integraciones existentes. |
| `/onboarding` | Protegido | Flujo existente, sin cambio funcional. |
| `/auth` | Público | Callback Withings existente. |
| `/login`, `/registro` | Público | Formularios reales existentes. |

Toda referencia interna a las antiguas rutas protegidas se actualiza al nuevo
prefijo `/app`. Esto incluye navegación, enlaces, redirecciones tras login,
logout, onboarding y destinos guardados. Un acceso anónimo a
`/app/nutricion?dia=hoy`, por ejemplo, conserva pathname y query para volver a
ese destino tras autenticarse. No se exige compatibilidad mediante redirects
desde las antiguas URLs sin prefijo, porque `/` adquiere un significado público;
los enlaces controlados por la aplicación no deben seguir generándolas.

### Landing

Se traduce de `docs/0-landing.html`:

- navbar con marca, navegación por anclas y accesos a login/aplicación;
- hero con propuesta de valor y tarjeta de autenticación;
- sección de beneficios;
- showcase del producto basado en capacidades reales;
- llamada a la acción final;
- footer reducido a enlaces o anclas que existan realmente.

La tarjeta del hero integra los campos y el contrato del login real. Debe
ofrecer un enlace real a `/registro`. Si la sesión ya está autenticada, la
acción principal lleva a `/app` y la landing no presenta un formulario que ya
no es necesario.

Se descarta del template cualquier elemento no respaldado por el producto:

- Tailwind, CDN, Google Fonts remotas y Material Symbols remotos;
- imágenes remotas, avatares testimoniales y fotografías de terceros;
- `+10.000 atletas`, `Nueva Versión 4.0` y métricas/claims no verificables;
- precios, planes comerciales, apps, blog, eventos, historias de éxito;
- enlaces legales (`Privacidad`, `Términos`, `Cookies`) mientras no existan sus
  páginas o URLs aprobadas;
- demo, “recordarme” y recuperación de contraseña, porque esos flujos no
  existen;
- claims de personalización, automatización o precisión que excedan las
  capacidades documentadas del MVP.

La redacción será directa, calmada y explicable: composición corporal,
entrenamiento, nutrición, compra e insights en un único lugar, sin promesas de
resultado físico ni lenguaje manipulativo.

### Autenticación existente

FOR-185 reutiliza sin cambiar el contrato de FOR-145d:

- `POST /api/v1/auth/login` inicia sesión;
- `POST /api/v1/auth/register` crea la cuenta y el frontend ejecuta después el
  login explícito;
- `GET /api/v1/auth/me` restaura la sesión;
- `POST /api/v1/auth/logout` cierra la sesión;
- cookies de sesión y CSRF siguen centralizadas en el cliente API.

La landing no replica estas reglas ni introduce otro estado de autenticación.
Durante submit, los controles quedan ocupados/deshabilitados; los errores se
anuncian de forma segura en español y permiten reintentar. Un fallo de bootstrap
no debe impedir leer el contenido público de la landing.

## Requisitos visuales y técnicos

- React, TypeScript y CSS Modules existentes; no Tailwind ni librerías nuevas.
- Solo tokens de `frontend/src/styles/theme.css` y componentes compartidos
  existentes (`Brand`, `Button`, `TextField`, estados, iconografía local) o
  extensiones reutilizables justificadas.
- Ningún color hexadecimal/rgb/hsl hardcodeado en componentes o CSS de landing.
- Tema claro y oscuro desde `ThemeProvider`, con un control accesible para
  cambiarlo.
- Assets locales y optimizados; ninguna dependencia de imágenes remotas.
- No se rediseña `AppShell` ni las páginas internas.
- SEO básico en el patrón actual: `lang="es"`, título descriptivo y meta
  description en el documento Vite. No se introduce framework SEO, SSR,
  analytics, cookies de marketing ni sitemap en esta historia.

## Accesibilidad y responsive

- HTML semántico con un único `h1`, jerarquía de títulos, landmarks y enlace
  de salto al contenido.
- Todas las entradas tienen label; errores usan `role="alert"` o asociación
  equivalente; pending usa `aria-busy` y evita doble submit.
- Navegación, selector de tema, formulario y CTA funcionan con teclado y foco
  visible.
- Anclas no quedan ocultas por la navbar fija/sticky.
- Contraste válido en ambos temas y respeto por
  `prefers-reduced-motion`; animaciones decorativas no son necesarias.
- Sin scroll horizontal a 320 px. Navbar y hero se apilan en móvil; el contenido
  no depende de hover y los objetivos táctiles son adecuados.

## Fuera de alcance

- Cambios backend, endpoints o contratos de autenticación.
- Rediseño del dashboard, shell o páginas internas.
- Password reset, recordarme, email verification, OAuth social o perfiles.
- Pricing, blog, eventos, legal, analytics, consentimiento o marketing CMS.
- Contenido dinámico, testimonios, métricas de adopción o imágenes generadas.
- Compatibilidad SEO avanzada/SSR.

## Criterios de aceptación

1. Una persona anónima abre `/` y ve la landing completa sin ser redirigida.
2. El dashboard y todas las rutas del shell viven bajo `/app` y siguen
   protegidas por el guard existente.
3. Un destino protegido preservado antes del login vuelve a la ruta equivalente
   bajo `/app`, incluida su query string.
4. Login y registro desde la landing usan el estado y API reales de FOR-145d;
   logout mantiene su contrato actual.
5. Una persona autenticada puede ir desde `/` a `/app` y no ve un formulario de
   login innecesario.
6. La landing conserva navbar, hero/auth card, beneficios, showcase, CTA y
   footer del template, sin contenido ficticio ni enlaces muertos.
7. Toda la UI funciona en temas claro y oscuro usando tokens existentes, sin
   colores hardcodeados ni recursos remotos.
8. La landing es usable a 320 px, con teclado y lector de pantalla, y no presenta
   violaciones automatizadas de accesibilidad en los estados cubiertos.
9. El documento incluye título y meta description descriptivos en español.
10. No cambia código productivo del backend ni se rediseñan pantallas internas.

## Definition of Done

- Criterios anteriores cubiertos por tests de rutas, interacción y accesibilidad.
- Tests frontend, typecheck, lint, format check y build en verde.
- Smoke manual de landing pública, login con destino preservado, `/app`, logout,
  temas y viewport móvil.
- Sin requests de assets remotos ni enlaces `href="#"` que simulen funciones.
- Diff revisado contra la base correcta y documentación actualizada si la
  implementación aclara una decisión.

