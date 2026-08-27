/**
 * Cómo escribe FORMA un número que es una medida: kilos, gramos, kilocalorías,
 * porcentajes, índices.
 *
 * <p><b>Punto decimal, en toda la aplicación.</b> Estaba a medias: las páginas
 * de mediciones y progreso escribían «74.0» con `toFixed`, mientras nutrición,
 * el generador y las fichas del panel pasaban por `Intl` en `es-ES` y escribían
 * «74,0». Dos separadores en la misma pantalla se leen como un fallo, y la
 * tarjeta de tendencia los ponía uno al lado del otro. Este módulo es el único
 * sitio donde se decide, para que no vuelva a divergir.
 *
 * <p><b>Sin separador de miles.</b> No es un detalle suelto: si el punto separa
 * decimales no puede separar también grupos. `es-ES` escribe 12000 como
 * «12.000», que junto a «74.0» se lee como doce con tres decimales. Ninguna
 * medida de esta aplicación necesita agrupar para leerse — el mayor kcal diario
 * son cuatro cifras.
 *
 * <p>Esto es formato, no dominio: aquí no se redondea para decidir nada ni se
 * deriva ningún valor (ADR-006), sólo se escribe el número que llega.
 *
 * <p>El dinero también, con `euro`. Lo que NO pasa por aquí son las fechas:
 * siguen en `es-ES` porque el idioma sí es castellano. Lo que cambia es el
 * separador de los números, no la lengua.
 */
const LOCALE = 'en-US';

/**
 * `Intl.NumberFormat` no es barato de construir y estas funciones se llaman en
 * cada render, así que cada combinación se construye una vez.
 */
const formatters = new Map<string, Intl.NumberFormat>();

function formatterFor(options: Intl.NumberFormatOptions): Intl.NumberFormat {
  const key = JSON.stringify(options);
  const cached = formatters.get(key);
  if (cached) {
    return cached;
  }
  const formatter = new Intl.NumberFormat(LOCALE, { useGrouping: false, ...options });
  formatters.set(key, formatter);
  return formatter;
}

/**
 * Hasta `decimals` decimales, sin ceros de relleno: `38.6`, `15`, `2350`.
 * Para cifras sueltas dentro de una frase o al lado de su unidad.
 */
export function measure(value: number, decimals = 1): string {
  return formatterFor({ maximumFractionDigits: decimals }).format(value);
}

/**
 * Exactamente `decimals` decimales: `74.0`, `15.0`. Para cifras que se comparan
 * entre sí — una columna, una ficha al lado de otra —, donde «74» y «73.6»
 * juntos parecen medidos con distinta precisión.
 */
export function fixed(value: number, decimals = 1): string {
  return formatterFor({
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

/**
 * Un precio: `12.50 €`, `0.00 €`. Siempre con céntimos — «3.9 €» parece a medio
 * escribir.
 *
 * <p>Se compone a mano en vez de pedirle `style: 'currency'` a `Intl`, porque
 * el formateador de moneda trae la colocación del símbolo pegada al idioma: en
 * un locale inglés lo pondría delante y sin espacio («€12.50»), que es otra
 * convención. Aquí sólo se cambia el separador; el símbolo se queda donde lo
 * pone el castellano, detrás y separado.
 */
export function euro(value: number): string {
  return `${fixed(value, 2)} €`;
}

/**
 * Una variación, siempre con su signo: `+0.7`, `-0.5`. El cero se queda sin
 * signo, porque no ha subido ni bajado.
 */
export function change(value: number, decimals = 1): string {
  return formatterFor({
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
    signDisplay: 'exceptZero',
  }).format(value);
}
