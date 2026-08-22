import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/Card';
import {
  generatePlanDraft,
  getEnergyRequirement,
  type EnergyRequirement,
} from '../../api/planGenerator';
import { ApiRequestError } from '../../api/client';
import { canCalculate, EMPTY_FUNNEL, type FunnelState } from './funnelState';
import { Stepper } from './GeneratorChrome';
import { PlanReady } from './PlanReady';
import { StepClinical } from './StepClinical';
import { StepContact } from './StepContact';
import { StepPatient } from './StepPatient';
import { StepPreferences } from './StepPreferences';
import styles from './PlanGenerator.module.css';

/**
 * El generador de plan de la portada: cuatro pasos y una pantalla final.
 *
 * <p>Público, sin sesión. Es el embudo: alguien que no conoce FORMA responde cuatro
 * pantallas y recibe un plan. Pedirle cuenta antes de enseñarle nada lo vaciaría de
 * sentido, así que los dos endpoints que hay detrás son los únicos de toda la API que
 * responden a un visitante anónimo.
 *
 * <p><b>Aquí no se calcula nada.</b> El requerimiento energético lo trabaja el servidor
 * y esta pantalla lo pinta: Mifflin-St Jeor tiene que existir en el backend para generar
 * el plan de verdad, y escribirlo también en React lo dejaría libre de separarse — el
 * número que convence a alguien dejaría de ser el número con el que se construye su plan.
 *
 * <p><b>Lo que NO se pregunta.</b> Patologías, alergias y restricciones alimentarias van
 * con candado, igual que en el diseño de partida. Se enseña que existen y no se piden:
 * son datos de salud, y recogerlos en un formulario público para no usarlos todavía sería
 * lo peor de las dos opciones.
 */
export function PlanGeneratorPage() {
  const [step, setStep] = useState(0);
  const [state, setState] = useState<FunnelState>(EMPTY_FUNNEL);
  const [energy, setEnergy] = useState<EnergyRequirement | undefined>(undefined);
  const [accepted, setAccepted] = useState<{ readonly email: string } | undefined>(undefined);
  const [error, setError] = useState<string | undefined>(undefined);
  const [pending, setPending] = useState(false);

  const patch = useCallback(
    (change: Partial<FunnelState>) => setState((current) => ({ ...current, ...change })),
    [],
  );

  /*
   * Al cambiar de paso, el foco se va al paso nuevo.
   *
   * <p>Sin esto, pulsar «Siguiente» deja el foco donde estaba: React reutiliza el
   * nodo del botón, así que el foco se queda en un «Siguiente» que ahora pertenece a
   * otra pregunta, y quien navega con lector de pantalla no oye que la pantalla haya
   * cambiado. Mover el foco lo anuncia y, de paso, sube la vista — que en un móvil
   * hace falta, porque los pasos son más altos que la pantalla y el siguiente
   * empezaba a media página.
   *
   * <p>En el primer render no: entrar en una página y que te robe el foco es peor
   * que no anunciar nada.
   */
  const stepRef = useRef<HTMLDivElement>(null);
  const entered = useRef(false);
  useEffect(() => {
    if (!entered.current) {
      entered.current = true;
      return;
    }
    stepRef.current?.focus();
  }, [step]);

  // El servidor recalcula cuando cambia algo que entra en la fórmula. Con retardo: son teclas, no
  // decisiones, y una petición por pulsación sería ruido.
  useEffect(() => {
    if (!canCalculate(state)) {
      setEnergy(undefined);
      return;
    }
    let active = true;
    const timer = window.setTimeout(() => {
      getEnergyRequirement({
        sex: state.sex,
        ageYears: Number(state.ageYears),
        weightKg: Number(state.weightKg),
        heightCm: Number(state.heightCm),
        activityLevel: state.activityLevel,
        objective: state.objective === '' ? undefined : state.objective,
      })
        .then((fresh) => {
          if (active) setEnergy(fresh);
        })
        .catch(() => {
          if (active) setEnergy(undefined);
        });
    }, 300);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [
    state.sex,
    state.ageYears,
    state.weightKg,
    state.heightCm,
    state.activityLevel,
    state.objective,
    state,
  ]);

  async function submit() {
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      const result = await generatePlanDraft({
        sex: state.sex,
        ageYears: Number(state.ageYears),
        weightKg: Number(state.weightKg),
        heightCm: Number(state.heightCm),
        activityLevel: state.activityLevel,
        objective: state.objective === '' ? 'MAINTENANCE' : state.objective,
        daysPerWeek: state.daysPerWeek,
        mealsPerDay: state.mealsPerDay,
        eatingStyle: state.eatingStyle,
        fullName: state.fullName.trim(),
        email: state.email.trim(),
        country: state.country,
        heardAboutUs: state.heardAboutUs === '' ? undefined : state.heardAboutUs,
        wantsMarketing: state.wantsMarketing,
        acceptsPrivacyPolicy: state.acceptsPrivacyPolicy,
      });
      setAccepted({ email: result.email });
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo generar el plan. Inténtalo de nuevo.',
      );
    } finally {
      setPending(false);
    }
  }

  if (accepted) {
    return (
      <PlanReady
        email={accepted.email}
        onRestart={() => {
          setAccepted(undefined);
          setState(EMPTY_FUNNEL);
          setStep(0);
        }}
      />
    );
  }

  return (
    <div className={styles.wrapper}>
      <Link className={styles.back} to="/">
        ← Volver al inicio
      </Link>

      {/*
       * El subtítulo («Crea tu plan personalizado en 4 pasos. Gratis.») se fue con
       * FOR-190: la barra de progreso ya dice cuántos pasos hay y por cuál vas, y
       * decirlo además en prosa era la misma frase dos veces. El h1 se queda — la
       * página necesita un encabezado, y el de cada paso es una pregunta, no un título.
       */}
      <header className={styles.header}>
        <h1 className={styles.title}>Generador de plan nutricional</h1>
        <Stepper current={step} />
      </header>

      <Card>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <div ref={stepRef} tabIndex={-1} className={styles.stepFocus}>
          {step === 0 && (
            <StepPatient state={state} energy={energy} onChange={patch} onNext={() => setStep(1)} />
          )}
          {step === 1 && (
            <StepClinical
              state={state}
              energy={energy}
              onChange={patch}
              onBack={() => setStep(0)}
              onNext={() => setStep(2)}
            />
          )}
          {step === 2 && (
            <StepPreferences
              state={state}
              onChange={patch}
              onBack={() => setStep(1)}
              onNext={() => setStep(3)}
            />
          )}
          {step === 3 && (
            <StepContact
              state={state}
              energy={energy}
              pending={pending}
              onChange={patch}
              onBack={() => setStep(2)}
              onSubmit={submit}
            />
          )}
        </div>
      </Card>

      <p className={styles.disclaimer}>
        El plan generado es orientativo y no sustituye la valoración de un profesional de la salud.
      </p>
    </div>
  );
}
