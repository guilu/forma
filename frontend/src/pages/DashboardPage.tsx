import { useEffect, useState } from 'react';
import { Icon } from '../components/Icon';
import { WaterTracker } from '../components/WaterTracker';
import { BodyWidget, type BodyState } from './dashboard/BodyWidget';
import { CaloriesWidget } from './dashboard/CaloriesWidget';
import { TrainingWidget } from './dashboard/TrainingWidget';
import { NutritionWidget } from './dashboard/NutritionWidget';
import { MacrosWidget } from './dashboard/MacrosWidget';
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
 * three nutrition widgets intentionally share one date-based read here; otherwise
 * each could render a different moment after a meal is logged.
 *
 * <p>Layout mirrors the mockup: a metrics row (body-composition tiles +
 * calories + hydration), a second row (next training / today's menu / macros /
 * 30-day trend), and a third row (first-summary / shopping preview / tip + plan
 * banner). Nutrition and hydration render persisted daily read models; no
 * placeholder consumption figures remain.
 */
export function DashboardPage() {
  const [today] = useState(() => new Date());
  const todayIso = localIsoDate(today);
  const [consumption, setConsumption] = useState<TodayConsumptionState>({ status: 'loading' });
  const [menu, setMenu] = useState<TodayMenuState>({ status: 'loading' });
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
            <button
              type="button"
              className={styles.dateArrow}
              aria-label="Medición anterior"
              // `history` is newest-first, so "previous" walks the index up.
              disabled={body.selected >= body.history.length - 1}
              onClick={() => setSelected((index) => index + 1)}
            >
              <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
            </button>
            <span className={styles.date}>
              {formatShortDate(new Date(body.history[body.selected].measuredAt))}
            </span>
            <button
              type="button"
              className={styles.dateArrow}
              aria-label="Medición siguiente"
              disabled={body.selected <= 0}
              onClick={() => setSelected((index) => index - 1)}
            >
              <Icon name="chevron" size={16} />
            </button>
          </div>
        )}
      </header>

      <WidgetSection id="metrics-row-title" title="Resumen de hoy" titleHidden surface={false}>
        <div className={styles.metrics}>
          <BodyWidget state={body} />
          <CaloriesWidget state={consumption} />
          <WaterTracker date={todayIso} />
        </div>
      </WidgetSection>

      <div className={styles.rowFour}>
        <TrainingWidget date={today} />
        <NutritionWidget
          menu={menu}
          consumption={consumption.status === 'ready' ? consumption.consumption : undefined}
        />
        <MacrosWidget state={consumption} />
        <TrendWidget />
      </div>

      <div className={styles.rowThree}>
        {/* Evolución takes two tracks: it inherited the width the retired
            "Tu progreso" card left behind, and a chart is what actually uses
            it (see DashboardPage.module.css `.rowThreeWide`). */}
        <EvolutionWidget />
        <ShoppingWidget />
        <div className={styles.tipColumn}>
          <TipWidget />
          <PlanBanner />
        </div>
      </div>
    </div>
  );
}
