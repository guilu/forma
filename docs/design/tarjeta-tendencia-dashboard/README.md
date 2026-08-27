# Tarjeta «Tendencia 30 días» del panel

Canvas de diseño de la tarjeta **Tendencia 30 días**: hoy superpone peso (kg),
grasa (%) y músculo (kg) en una misma caja y la gráfica sale ilegible. Aquí está
por qué, y tres formas de arreglarlo.

Canvas publicado: <https://claude.ai/code/artifact/70cf3ff6-5701-425c-8e1e-5b6e4ab0806a>

## Qué hay aquí

Los ficheros de este directorio son la **fuente**; el enlace de arriba es el
canvas montado a partir de ellos. Cada `.dc.html` es un artboard.

| Fichero | Artboard |
|---|---|
| `Actual.dc.html` | La tarjeta de hoy, con su normalización por serie |
| `Main.dc.html` | **A — propuesta**: tres series apiladas, cada una con su eje |
| `OpcionB.dc.html` | **B**: las tres líneas indexadas a % sobre el primer valor |
| `OpcionC.dc.html` | **C**: barras divergentes de variación, sin eje de tiempo |
| `canvas.json` | Posiciones, páginas y notas del canvas |

Las medidas no son inventadas: la tarjeta se dibuja a los 318 × 413 px que mide
en el panel a 1680, con los tokens de `theme.css` y la caja de `WidgetSection`.
La curva es cúbica monótona (Fritsch-Carlson), que es la misma que dibuja
`type="monotone"` de Recharts, así que la forma es la que saldrá en producción.

Los datos son de muestra: 16 mediciones cada dos días, peso 75,6 → 74,0 kg,
grasa 16,8 → 15,0 %, músculo 62,1 → 62,9 kg. Elegidos para enseñar el caso
interesante — recomposición, con el peso casi quieto y el reparto moviéndose.

## Los dos problemas

**Uno: no hay un eje, hay tres.** `MultiLineChart.normalize()` reescala cada
serie contra su propio mínimo y máximo y las pinta en la misma caja. Eso hace
dos cosas malas a la vez: estira 1,7 kg de peso y 1,8 puntos de grasa a la misma
altura, así que el ruido de la báscula dibuja la misma montaña que una bajada
real; e insinúa correlaciones entre líneas que no comparten escala. Para
magnitudes con unidades distintas sólo hay dos arreglos honestos: una gráfica
por serie, o indexarlas todas a una base común.

**Dos: dos de los tres colores son el mismo color.** El verde de acento
(`#63e662`) y el ámbar de grasa (`#ffab70`) están a ΔE 0,8 en deuteranopía; el
umbral para distinguir dos series adyacentes es 8. No se arregla repintando:
ninguna combinación de los tonos que ya usa el panel pasa el umbral, y además
peso=verde / grasa=ámbar / músculo=azul es la convención de toda la pantalla
(las casillas de arriba, los aros de nutrición). La salida es que esas dos
líneas dejen de compartir caja.

## La propuesta (A)

Una gráfica por métrica, apiladas, cada una con su escala real, su unidad, su
valor de hoy y su variación absoluta. El problema del color se cae solo: dos
tonos que chocan ya no coinciden nunca en la misma caja. Y aparecen los valores
absolutos, que hoy no están en ninguna parte de esta tarjeta.

El dibujo pasa de 140 px de trazo a 210, y el hueco vacío de dos tercios de
tarjeta desaparece.

A cambio, comparar la **forma** de dos métricas obliga a mover el ojo entre dos
cajas, y cada gráfica es más baja.

## Lo que costó implementarlo

`MultiLineChart` se borra entero — nadie más lo usaba — y `TrendWidget` compone
tres `LineChart` en su variante `spark`, que ya existía para las fichas de
métrica. Su altura pasa a ser una variable (`--spark-height`) porque 44 px es lo
que le toca dentro de una ficha y aquí hay sitio para 70.

**Las fechas cambiaron de significado, y no estaba previsto en la maqueta.** El
maquetado ponía «28 jul / 27 ago», los bordes de la ventana. Al implementarlo
salió que la tarjeta llevaba tiempo diciendo dos cosas a la vez: etiquetaba la
ventana entera mientras el trazo abarcaba sólo las mediciones que cayeran
dentro. Con una semana de historial, eso es una semana pintada de borde a borde
bajo un par de fechas que anuncian un mes.

Fijar el eje a la ventana (que fue el primer intento) lo arregla y deja las
líneas apelotonadas en el cuarto derecho: honesto e ilegible. Así que las tres
filas comparten un eje fijado al tramo que **de verdad** cubren las mediciones,
y las fechas dicen ese tramo. La ventana la sigue diciendo el título.

Compartir el eje entre las tres no es opcional: si a una métrica le faltan
mediciones en algunas fechas, sin fijarlo dibujaría la misma quincena a otro
ancho que sus vecinas, y la comparación entre filas —que es todo el argumento de
esta opción— se cae.

Lo que **no** se implementó de la maqueta: el punto en el extremo de cada línea.
El valor de hoy está escrito justo encima; el punto era decoración.

La tarjeta no cambia de alto: sigue siendo la que estira la fila del panel, no
la que la marca.

## Cómo volver a tocarlo

Dos caminos, y comparten el mismo enlace:

1. **En el navegador.** Abre el canvas, edita ahí mismo y pulsa Save. Publica una
   versión nueva para todo el mundo, y los ficheros de este directorio se quedan
   atrás — vuelve a exportarlos antes de editarlos a mano.
2. **Desde aquí.** Edita los `.dc.html` / `canvas.json`, vuelve a montar el canvas
   con la skill `/design` y publica **sobre la misma URL**. Publicar sin pasar esa
   URL crea un artefacto nuevo en vez de actualizar este.

El HTML montado no se versiona: son megas de editor incrustado que se regeneran
desde estos ficheros cuando hagan falta.
