import { describe, expect, it } from 'vitest';
import {
  buildPlanRequestInput,
  mapCuisineStyleAnswer,
  mapDietPatternAnswer,
  mapEquipmentLabelToTrainingEquipment,
  mapWeekdayLabelToDayOfWeek,
} from './planRequestMapping';
import { EMPTY_ANSWERS, type OnboardingAnswers } from './onboardingStorage';

describe('mapWeekdayLabelToDayOfWeek', () => {
  it('maps every one of the wizard-s seven Spanish weekday labels', () => {
    expect(mapWeekdayLabelToDayOfWeek('Lunes')).toBe('MONDAY');
    expect(mapWeekdayLabelToDayOfWeek('Martes')).toBe('TUESDAY');
    expect(mapWeekdayLabelToDayOfWeek('Miércoles')).toBe('WEDNESDAY');
    expect(mapWeekdayLabelToDayOfWeek('Jueves')).toBe('THURSDAY');
    expect(mapWeekdayLabelToDayOfWeek('Viernes')).toBe('FRIDAY');
    expect(mapWeekdayLabelToDayOfWeek('Sábado')).toBe('SATURDAY');
    expect(mapWeekdayLabelToDayOfWeek('Domingo')).toBe('SUNDAY');
  });

  it('throws for a label the wizard never offers, rather than dropping a day silently', () => {
    expect(() => mapWeekdayLabelToDayOfWeek('Funday')).toThrow();
  });
});

describe('mapEquipmentLabelToTrainingEquipment', () => {
  it('maps every one of the wizard-s six equipment labels', () => {
    expect(mapEquipmentLabelToTrainingEquipment('Sin equipamiento (peso corporal)')).toBe(
      'BODYWEIGHT',
    );
    expect(mapEquipmentLabelToTrainingEquipment('Mancuernas')).toBe('DUMBBELLS');
    expect(mapEquipmentLabelToTrainingEquipment('Barra y discos')).toBe('BARBELL');
    expect(mapEquipmentLabelToTrainingEquipment('Bandas elásticas')).toBe('BANDS');
    expect(mapEquipmentLabelToTrainingEquipment('Máquinas de gimnasio')).toBe('MACHINES');
    expect(mapEquipmentLabelToTrainingEquipment('Cinta o bicicleta estática')).toBe(
      'CARDIO_MACHINE',
    );
  });

  it('throws for a label the wizard never offers', () => {
    expect(() => mapEquipmentLabelToTrainingEquipment('Kettlebell')).toThrow();
  });
});

describe('mapDietPatternAnswer', () => {
  it('passes through the four DietPattern-shaped answers verbatim', () => {
    expect(mapDietPatternAnswer('OMNIVORE')).toBe('OMNIVORE');
    expect(mapDietPatternAnswer('VEGETARIAN')).toBe('VEGETARIAN');
    expect(mapDietPatternAnswer('VEGAN')).toBe('VEGAN');
    expect(mapDietPatternAnswer('GLUTEN_FREE')).toBe('GLUTEN_FREE');
  });

  it('maps "nobody said" (blank) to UNSPECIFIED', () => {
    expect(mapDietPatternAnswer('')).toBe('UNSPECIFIED');
  });

  it('maps "Otra" (OTHER) to UNSPECIFIED — DietPattern has no equivalent value', () => {
    expect(mapDietPatternAnswer('OTHER')).toBe('UNSPECIFIED');
  });
});

describe('mapCuisineStyleAnswer', () => {
  it('passes through the two named cuisines verbatim', () => {
    expect(mapCuisineStyleAnswer('ESPANOLA')).toBe('ESPANOLA');
    expect(mapCuisineStyleAnswer('MEDITERRANEA')).toBe('MEDITERRANEA');
  });

  it('maps "nobody said" (blank) to UNSPECIFIED', () => {
    expect(mapCuisineStyleAnswer('')).toBe('UNSPECIFIED');
  });
});

describe('buildPlanRequestInput', () => {
  const BASE: OnboardingAnswers = {
    ...EMPTY_ANSWERS,
    direction: { selected: 'LOSE_FAT' },
  };

  it('returns undefined when no direction was chosen — the caller must not submit yet', () => {
    expect(buildPlanRequestInput(EMPTY_ANSWERS)).toBeUndefined();
  });

  it('maps a fully-answered wizard onto the plan-request body', () => {
    const answers: OnboardingAnswers = {
      ...BASE,
      training: { days: ['Lunes', 'Miércoles', 'Viernes'] },
      equipment: { items: ['Mancuernas', 'Barra y discos'] },
      nutrition: { preference: 'VEGAN', mealsPerDay: 4, cuisineStyle: 'MEDITERRANEA' },
    };

    expect(buildPlanRequestInput(answers)).toEqual({
      direction: 'LOSE_FAT',
      trainingDaysPerWeek: 3,
      trainingWeekdays: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
      equipment: ['DUMBBELLS', 'BARBELL'],
      mealsPerDay: 4,
      dietPattern: 'VEGAN',
      cuisineStyle: 'MEDITERRANEA',
    });
  });

  it('sends trainingDaysPerWeek 0 and omits trainingWeekdays/equipment when nobody said (ADR-015 decision 6)', () => {
    const answers: OnboardingAnswers = {
      ...BASE,
      training: { days: [] },
      equipment: { items: [] },
    };

    const input = buildPlanRequestInput(answers);

    expect(input?.trainingDaysPerWeek).toBe(0);
    expect(input?.trainingWeekdays).toBeUndefined();
    expect(input?.equipment).toBeUndefined();
  });
});
