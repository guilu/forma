import { describe, expect, it } from 'vitest';
import { displayTitle, prescriptionSummary, repTarget } from './trainingPrescription';
import type { WorkoutItem } from '../api/training';

/**
 * Prescription formatting shared by the two screens that print it: the full
 * "Entrenar" page (`TrainingDetailPage`) and the week's session detail dialog
 * (`TrainingPage`). It lives in one module precisely so the two cannot drift —
 * a session that reads "8–12" in one place and "8-12 reps" in the other is the
 * bug this file exists to prevent.
 */
function item(overrides: Partial<WorkoutItem> = {}): WorkoutItem {
  return {
    exerciseId: 'dumbbell-bench-press',
    exerciseName: 'Press de banca con mancuernas',
    order: 1,
    sets: 4,
    repScheme: 'RANGE',
    repsMin: 8,
    repsMax: 12,
    restSeconds: 90,
    rir: 2,
    ...overrides,
  };
}

describe('repTarget', () => {
  it('prints a rep range with an en dash', () => {
    expect(repTarget(item())).toBe('8–12');
  });

  it('collapses a range whose ends match into a single number', () => {
    expect(repTarget(item({ repsMin: 10, repsMax: 10 }))).toBe('10');
  });

  it('falls back to whichever end is present', () => {
    expect(repTarget(item({ repsMin: undefined, repsMax: 12 }))).toBe('12');
    expect(repTarget(item({ repsMin: 8, repsMax: undefined }))).toBe('8');
  });

  it('names AMRAP rather than inventing a number', () => {
    expect(repTarget(item({ repScheme: 'AMRAP', repsMin: undefined, repsMax: undefined }))).toBe(
      'AMRAP',
    );
  });

  it('prints a timed hold in seconds', () => {
    expect(
      repTarget(
        item({
          repScheme: 'TIME_HOLD',
          repsMin: undefined,
          repsMax: undefined,
          durationSecondsMin: 45,
          durationSecondsMax: 75,
        }),
      ),
    ).toBe('45–75 s');
  });
});

describe('prescriptionSummary', () => {
  /*
   * The one-line form the session dialog prints under each exercise name. The
   * unit word belongs to the RANGE scheme only: "AMRAP reps" and "45–75 s reps"
   * are both nonsense, so the word is attached where it means something rather
   * than appended blindly.
   */
  it('reads sets, reps, RIR and rest for a rep range', () => {
    expect(prescriptionSummary(item())).toBe('4 series · 8–12 reps · RIR 2 · Desc 90 s');
  });

  it('drops the "reps" unit for AMRAP', () => {
    expect(
      prescriptionSummary(
        item({
          repScheme: 'AMRAP',
          sets: 3,
          repsMin: undefined,
          repsMax: undefined,
          restSeconds: 60,
          rir: 1,
        }),
      ),
    ).toBe('3 series · AMRAP · RIR 1 · Desc 60 s');
  });

  it('drops the "reps" unit for a timed hold', () => {
    expect(
      prescriptionSummary(
        item({
          repScheme: 'TIME_HOLD',
          sets: 3,
          repsMin: undefined,
          repsMax: undefined,
          durationSecondsMin: 45,
          durationSecondsMax: 75,
          restSeconds: 45,
        }),
      ),
    ).toBe('3 series · 45–75 s · RIR 2 · Desc 45 s');
  });

  it('says "1 serie" in the singular', () => {
    expect(prescriptionSummary(item({ sets: 1 }))).toBe('1 serie · 8–12 reps · RIR 2 · Desc 90 s');
  });
});

describe('displayTitle', () => {
  it('strips the redundant kind prefix the dialog already shows', () => {
    expect(displayTitle('Fuerza · Empuje')).toBe('Empuje');
  });

  it('leaves a title without the prefix alone', () => {
    expect(displayTitle('Tirada larga')).toBe('Tirada larga');
  });
});
