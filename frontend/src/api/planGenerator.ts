/**
 * El generador público de planes, en la portada.
 *
 * <p>Los dos únicos endpoints de la API que responden a alguien sin sesión, aparte
 * de entrar y registrarse — y a propósito: es el embudo, y pedir cuenta antes de
 * enseñar nada lo vaciaría de sentido.
 *
 * <p>Mifflin-St Jeor NO está aquí. El requerimiento lo calcula el servidor, que es
 * quien tiene que usar la misma fórmula para generar el plan de verdad: escrita
 * también en React, sería libre de separarse de la copia con la que se construyen
 * los planes, y el número que convence a alguien dejaría de ser el que recibe.
 */
import { apiClient, type ApiClient } from './client';

const PATH = '/api/v1/public/plan-generator';

export type Sex = 'MALE' | 'FEMALE';

export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE';

export type PlanObjective = 'WEIGHT_LOSS' | 'MUSCLE_GAIN' | 'MAINTENANCE' | 'HEALTHY_EATING';

export type EatingStyle = 'ESTANDAR_ESPANOL' | 'MEDITERRANEA';

/** Lo que hace falta para calcular. El objetivo falta mientras se está en el paso 1. */
export interface EnergyRequirementInput {
  readonly sex: Sex;
  readonly ageYears: number;
  readonly weightKg: number;
  readonly heightCm: number;
  readonly activityLevel: ActivityLevel;
  readonly objective?: PlanObjective;
}

/**
 * Las tres cifras que enseña el embudo, cada una con el paso que la produce.
 *
 * <p>Llegan separadas porque la pantalla explica la cuenta —basal, luego movimiento,
 * luego objetivo— y una explicación que el cliente tiene que reconstruir es una que
 * el cliente puede equivocar.
 */
export interface EnergyRequirement {
  readonly basalKcal: number;
  readonly activityFactor: number;
  readonly dailyKcal: number;
  readonly objectiveFactor: number;
  readonly planKcal: number;
}

/** Todo lo que recogen las cuatro pantallas. Sin patologías ni restricciones: van con candado. */
export interface PlanDraftInput extends Required<Omit<EnergyRequirementInput, 'objective'>> {
  readonly objective: PlanObjective;
  readonly daysPerWeek: number;
  readonly mealsPerDay: number;
  readonly eatingStyle: EatingStyle;
  readonly fullName: string;
  readonly email: string;
  readonly country?: string;
  readonly heardAboutUs?: string;
  readonly wantsMarketing: boolean;
  readonly acceptsPrivacyPolicy: boolean;
}

/**
 * Lo que responde el embudo al terminar.
 *
 * <p>Delgado a propósito: hoy no se genera nada ni se guarda nada. No trae identificador
 * de plan porque no hay plan.
 */
export interface PlanDraftAccepted {
  readonly email: string;
  readonly planKcal: number;
  readonly mealsPerDay: number;
}

/** El requerimiento diario, calculado por el servidor. Sin efectos: se puede pedir al teclear. */
export function getEnergyRequirement(
  input: EnergyRequirementInput,
  client: ApiClient = apiClient,
): Promise<EnergyRequirement> {
  return client.request<EnergyRequirement>(`${PATH}/energy-requirement`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

/** Entrega el embudo terminado. Hoy solo lo valida y dice que sí. */
export function generatePlanDraft(
  input: PlanDraftInput,
  client: ApiClient = apiClient,
): Promise<PlanDraftAccepted> {
  return client.request<PlanDraftAccepted>(PATH, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}
