import { describe, expect, it } from 'vitest';
import { shortUnitLabel, splitProductName, unitLabel } from './shoppingDisplay';

describe('shortUnitLabel', () => {
  /* Solo se acortan las dos palabras largas: g, kg y L ya son abreviaturas, y
     el mismo valor escrito de dos formas sería peor que una palabra larga. */
  it('shortens only the units whose label is a word', () => {
    expect(shortUnitLabel('UD')).toBe('u');
    expect(shortUnitLabel('PAQUETE')).toBe('paq.');
    expect(shortUnitLabel('G')).toBe(unitLabel('G'));
    expect(shortUnitLabel('KG')).toBe(unitLabel('KG'));
    expect(shortUnitLabel('L')).toBe(unitLabel('L'));
  });

  it('falls back to the raw value for a unit the frontend does not know', () => {
    expect(shortUnitLabel('DOCENA')).toBe('DOCENA');
  });
});

describe('splitProductName', () => {
  it('takes the first word as the head and the rest as its qualifier', () => {
    expect(splitProductName('Atún claro al natural Hacendado')).toEqual({
      head: 'Atún',
      rest: 'claro al natural Hacendado',
    });
  });

  /* Un nombre de una palabra no tiene calificador, y una segunda línea vacía
     bajo él sería una fila más alta sin nada dentro. */
  it('leaves no qualifier for a single-word name', () => {
    expect(splitProductName('Avena')).toEqual({ head: 'Avena', rest: '' });
  });

  it('ignores the whitespace around a name', () => {
    expect(splitProductName('  Leche entera  ')).toEqual({ head: 'Leche', rest: 'entera' });
  });
});
