# Plan de implementación — FOR-185 landing pública

## Decisión

Entregar una landing pública en `/` y mover el árbol protegido actual a `/app`,
reutilizando FOR-145d y el sistema visual existente. `docs/0-landing.html` aporta
la composición, NO una implementación ni una fuente de verdad de producto.

## Estrategia de rama y PR

1. Trabajar en `feature/FOR-185-public-landing`, creada desde
   `feature/FOR-145d-frontend-auth-state`.
2. Abrir el PR de FOR-185 contra `feature/FOR-145d-frontend-auth-state` para que
   el diff excluya FOR-145d.
3. Cuando se fusione #167, actualizar/retargetear la base a `main`.
4. Antes de review final, verificar que el diff contra `main` solo contiene
   FOR-185 y que los tests siguen verdes.

## Unidades de trabajo TDD

### 1. Reubicar el árbol protegido

- Escribir primero pruebas de `/`, `/app`, subrutas, destino con query y rutas
  públicas excepcionales.
- Mover `AppShell` a `/app` y actualizar navegación/redirecciones controladas.
- Mantener el guard y el callback `/auth` sin duplicación.

**Fin verificable:** la landing placeholder es pública y todos los destinos
internos existentes funcionan bajo `/app`.

### 2. Integrar auth en la landing

- Escribir pruebas por estado de `useAuth` y flujo de submit.
- Reusar `TextField`, `Button` y el método `login` del provider.
- Ajustar defaults de login/registro a `/app`, preservando destinos válidos.
- Mostrar acceso a `/app` cuando ya hay sesión; no bloquear la landing por
  bootstrap error.

**Fin verificable:** login, registro, bootstrap y logout conservan el contrato de
FOR-145d desde la nueva topología.

### 3. Construir la composición pública

- Escribir pruebas de estructura y contenido respaldado.
- Crear navbar, hero, beneficios, showcase, CTA y footer como secciones pequeñas.
- Usar marca/assets locales; eliminar del diseño todo claim o link ficticio.
- Añadir selector de tema con el hook actual.

**Fin verificable:** ambas variantes de sesión presentan una landing completa,
sin recursos remotos ni controles falsos.

### 4. Responsive, accesibilidad y SEO

- Añadir pruebas axe para los estados importantes.
- Implementar CSS Modules solo con tokens y breakpoints mínimos.
- Verificar teclado, 320 px, zoom, reduced motion y ambos temas.
- Actualizar título y meta description en `frontend/index.html` siguiendo el
  patrón Vite existente.

**Fin verificable:** axe, smoke responsive y SEO básico pasan sin ampliar el
stack.

### 5. Verificación final

```bash
cd frontend
npm test
npm run typecheck
npm run lint
npm run format:check
npm run build
cd ..
git diff --check
```

Realizar smoke de destino preservado, login, `/app`, logout, tema y navegación
por teclado. Confirmar ausencia de requests remotos.

## Archivos probables

### Crear

- `frontend/src/pages/LandingPage.tsx`
- `frontend/src/pages/LandingPage.module.css`
- `frontend/src/pages/LandingPage.test.tsx`

### Modificar

- `frontend/src/app/routes.tsx` y sus pruebas.
- `frontend/src/app/navigation.ts` y pruebas/enlaces con paths absolutos.
- `frontend/src/pages/LoginPage.tsx` y `RegisterPage.tsx` para default `/app`.
- Tests de auth/rutas/shell afectados por el nuevo prefijo.
- `frontend/index.html` para título y description.

La lista es orientativa: buscar referencias reales antes de editar y evitar
tocar componentes internos si solo cambia su ruta padre.

## Review Workload Forecast

| Señal | Previsión |
|---|---|
| Archivos | 10–18 |
| Líneas cambiadas | 500–900 |
| Riesgo sobre presupuesto de 400 líneas | Alto |
| Chained PRs recommended | Yes |
| Decision needed before apply | Yes |

### Frontera recomendada de revisión

Dividir FOR-185 en dos PRs encadenados si la implementación supera 400 líneas:

1. **Routing/auth integration**: `/app`, destinos, defaults y regresiones de
   autenticación. Base inicial: FOR-145d.
2. **Landing UI**: estructura, estilos, contenido, a11y y SEO. Base: PR anterior.

Cada PR debe ser autónomo, verde y reversible. Si mantenimiento exige un solo PR,
registrar `size:exception` antes de implementar y mantener commits separados en
esas mismas dos unidades. NO mezclar un rediseño del shell para “aprovechar” el
cambio.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Paths antiguos quedan embebidos | Búsqueda global + tests parametrizados de todas las rutas. |
| Landing bloqueada por bootstrap | El contenido público no depende del guard; probar loading/error. |
| Duplicación del flujo login | Usar exclusivamente `useAuth`; no API directa. |
| Template introduce ficción/dependencias | Lista explícita de descartes y auditoría de URLs/claims. |
| Contraste o layout falla en light/mobile | Axe + smoke en ambos temas y 320 px. |
| Diff apilado contamina el PR | Base FOR-145d hasta merge #167; retarget y revisar diff contra main. |

