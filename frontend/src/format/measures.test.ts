import { describe, expect, it } from 'vitest';
import { change, fixed, measure } from './measures';

describe('measure formatting', () => {
  it('separates decimals with a point', () => {
    expect(measure(38.6)).toBe('38.6');
    expect(fixed(74)).toBe('74.0');
    expect(change(-0.5)).toBe('-0.5');
  });

  /**
   * El punto es el separador decimal, así que no puede ser también el de los
   * miles: `es-ES` escribe 12000 como «12.000», que junto a «74.0» se lee como
   * doce con tres decimales. Los grupos se quitan enteros — ninguna medida de
   * esta aplicación (kcal, kg, gramos) necesita separarlos para leerse.
   */
  it('never groups thousands, which would collide with the decimal point', () => {
    expect(measure(12000)).toBe('12000');
    expect(measure(2350)).toBe('2350');
  });

  describe('measure', () => {
    it('prints up to the decimals asked for, without padding', () => {
      expect(measure(15)).toBe('15');
      expect(measure(15.04)).toBe('15');
      expect(measure(15.06)).toBe('15.1');
    });

    it('takes a decimal count', () => {
      expect(measure(2350.4, 0)).toBe('2350');
      expect(measure(15.06, 2)).toBe('15.06');
    });
  });

  describe('fixed', () => {
    /** Una columna de cifras sólo se compara si todas tienen los mismos decimales. */
    it('pads to exactly the decimals asked for', () => {
      expect(fixed(15)).toBe('15.0');
      expect(fixed(15.06)).toBe('15.1');
      expect(fixed(15, 2)).toBe('15.00');
    });
  });

  describe('change', () => {
    it('always carries the sign, so it reads as a change and not as a value', () => {
      expect(change(0.7)).toBe('+0.7');
      expect(change(-0.5)).toBe('-0.5');
    });

    /** Cero no sube ni baja: un «+0.0» diría que ha subido algo. */
    it('leaves zero unsigned', () => {
      expect(change(0)).toBe('0.0');
    });
  });
});
