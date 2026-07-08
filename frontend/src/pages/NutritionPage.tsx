import { useEffect, useState } from 'react';
import { Card } from '../components/Card';
import { getNutritionDay, type NutritionDay, type NutritionMeal } from '../api/nutrition';
import styles from './NutritionPage.module.css';

/**
 * Nutrition page (FOR-34). Shows the running-day meal flow from the FOR-33 seeded template via the
 * nutrition API: breakfast → lunch → pre-run snack → (run) → optional post-run recovery → light
 * dinner, with a short explanation that carbs are front-loaded and dinner stays light after a late
 * run. Renders the API read model directly (ADR-006); handles loading and error states.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

export function NutritionPage() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getNutritionDay('running')
      .then((day) => {
        if (active) {
          setState({ status: 'ready', day });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Nutrición</h1>
        <p className={styles.subtitle}>Día de carrera: come los carbohidratos antes de correr.</p>
      </header>
      {renderContent(state)}
    </div>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu día de nutrición…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudo cargar tu día de nutrición. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  if (state.day.meals.length === 0) {
    return (
      <p className={styles.message} role="status">
        No hay comidas planificadas para este día.
      </p>
    );
  }

  return (
    <>
      <Card>
        <p className={styles.explanation}>
          Los carbohidratos se concentran temprano; la cena es más ligera tras correr por la noche.
          La recuperación post-carrera es opcional: sáltala si ya has alcanzado tu proteína diaria.
        </p>
      </Card>
      <ol className={styles.flow} aria-label="Flujo de comidas del día de carrera">
        {state.day.meals.map((meal) => (
          <li key={`${meal.mealType}-${meal.preferredTime}`}>
            <MealCard meal={meal} />
          </li>
        ))}
      </ol>
    </>
  );
}

function MealCard({ meal }: { readonly meal: NutritionMeal }) {
  return (
    <Card title={`${meal.preferredTime} · ${meal.name}${meal.optional ? ' (opcional)' : ''}`}>
      {meal.optional && (
        <span className={styles.optional} data-testid="optional-badge">
          Opcional
        </span>
      )}
      <ul className={styles.items}>
        {meal.items.map((item) => (
          <li key={item.food} className={styles.item}>
            <span className={styles.food}>{item.food}</span>
            <span className={styles.quantity}>{item.quantityG} g</span>
          </li>
        ))}
      </ul>
    </Card>
  );
}
