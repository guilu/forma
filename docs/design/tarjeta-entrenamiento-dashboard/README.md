# Tarjeta de entrenamiento del panel

Canvas de diseño de la tarjeta **Entrenamiento** del panel: que enseñe la
silueta con los músculos trabajados, como la página de entrenamiento, y que el
clic en cualquier punto abra el mismo diálogo de detalle.

Canvas publicado: <https://claude.ai/code/artifact/7a46d617-2499-42b4-82c7-5640baa185ae>

## Qué hay aquí

Los ficheros de este directorio son la **fuente**; el enlace de arriba es el
canvas montado a partir de ellos. Cada `.dc.html` es un artboard.

| Fichero | Artboard |
|---|---|
| `Main.dc.html` | **Dirección elegida** — día de fuerza, en reposo y bajo el puntero |
| `Estados.dc.html` | Carrera, descanso y sesión completada |
| `Modal.dc.html` | El diálogo al que lleva el clic, sin cambios |
| `OpcionB.dc.html` | Silueta al lado del texto (descartada) |
| `OpcionC.dc.html` | Silueta a sangre (descartada) |
| `canvas.json` | Posiciones, páginas y notas del canvas |
| `body-*.png` | Siluetas de `frontend/src/assets/anatomy/male`, reducidas a 320 px |
| `*.svg` | Máscaras de músculo del mismo pack, sin tocar |

Las medidas no son inventadas: la tarjeta se dibuja a los 318 px que mide en el
panel a 1680, con los tokens de `theme.css` y la caja de `WidgetSection`. Las
máscaras se pintan como en el producto — bloque de color revelado por el SVG,
no el SVG pintado —, así que el verde es `--color-accent` y sigue al tema.

## La dirección elegida

Par frente + espalda, porque es lo único que enseña entero lo que trabaja una
sesión: en un empuje el tríceps vive en la espalda, y una sola silueta lo
perdería. Debajo, la barra de progreso semanal tal cual estaba.

El clic se lleva por delante el botón «Ver plan completo»: un control dentro de
un control funciona con ratón y con nada más. Su destino sube a la cabecera como
enlace «Ver plan», que es lo que `WidgetSection` ya ofrece a todos los widgets.

Un día de descanso no es pinchable — no hay sesión que abrir, y una tarjeta que
se ilumina bajo el puntero y luego no hace nada es peor que una plana.

## Lo que costó implementarlo

La tarjeta creció de 329 a 436 px de alto (413 en la banda de tablet), y la fila
del panel estira a todas sus tarjetas por igual: ese alto lo pagan también
Nutrición, Menú y Tendencia. La palanca para recortarlo es la altura de la
silueta, hoy 200 px.

En móvil las siluetas **no** crecen aunque la tarjeta sea más ancha: a 240 px la
tarjeta se iba a 496, casi tres cuartos de una pantalla de 320×700. Lo que
escasea en un móvil es alto.

## Cómo volver a tocarlo

Dos caminos, y comparten el mismo enlace:

1. **En el navegador.** Abre el canvas, edita ahí mismo y pulsa Save. Publica una
   versión nueva para todo el mundo, y los ficheros de este directorio se quedan
   atrás — vuelve a exportarlos antes de editarlos a mano.
2. **Desde aquí.** Edita los `.dc.html` / `canvas.json`, vuelve a montar el canvas
   con la skill `/design` y publica **sobre la misma URL**. Publicar sin pasar esa
   URL crea un artefacto nuevo en vez de actualizar este.

El HTML montado no se versiona: son 2,7 MB de editor incrustado que se
regeneran desde estos ficheros cuando hagan falta.
