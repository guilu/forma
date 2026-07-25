# FOR-185 — Especificación UI

## Estructura de página

1. **Skip link**: lleva al contenido principal.
2. **Navbar sticky**: marca FORMA, anclas `Beneficios` y `Producto`, selector de
   tema y acción contextual (`Iniciar sesión` o `Ir a la aplicación`).
3. **Hero**: propuesta de valor verificable, CTA secundario por ancla y auth
   card real para personas anónimas.
4. **Beneficios**: composición corporal, planificación de entrenamiento/nutrición
   y visión integrada, sin cifras ni resultados prometidos.
5. **Showcase**: representación local y accesible del producto; puede usar
   composición CSS/componentes existentes o un asset local aprobado, nunca una
   captura remota ni datos personales reales.
6. **CTA final**: registro para anónimos; `/app` para autenticados.
7. **Footer**: marca, descripción corta, anclas/rutas reales y copyright; no
   enlaces placeholder.

## Estados por sesión

| Estado | Auth card / acción |
|---|---|
| `anonymous` | Login real, enlace a `/registro`, errores y pending de FOR-145d. |
| `authenticated` | Resumen mínimo con email y botón `Ir a la aplicación`; no formulario. |
| `loading` | Landing visible; la zona de acceso indica comprobación sin bloquear el resto. |
| bootstrap error | Landing visible; copy neutral y acceso explícito a `/login`; no error de página completa. |
| submit error | Mensaje español con `role="alert"`; campos y reintento permanecen disponibles. |

## Diseño

- Inspiración estructural: `docs/0-landing.html`.
- Implementación: CSS Modules + tokens de `frontend/src/styles/theme.css`.
- Componentes base: `Brand`, `Button`, `TextField` y hooks de tema/auth actuales.
- Tipografía: `--font-heading` y `--font-sans`, ya autoalojadas.
- Superficies, bordes, texto, acento, estados, espaciado, radios y sombras usan
  variables existentes. CERO colores literales nuevos.
- El acento guía CTA y estado activo; no debe saturar toda la página.
- Decoración geométrica opcional solo con tokens/CSS, `aria-hidden` y sin afectar
  lectura o reduced motion.

## Contenido aprobado

- Nombre: `FORMA`.
- Posicionamiento: sistema personal para organizar composición corporal,
  entrenamiento, nutrición, compra e insights explicables.
- Tono: directo, calmado, técnico y sin culpa.
- No usar “plataforma definitiva”, “elite”, “optimiza tu rendimiento”, números de
  usuarios, versiones, automatización total o resultados garantizados.

## Responsive

- **320–767 px**: navbar compacta sin enlaces ocultos inaccesibles; hero y card
  en una columna; CTA de ancho cómodo; grids en una columna.
- **768–1023 px**: navegación visible si cabe; beneficios adaptables; hero puede
  seguir apilado.
- **≥1024 px**: hero en dos columnas, ancho máximo legible y beneficios en grid.
- Las secciones admiten zoom al 200 %, texto largo y navegación sin hover.

## Accesibilidad

- `header`, `nav`, `main`, secciones etiquetadas y `footer` semánticos.
- Un `h1`; títulos en orden y nombres de anclas descriptivos.
- Logo decorativo dentro de `Brand`; la palabra FORMA mantiene el nombre.
- Todos los botones son `<button>` y todas las navegaciones son enlaces reales.
- Labels visibles, autocomplete actual, errores asociados, busy anunciado y foco
  no trasladado de forma sorpresiva.
- El selector de tema comunica acción/estado; foco visible con el patrón global.
- Contraste AA en claro y oscuro; la información no depende solo del color.

