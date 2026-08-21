import { describe, expect, it } from 'vitest';
import { groupMusclesForDisplay } from './trainingMuscleLabels';

describe('groupMusclesForDisplay (FOR-53)', () => {
  it('capitalizes a raw muscle label for display', () => {
    const result = groupMusclesForDisplay([{ muscle: 'pecho', load: 'HIGH' }]);

    expect(result).toEqual([{ canonical: 'pecho', label: 'Pecho', load: 'HIGH' }]);
  });

  it('preserves accents when capitalizing', () => {
    const result = groupMusclesForDisplay([{ muscle: 'tríceps', load: 'MEDIUM' }]);

    expect(result).toEqual([{ canonical: 'tríceps', label: 'Tríceps', load: 'MEDIUM' }]);
  });

  it('groups "hombro anterior" into "hombro" (FOR-53 spec)', () => {
    const result = groupMusclesForDisplay([{ muscle: 'hombro anterior', load: 'MEDIUM' }]);

    expect(result).toEqual([{ canonical: 'hombro', label: 'Hombro', load: 'MEDIUM' }]);
  });

  it('merges "hombro" and "hombro anterior" into one group, keeping the higher load', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'hombro', load: 'MEDIUM' },
      { muscle: 'hombro anterior', load: 'HIGH' },
    ]);

    expect(result).toEqual([{ canonical: 'hombro', label: 'Hombro', load: 'HIGH' }]);
  });

  it('does not downgrade an already-HIGH group when a later entry is lower', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'hombro', load: 'HIGH' },
      { muscle: 'hombro anterior', load: 'MEDIUM' },
    ]);

    expect(result).toEqual([{ canonical: 'hombro', label: 'Hombro', load: 'HIGH' }]);
  });

  it('groups "hombro", "hombro anterior" and "hombro lateral" into one "Hombro" group, keeping the highest load (FOR-160)', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'hombro', load: 'MEDIUM' },
      { muscle: 'hombro anterior', load: 'LOW' },
      { muscle: 'hombro lateral', load: 'HIGH' },
    ]);

    expect(result).toEqual([{ canonical: 'hombro', label: 'Hombro', load: 'HIGH' }]);
  });

  it('preserves first-appearance order when "hombro lateral" introduces the group (FOR-160)', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'hombro lateral', load: 'MEDIUM' },
      { muscle: 'hombro', load: 'LOW' },
    ]);

    expect(result).toEqual([
      { canonical: 'pecho', label: 'Pecho', load: 'HIGH' },
      { canonical: 'hombro', label: 'Hombro', load: 'MEDIUM' },
    ]);
  });

  it('keeps distinct muscles as separate groups, in first-appearance order', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'tríceps', load: 'MEDIUM' },
      { muscle: 'hombro anterior', load: 'MEDIUM' },
    ]);

    expect(result).toEqual([
      { canonical: 'pecho', label: 'Pecho', load: 'HIGH' },
      { canonical: 'tríceps', label: 'Tríceps', load: 'MEDIUM' },
      { canonical: 'hombro', label: 'Hombro', load: 'MEDIUM' },
    ]);
  });

  it('returns an empty list for an empty muscle map (non-strength session)', () => {
    expect(groupMusclesForDisplay([])).toEqual([]);
  });
});

/**
 * The canonical key travels with the display group (FOR-53 legend split): the
 * session detail lists muscles in a "Frente"/"Espalda" column pair, and to
 * place a group it has to ask `viewForMuscle`, which is keyed on the canonical
 * catalog label — not on the capitalized display text.
 */
describe('groupMusclesForDisplay · canonical key', () => {
  it('carries the canonical label beside the display one', () => {
    const [group] = groupMusclesForDisplay([{ muscle: 'pecho', load: 'HIGH' }]);

    expect(group).toMatchObject({ canonical: 'pecho', label: 'Pecho', load: 'HIGH' });
  });

  it('carries the GROUP key, not the raw label, for a synonym', () => {
    // "hombro anterior" is displayed and keyed as "hombro"; keying it on the
    // raw label would fail the view lookup.
    const [group] = groupMusclesForDisplay([{ muscle: 'hombro anterior', load: 'HIGH' }]);

    expect(group.canonical).toBe('hombro');
    expect(group.label).toBe('Hombro');
  });

  it('keeps the accented canonical intact', () => {
    const [group] = groupMusclesForDisplay([{ muscle: 'Tríceps', load: 'MEDIUM' }]);

    expect(group.canonical).toBe('tríceps');
  });
});
