# Entrenamiento sin scroll

Canvas de diseño de la página de Entrenamiento: **que toda la información se vea
de un vistazo, sin scroll, a 1440×900**.

Canvas publicado: <https://claude.ai/code/artifact/0bd07429-5d88-4dbe-ac65-842dd9aee525>

## Qué hay aquí

Los ficheros de este directorio son la **fuente**; el enlace de arriba es el
canvas montado a partir de ellos. Cada `.dc.html` es un artboard.

| Fichero | Artboard |
|---|---|
| `OpcionC.dc.html` | **Dirección elegida** — día de fuerza |
| `CarreraHoy.dc.html` | Dirección C, día de carrera |
| `DescansoHoy.dc.html` | Dirección C, día de descanso |
| `SinPlan.dc.html` | Dirección C, sin plan generado todavía |
| `Tablet.dc.html` | 1024×768, con el carril de iconos |
| `Movil.dc.html` | 390×844 |
| `Main.dc.html` | Opción A · Rejilla densa (descartada) |
| `OpcionB.dc.html` | Opción B · Hoy manda (descartada) |
| `canvas.json` | Posiciones, páginas y notas del canvas |
| `body-*.png` | Siluetas de `frontend/src/assets/anatomy/male`, reducidas a 320px |

Las dos descartadas se conservan a propósito: la razón por la que se eligió la C
no se entiende sin ver contra qué competía.

## La dirección elegida

La semana deja de ser una tarjeta y pasa a ser la página: siete columnas a lo
ancho, y la de hoy se expande dentro de la propia fila en vez de vivir aparte.
Debajo, una franja de KPIs.

Lo que resuelve es una duplicación, no un problema de espacio: «tarjeta de hoy»
y «calendario semanal» eran la misma información contada dos veces, y el `1 / 6`
de sesiones aparecía en tres sitios distintos (Resumen semanal, el tile
«Sesiones completadas» y Distribución semanal).

## Datos que no existen

Al leer el código para dibujar esto salieron dos cosas que conviene no olvidar:

- **Volumen total, Duración total y Calorías estimadas son inventados.** Están
  fijos en `const PLACEHOLDER` (`frontend/src/pages/TrainingPage.tsx`) y ningún
  endpoint los devuelve. La dirección C los elimina.
- **`TrainingSession` no lleva lista de ejercicios** (`frontend/src/api/training.ts`):
  solo `id`, `kind`, `title`, `detail`, `status`, `notes`, `workoutType`,
  `bodyView`. Cualquier diseño que muestre ejercicios exige backend nuevo. El
  artboard de la opción B lo marcaba en ámbar por eso.

## Cómo volver a tocarlo

Dos caminos, y comparten el mismo enlace:

1. **En el navegador.** Abre el canvas, edita ahí mismo (selección, panel de
   propiedades, texto en línea) y pulsa Save. Publica una versión nueva para
   todo el mundo. Si lo haces así, los ficheros de este directorio se quedan
   atrás — vuelve a exportarlos antes de editarlos a mano.
2. **Desde aquí.** Edita los `.dc.html` / `canvas.json`, vuelve a montar el
   canvas con la skill `/design` y publica **sobre la misma URL**. Publicar sin
   pasar esa URL crea un artefacto nuevo en vez de actualizar este.

El editor va incrustado en la página publicada y no se actualiza solo: es una
preview temprana de Claude Design, no tiene paridad con claude.ai/design.
