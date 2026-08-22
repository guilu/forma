import type { ActivityLevel, EatingStyle, PlanObjective, Sex } from '../../api/planGenerator';

/**
 * Lo que el embudo va recogiendo, entre pantalla y pantalla.
 *
 * <p>Vive aquí y no junto a la página porque lo leen los cuatro pasos, y porque un
 * fichero que exporta un componente y además constantes rompe el recargado en caliente
 * de React — el aviso que lo dijo tenía razón.
 *
 * <p>Las medidas se guardan como texto, no como número: un campo a medio escribir no es
 * un número, y convertirlo antes de tiempo obliga a inventarse qué significa el vacío.
 */
export interface FunnelState {
  readonly sex: Sex;
  readonly ageYears: string;
  readonly weightKg: string;
  readonly heightCm: string;
  readonly activityLevel: ActivityLevel;
  readonly objective: PlanObjective | '';
  readonly daysPerWeek: number;
  readonly mealsPerDay: number;
  readonly eatingStyle: EatingStyle;
  readonly fullName: string;
  readonly email: string;
  readonly country: string;
  readonly heardAboutUs: string;
  readonly wantsMarketing: boolean;
  readonly acceptsPrivacyPolicy: boolean;
}

/**
 * Cómo se llama cada opción, en un sitio.
 *
 * <p>Viven aquí por el mismo motivo que el estado: los lee más de un paso. El paso 2
 * ofrece los objetivos y el 4 los resume en una pastilla; el 3 ofrece los estilos y el
 * 4 hace lo mismo. Escritos dos veces, una pantalla podría acabar llamando «Pérdida de
 * peso» a lo que la otra llama otra cosa, y el resumen dejaría de resumir.
 */
export const OBJECTIVE_LABELS: Readonly<Record<PlanObjective, string>> = {
  WEIGHT_LOSS: 'Pérdida de peso',
  MUSCLE_GAIN: 'Ganancia muscular',
  MAINTENANCE: 'Mantenimiento',
  HEALTHY_EATING: 'Comer sano',
};

export const EATING_STYLE_LABELS: Readonly<Record<EatingStyle, string>> = {
  ESTANDAR_ESPANOL: 'Estándar español',
  MEDITERRANEA: 'Mediterránea',
};

/** Un embudo sin empezar. Lo preseleccionado son los valores más comunes, no una decisión. */
export const EMPTY_FUNNEL: FunnelState = {
  sex: 'MALE',
  ageYears: '',
  weightKg: '',
  heightCm: '',
  activityLevel: 'MODERATE',
  objective: '',
  daysPerWeek: 5,
  mealsPerDay: 5,
  eatingStyle: 'ESTANDAR_ESPANOL',
  fullName: '',
  email: '',
  country: 'ES',
  heardAboutUs: '',
  wantsMarketing: true,
  acceptsPrivacyPolicy: false,
};

/**
 * Si el servidor puede calcular con lo que hay.
 *
 * <p>Los mismos límites que valida el backend, y eso es una duplicación consciente: aquí
 * evitan una petición que se sabe rechazada y allí protegen un endpoint público que
 * cualquiera puede llamar sin pasar por esta pantalla. El que manda es el del servidor.
 */
export function canCalculate(state: FunnelState): boolean {
  const age = Number(state.ageYears);
  const weight = Number(state.weightKg);
  const height = Number(state.heightCm);
  return (
    state.ageYears !== '' &&
    state.weightKg !== '' &&
    state.heightCm !== '' &&
    age >= 14 &&
    age <= 120 &&
    weight > 0 &&
    weight <= 400 &&
    height > 0 &&
    height <= 260
  );
}
