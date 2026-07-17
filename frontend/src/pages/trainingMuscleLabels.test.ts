import { describe, expect, it } from 'vitest';
import { groupMusclesForDisplay } from './trainingMuscleLabels';

describe('groupMusclesForDisplay (FOR-53)', () => {
  it('capitalizes a raw muscle label for display', () => {
    const result = groupMusclesForDisplay([{ muscle: 'pecho', load: 'HIGH' }]);

    expect(result).toEqual([{ label: 'Pecho', load: 'HIGH' }]);
  });

  it('preserves accents when capitalizing', () => {
    const result = groupMusclesForDisplay([{ muscle: 'tríceps', load: 'MEDIUM' }]);

    expect(result).toEqual([{ label: 'Tríceps', load: 'MEDIUM' }]);
  });

  it('groups "hombro anterior" into "hombro" (FOR-53 spec)', () => {
    const result = groupMusclesForDisplay([{ muscle: 'hombro anterior', load: 'MEDIUM' }]);

    expect(result).toEqual([{ label: 'Hombro', load: 'MEDIUM' }]);
  });

  it('merges "hombro" and "hombro anterior" into one group, keeping the higher load', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'hombro', load: 'MEDIUM' },
      { muscle: 'hombro anterior', load: 'HIGH' },
    ]);

    expect(result).toEqual([{ label: 'Hombro', load: 'HIGH' }]);
  });

  it('does not downgrade an already-HIGH group when a later entry is lower', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'hombro', load: 'HIGH' },
      { muscle: 'hombro anterior', load: 'MEDIUM' },
    ]);

    expect(result).toEqual([{ label: 'Hombro', load: 'HIGH' }]);
  });

  it('keeps distinct muscles as separate groups, in first-appearance order', () => {
    const result = groupMusclesForDisplay([
      { muscle: 'pecho', load: 'HIGH' },
      { muscle: 'tríceps', load: 'MEDIUM' },
      { muscle: 'hombro anterior', load: 'MEDIUM' },
    ]);

    expect(result).toEqual([
      { label: 'Pecho', load: 'HIGH' },
      { label: 'Tríceps', load: 'MEDIUM' },
      { label: 'Hombro', load: 'MEDIUM' },
    ]);
  });

  it('returns an empty list for an empty muscle map (non-strength session)', () => {
    expect(groupMusclesForDisplay([])).toEqual([]);
  });
});
