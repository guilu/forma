import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { listFoods, type CatalogFood } from '../../api/foods';
import { listServings, type FoodServing } from '../../api/servings';
import { logMeal, type LogMealBody, type PlannedMealState } from '../../api/nutrition';
import styles from './LogMealForm.module.css';

/**
 * Recording something eaten (FOR-127, reachable at last).
 *
 * <p>Two shapes, because two things happen. Most of the time it is a food the
 * catalog knows and the macros are its business, not yours — you say what and how
 * much. Sometimes it is a plate in a restaurant nobody has ever weighed, and then
 * the only honest thing is to type the numbers off the menu.
 *
 * <p>Which is why the free entry asks for macros and the catalog one does not.
 * Offering both at once would invite typing kcal beside a food that already knows
 * its own, and then two answers would exist for one plate.
 *
 * <p>The amount is said the same three ways a plan line says it — grams, or a
 * count of a named portion, or a count of the food's default one. That is not a
 * coincidence: logging what the plan asked for should not mean translating it
 * first.
 */
interface LogMealFormProps {
  /** The day being logged, ISO. */
  readonly date: string;
  /** The plan's meals for that day, to answer one of them. Empty when there is no plan. */
  readonly plannedMeals: readonly PlannedMealState[];
  /** Preselected when the form was opened from a specific planned meal. */
  readonly plannedMealId?: string;
  readonly onCancel: () => void;
  readonly onLogged: () => void;
}

const MEAL_TYPES = [
  { value: 'BREAKFAST', label: 'Desayuno' },
  { value: 'MID_MORNING', label: 'Media mañana' },
  { value: 'LUNCH', label: 'Comida' },
  { value: 'SNACK', label: 'Merienda' },
  { value: 'PRE_WORKOUT', label: 'Pre-entreno' },
  { value: 'POST_WORKOUT', label: 'Post-entreno' },
  { value: 'DINNER', label: 'Cena' },
] as const;

type Shape = 'catalog' | 'free';

export function LogMealForm({
  date,
  plannedMeals,
  plannedMealId,
  onCancel,
  onLogged,
}: LogMealFormProps) {
  const notify = useNotify();
  const preselected = plannedMeals.find((meal) => meal.id === plannedMealId);
  const [shape, setShape] = useState<Shape>('catalog');
  const [mealType, setMealType] = useState<string>(preselected?.mealType ?? 'LUNCH');
  const [plannedMeal, setPlannedMeal] = useState(plannedMealId ?? '');
  const [foodId, setFoodId] = useState('');
  const [servingId, setServingId] = useState('');
  const [amount, setAmount] = useState('');
  const [name, setName] = useState('');
  const [kcal, setKcal] = useState('');
  const [proteinG, setProteinG] = useState('');
  const [carbsG, setCarbsG] = useState('');
  const [fatG, setFatG] = useState('');
  const [foods, setFoods] = useState<CatalogFood[]>([]);
  const [servings, setServings] = useState<FoodServing[]>([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  useEffect(() => {
    let active = true;
    listFoods()
      .then((rows) => {
        if (active) setFoods(rows);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  // A food's named portions, fetched when it is picked. Cleared with the food, because a portion
  // belongs to the food that had it: counting slices of something with no slices is nonsense.
  useEffect(() => {
    let active = true;
    setServingId('');
    if (foodId === '') {
      setServings([]);
      return;
    }
    listServings(foodId)
      .then((rows) => {
        if (active) setServings(rows);
      })
      .catch(() => {
        if (active) setServings([]);
      });
    return () => {
      active = false;
    };
  }, [foodId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      await logMeal(body());
      notify.success('Comida registrada.');
      onLogged();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo registrar la comida. Inténtalo de nuevo.',
      );
    } finally {
      setPending(false);
    }
  }

  function body(): LogMealBody {
    const common = {
      date,
      mealType,
      plannedMealId: plannedMeal === '' ? undefined : plannedMeal,
    };
    if (shape === 'free') {
      return {
        ...common,
        name: name.trim(),
        kcal: Number(kcal),
        proteinG: Number(proteinG),
        carbsG: Number(carbsG),
        fatG: Number(fatG),
      };
    }
    // Grams unless a portion is chosen, in which case the amount counts portions of it.
    return servingId === ''
      ? { ...common, foodItemId: foodId, grams: Number(amount) }
      : { ...common, foodItemId: foodId, servingId, portions: Number(amount) };
  }

  return (
    <form className={styles.form} onSubmit={submit} aria-busy={pending || undefined}>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <div className={styles.shape} role="radiogroup" aria-label="Qué se registra">
        <button
          type="button"
          role="radio"
          aria-checked={shape === 'catalog'}
          className={shape === 'catalog' ? styles.shapeActive : styles.shapeButton}
          onClick={() => setShape('catalog')}
        >
          Del catálogo
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={shape === 'free'}
          className={shape === 'free' ? styles.shapeActive : styles.shapeButton}
          onClick={() => setShape('free')}
        >
          Escribir los macros
        </button>
      </div>

      <SelectField
        id="log-meal-type"
        label="Momento"
        value={mealType}
        disabled={pending}
        onChange={(event) => setMealType(event.target.value)}
      >
        {MEAL_TYPES.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>

      {plannedMeals.length > 0 && (
        <>
          <SelectField
            id="log-planned-meal"
            label="¿Es una comida del plan?"
            value={plannedMeal}
            disabled={pending}
            onChange={(event) => setPlannedMeal(event.target.value)}
          >
            <option value="">No, algo aparte</option>
            {plannedMeals.map((meal) => (
              <option key={meal.id} value={meal.id}>
                {meal.name}
              </option>
            ))}
          </SelectField>
          {/* Answering a planned meal is what lets the plan say whether it was followed; leaving it
              unanswered is the ordinary case and not a worse one. */}
          <p className={styles.hint}>
            Enlazarlo con una comida del plan es lo que permite saber si lo seguiste.
          </p>
        </>
      )}

      {shape === 'catalog' ? (
        <div className={styles.row}>
          <SelectField
            id="log-food"
            label="Alimento"
            value={foodId}
            required
            disabled={pending}
            onChange={(event) => setFoodId(event.target.value)}
          >
            <option value="">Elige un alimento</option>
            {foods.map((food) => (
              <option key={food.id} value={food.id}>
                {food.name}
              </option>
            ))}
          </SelectField>
          <TextField
            id="log-amount"
            label={servingId === '' ? 'Gramos' : 'Raciones'}
            type="number"
            min="0.1"
            step="0.1"
            value={amount}
            required
            disabled={pending}
            onChange={(event) => setAmount(event.target.value)}
          />
          {servings.length > 0 && (
            <SelectField
              id="log-serving"
              label="Ración"
              value={servingId}
              disabled={pending}
              onChange={(event) => setServingId(event.target.value)}
            >
              <option value="">En gramos</option>
              {servings.map((serving) => (
                <option key={serving.id} value={serving.id}>
                  {serving.name ?? 'Ración'} ({serving.grams} g)
                </option>
              ))}
            </SelectField>
          )}
        </div>
      ) : (
        <>
          <TextField
            id="log-name"
            label="Qué has comido"
            value={name}
            required
            disabled={pending}
            onChange={(event) => setName(event.target.value)}
          />
          <div className={styles.row}>
            <TextField
              id="log-kcal"
              label="kcal"
              type="number"
              min="0"
              value={kcal}
              required
              disabled={pending}
              onChange={(event) => setKcal(event.target.value)}
            />
            <TextField
              id="log-protein"
              label="Proteínas (g)"
              type="number"
              min="0"
              step="0.1"
              value={proteinG}
              required
              disabled={pending}
              onChange={(event) => setProteinG(event.target.value)}
            />
            <TextField
              id="log-carbs"
              label="Hidratos (g)"
              type="number"
              min="0"
              step="0.1"
              value={carbsG}
              required
              disabled={pending}
              onChange={(event) => setCarbsG(event.target.value)}
            />
            <TextField
              id="log-fat"
              label="Grasas (g)"
              type="number"
              min="0"
              step="0.1"
              value={fatG}
              required
              disabled={pending}
              onChange={(event) => setFatG(event.target.value)}
            />
          </div>
          {/* The catalog shape has no macro fields on purpose: a food already knows its own, and a
              second answer would only be there to disagree. */}
          <p className={styles.hint}>
            Solo para algo que no esté en el catálogo. Si lo está, elígelo arriba y sus macros salen
            solos.
          </p>
        </>
      )}

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
          Cancelar
        </Button>
        <Button type="submit" disabled={pending}>
          Registrar
        </Button>
      </div>
    </form>
  );
}
