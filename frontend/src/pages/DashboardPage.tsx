import { useCallback, useEffect, useState } from 'react';
import { Icon } from '../components/Icon';
import { IconButton } from '../components/IconButton';
import { BodyWidget, type BodyState } from './dashboard/BodyWidget';
import type { MeasurementsState } from './dashboard/measurementsState';
import { TrainingWidget } from './dashboard/TrainingWidget';
import { NutritionWidget } from './dashboard/NutritionWidget';
import { NutritionSummaryWidget } from './dashboard/NutritionSummaryWidget';
import { TrendWidget } from './dashboard/TrendWidget';
import { EvolutionWidget } from './dashboard/EvolutionWidget';
import { ShoppingWidget } from './dashboard/ShoppingWidget';
import { TipWidget } from './dashboard/TipWidget';
import { PlanBanner } from './dashboard/PlanBanner';
import { WidgetSection } from './dashboard/WidgetSection';
import { getProfile } from '../api/profile';
import { getDayConsumption, getNutritionDay } from '../api/nutrition';
import type { TodayConsumptionState, TodayMenuState } from './dashboard/todayNutrition';
import { localIsoDate } from './localIsoDate';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { formatShortDate } from './dateLabel';
import styles from './DashboardPage.module.css';

/**
 * Dashboard page (FOR-19, rebuilt to the FOR-164 mockup
 * `docs/1-dashboard-1-medicion.png`). The daily entry point, composed from
 * independently-failing data regions so one source never breaks the others. The
 * the nutrition views intentionally share one date-based read here; otherwise
 * each could render a different moment after a meal is logged.
 *
 * <p>Layout mirrors the mockup: a metrics row (body-composition tiles), a second
 * row (today's training / today's menu / nutrition summary / 30-day trend), and a
 * third row (first-summary / shopping preview / tip + plan banner). Nutrition
 * renders persisted daily read models; no placeholder consumption figures
 * remain.
 */
export function DashboardPage() {
  const [today] = useState(() => new Date());
  const todayIso = localIsoDate(today);
  const [consumption, setConsumption] = useState<TodayConsumptionState>({ status: 'loading' });
  const [menu, setMenu] = useState<TodayMenuState>({ status: 'loading' });
  /*
   * Refetched, not patched in place: marking a meal changes the day's consumed
   * totals, its per-meal states and the progress bar, and the server is the one
   * that recomputes all three. Guessing them here would put the nutrition maths
   * in the browser.
   */
  const reloadConsumption = useCallback(
    () =>
      getDayConsumption(todayIso).then((current) => {
        setConsumption({ status: 'ready', consumption: current });
        return current;
      }),
    [todayIso],
  );

  useEffect(() => {
    let active = true;
    getDayConsumption(todayIso)
      .then((current) => {
        if (!active) return;
        setConsumption({ status: 'ready', consumption: current });
        if (!current.dayType) {
          setMenu({ status: 'empty' });
          return;
        }
        getNutritionDay(current.dayType.toLowerCase())
          .then((day) => {
            if (active)
              setMenu(day.meals.length > 0 ? { status: 'ready', day } : { status: 'empty' });
          })
          .catch(() => {
            if (active) setMenu({ status: 'error' });
          });
      })
      .catch(() => {
        if (active) {
          setConsumption({ status: 'error' });
          setMenu({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [todayIso]);
  // Greeting name comes from the profile (FOR-169): on an empty first-run there
  // is no saved profile, so the greeting stays generic ("Hola 👋") rather than
  // leaking a seeded persona. A failed/absent profile also falls back to generic.
  const [name, setName] = useState<string | undefined>(undefined);
  useEffect(() => {
    let active = true;
    getProfile()
      .then((profile) => {
        if (active) setName(profile.name?.trim() || undefined);
      })
      .catch(() => {
        // Greeting stays generic if the profile can't load.
      });
    return () => {
      active = false;
    };
  }, []);

  /*
   * The measurement history is fetched here rather than inside BodyWidget: the
   * header's navigator and the tiles are two views of one selection, and a
   * selection cannot be shared between components that each fetch their own
   * copy (FOR-189).
   */
  const [history, setHistory] = useState<BodyMeasurement[] | undefined>(undefined);
  const [failed, setFailed] = useState(false);
  const [selected, setSelected] = useState(0);
  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) setHistory(measurements);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
    };
  }, []);

  /*
   * Una sola lectura del historial, repartida a los tres que lo pintan. `TrendWidget` y
   * `EvolutionWidget` se la pedían cada uno por su cuenta: tres `listBodyMeasurements()`
   * idénticas por carga del panel, con un limitador delante que convierte las últimas de la
   * ráfaga en 429. Ver `measurementsState.ts`.
   */
  const measurements: MeasurementsState = failed
    ? { status: 'error' }
    : history === undefined
      ? { status: 'loading' }
      : { status: 'ready', history };

  const body: BodyState = failed
    ? { status: 'error' }
    : history === undefined
      ? { status: 'loading' }
      : history.length === 0
        ? { status: 'empty' }
        : { status: 'ready', history, selected: Math.min(selected, history.length - 1) };

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>{name ? `Hola ${name} 👋` : 'Hola 👋'}</h1>
          <p className={styles.subtitle}>Este es tu resumen</p>
        </div>
        {/*
          Steps through the dates that actually have a measurement, not through
          the calendar: a day with nothing recorded has nothing to show. Absent
          entirely with no measurements — a navigator over an empty history is a
          control that cannot do anything.
        */}
        {body.status === 'ready' && (
          <div className={styles.dateNav}>
            <IconButton
              variant="ghost"
              size="sm"
              label="Medición anterior"
              // `history` is newest-first, so "previous" walks the index up.
              disabled={body.selected >= body.history.length - 1}
              onClick={() => setSelected((index) => index + 1)}
            >
              <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
            </IconButton>
            <span className={styles.date}>
              {formatShortDate(new Date(body.history[body.selected].measuredAt))}
            </span>
            <IconButton
              variant="ghost"
              size="sm"
              label="Medición siguiente"
              disabled={body.selected <= 0}
              onClick={() => setSelected((index) => index - 1)}
            >
              <Icon name="chevron" size={16} />
            </IconButton>
          </div>
        )}
      </header>

      <WidgetSection id="metrics-row-title" title="Resumen de hoy" titleHidden surface={false}>
        <div className={styles.todayGrid}>
          <BodyWidget state={body} />
          <NutritionSummaryWidget state={consumption} />
          <TrainingWidget date={today} />
          <NutritionWidget
            menu={menu}
            dateIso={todayIso}
            onMealToggled={reloadConsumption}
            consumption={consumption.status === 'ready' ? consumption.consumption : undefined}
          />
          <TrendWidget state={measurements} />
          <div className={styles.rowThree}>
            {/* Evolución takes two tracks: it inherited the width the retired
                "Tu progreso" card left behind, and a chart is what actually uses
                it (see DashboardPage.module.css `.rowThreeWide`). */}
            <EvolutionWidget measurements={measurements} />
            <ShoppingWidget />
            <div className={styles.tipColumn}>
              <TipWidget />
              <PlanBanner />
            </div>
          </div>
        </div>
      </WidgetSection>
    </div>
  );
}
