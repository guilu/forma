import { describe, expect, it } from 'vitest';
import { overlayFromMuscleMap, MUSCLE_CODES_BY_VIEW } from './trainingMuscleOverlay';

/**
 * The catalog speaks Spanish and the silhouette pack speaks codes, so this is
 * the translation between them (see `trainingMuscleLabels.ts` for the sibling
 * that handles the *label* side of the same read model).
 *
 * <p>The mapping is deliberately partial. Six of the pack's codes — the two
 * forearms, adductors, the front lower leg, the lower back and the soleus —
 * have no counterpart in the exercise catalog, and four of them stay dark
 * rather than being approximated onto a neighbour.
 */
describe('overlayFromMuscleMap', () => {
  it('translates catalog labels into pack codes', () => {
    const overlay = overlayFromMuscleMap([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'dorsal', load: 'HIGH' },
      { muscle: 'cuádriceps', load: 'HIGH' },
    ]);

    expect(overlay).toMatchObject({
      PECTORAL: 'primary',
      LATS: 'primary',
      QUADRICEPS: 'primary',
    });
  });

  it('maps load onto emphasis: HIGH is primary, the rest secondary', () => {
    const overlay = overlayFromMuscleMap([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'tríceps', load: 'MEDIUM' },
      { muscle: 'gemelos', load: 'LOW' },
    ]);

    expect(overlay.PECTORAL).toBe('primary');
    expect(overlay.TRICEPS).toBe('secondary');
    expect(overlay.CALVES).toBe('secondary');
  });

  it('folds every shoulder synonym onto the front deltoid', () => {
    for (const label of ['hombro', 'hombro anterior', 'hombro lateral']) {
      expect(overlayFromMuscleMap([{ muscle: label, load: 'HIGH' }])).toMatchObject({
        DELTOID_FRONT: 'primary',
      });
    }
  });

  it('lights both abdominals and obliques for the catalog "core"', () => {
    const overlay = overlayFromMuscleMap([{ muscle: 'core', load: 'HIGH' }]);

    expect(overlay).toMatchObject({ ABS: 'primary', OBLIQUES: 'primary' });
  });

  it('keeps the strongest role when two labels land on the same code', () => {
    // "abdomen" and "core" both reach ABS; the harder load has to win, or a
    // session that works the core hard would render it as an afterthought.
    const overlay = overlayFromMuscleMap([
      { muscle: 'abdomen', load: 'LOW' },
      { muscle: 'core', load: 'HIGH' },
    ]);

    expect(overlay.ABS).toBe('primary');
  });

  it('ignores labels the pack has no muscle for, rather than guessing', () => {
    const overlay = overlayFromMuscleMap([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'antebrazo', load: 'HIGH' },
      { muscle: 'no-existe', load: 'HIGH' },
    ]);

    expect(Object.keys(overlay)).toEqual(['PECTORAL']);
  });

  it('is case and whitespace insensitive, like the label helper beside it', () => {
    expect(overlayFromMuscleMap([{ muscle: '  Pecho  ', load: 'HIGH' }])).toMatchObject({
      PECTORAL: 'primary',
    });
  });

  it('returns nothing for an empty map, which is what a run or a rest day sends', () => {
    expect(overlayFromMuscleMap([])).toEqual({});
  });

  it('files every mapped code under exactly one view', () => {
    const front = new Set(MUSCLE_CODES_BY_VIEW.front);
    const back = new Set(MUSCLE_CODES_BY_VIEW.back);

    for (const code of [...front]) {
      expect(back.has(code), `${code} is claimed by both views`).toBe(false);
    }
    // Spot-check the split against the pack's own manifest (muscle-map.json).
    expect(front.has('PECTORAL')).toBe(true);
    expect(back.has('LATS')).toBe(true);
  });
});
