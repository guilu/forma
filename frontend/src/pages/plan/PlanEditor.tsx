import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { Icon } from '../../components/Icon';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { listFoods, type CatalogFood } from '../../api/foods';
import { listServings, type FoodServing } from '../../api/servings';
import {
  createPlan,
  updatePlan,
  type DayType,
  type MealTypeCode,
  type NewPlan,
  type NutritionPlan,
} from '../../api/plans';
import styles from './PlanEditor.module.css';

/**
 * Create/edit form for a nutrition plan (V53/V54).
 *
 * <p>No macro fields anywhere, and that is the point: a day's totals are the sum
 * over its meals of what the catalog holds, so there is nothing here to type
 * them into. What each day comes to is shown beside its name, read back from
 * the server rather than computed here — a second copy of that arithmetic is
 * exactly what the model refused to store.
 *
 * <p>The one number a day DOES take is its target, which is a decision and
 * cannot be computed. Left empty it means nobody decided, and the day falls back
 * to the plan's and then to the profile's — so an empty field here is a real
 * answer, not a missing one.
 *
 * <p>An amount is grams unless a portion is chosen beside it, in which case it
 * counts portions: `1` and "Mediano" is one medium banana, and its grams follow
 * from what that portion weighs. A food's portions are fetched the first time it
 * is picked and kept, so choosing oats in nine meals asks once.
 */
interface PlanEditorProps {
  /** Absent when creating. */
  readonly plan?: NutritionPlan;
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

const WEEKDAYS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

const DAY_TYPES: ReadonlyArray<{ readonly value: DayType | ''; readonly label: string }> = [
  { value: '', label: 'Sin clasificar' },
  { value: 'RUNNING', label: 'Carrera' },
  { value: 'STRENGTH', label: 'Fuerza' },
  { value: 'REST', label: 'Descanso' },
];

const MEAL_TYPES: ReadonlyArray<{ readonly value: MealTypeCode; readonly label: string }> = [
  { value: 'BREAKFAST', label: 'Desayuno' },
  { value: 'MID_MORNING', label: 'Media mañana' },
  { value: 'LUNCH', label: 'Comida' },
  { value: 'SNACK', label: 'Merienda' },
  { value: 'PRE_WORKOUT', label: 'Pre-entreno' },
  { value: 'POST_WORKOUT', label: 'Post-entreno' },
  { value: 'DINNER', label: 'Cena' },
];

interface ItemDraft {
  readonly foodId: string;
  readonly servingId: string;
  readonly amount: string;
  readonly optional: boolean;
  /** What it works out to, as the server last said. Never recomputed here. */
  readonly grams?: number;
  readonly calories?: number;
}

interface MealDraft {
  readonly mealType: MealTypeCode;
  readonly name: string;
  readonly scheduledTime: string;
  readonly optional: boolean;
  readonly instructions: string;
  readonly items: ItemDraft[];
}

interface DayDraft {
  readonly dayNumber: number;
  readonly dayType: DayType | '';
  readonly targetKcal: string;
  readonly notes: string;
  readonly meals: MealDraft[];
  readonly totalCalories?: number;
}

const EMPTY_ITEM: ItemDraft = { foodId: '', servingId: '', amount: '', optional: false };

export function PlanEditor({ plan, onCancel, onSaved }: PlanEditorProps) {
  const notify = useNotify();
  const creating = plan === undefined;
  const [name, setName] = useState(plan?.name ?? '');
  const [description, setDescription] = useState(plan?.description ?? '');
  const [startDate, setStartDate] = useState(plan?.startDate ?? '');
  const [kcalMin, setKcalMin] = useState(numberField(plan?.targets.kcalMin));
  const [kcalMax, setKcalMax] = useState(numberField(plan?.targets.kcalMax));
  const [days, setDays] = useState<DayDraft[]>(() => draftDays(plan));
  const [openDay, setOpenDay] = useState<number | undefined>(undefined);
  const [foods, setFoods] = useState<CatalogFood[]>([]);
  const [servings, setServings] = useState<Record<string, FoodServing[]>>({});
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

  /** Fetches a food's portions once, the first time anybody picks it. */
  const loadServings = (foodId: string) => {
    if (foodId === '' || servings[foodId] !== undefined) return;
    listServings(foodId)
      .then((rows) => setServings((current) => ({ ...current, [foodId]: rows })))
      .catch(() => setServings((current) => ({ ...current, [foodId]: [] })));
  };

  useEffect(() => {
    days.forEach((day) =>
      day.meals.forEach((meal) => meal.items.forEach((item) => loadServings(item.foodId))),
    );
    // Only on mount: later picks load through the change handler, and re-running this on every
    // keystroke would ask for the same portions again on every character typed into an amount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const patchDay = (at: number, patch: Partial<DayDraft>) =>
    setDays((current) => current.map((day, i) => (i === at ? { ...day, ...patch } : day)));

  const patchMeal = (dayAt: number, mealAt: number, patch: Partial<MealDraft>) =>
    setDays((current) =>
      current.map((day, i) =>
        i === dayAt
          ? {
              ...day,
              meals: day.meals.map((meal, j) => (j === mealAt ? { ...meal, ...patch } : meal)),
            }
          : day,
      ),
    );

  const patchItem = (dayAt: number, mealAt: number, itemAt: number, patch: Partial<ItemDraft>) =>
    setDays((current) =>
      current.map((day, i) =>
        i === dayAt
          ? {
              ...day,
              meals: day.meals.map((meal, j) =>
                j === mealAt
                  ? {
                      ...meal,
                      items: meal.items.map((item, k) =>
                        k === itemAt ? { ...item, ...patch } : item,
                      ),
                    }
                  : meal,
              ),
            }
          : day,
      ),
    );

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      const payload = toPayload({ name, description, startDate, kcalMin, kcalMax, days });
      if (creating) {
        await createPlan(payload);
        notify.success('Plan creado.');
      } else {
        await updatePlan(plan.id, payload);
        notify.success('Plan actualizado.');
      }
      onSaved();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar el plan. Inténtalo de nuevo.',
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={submit} aria-busy={pending || undefined}>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <TextField
        id="plan-name"
        label="Nombre"
        value={name}
        required
        disabled={pending}
        onChange={(event) => setName(event.target.value)}
      />
      <TextField
        id="plan-description"
        label="Descripción"
        value={description}
        disabled={pending}
        onChange={(event) => setDescription(event.target.value)}
      />
      <div className={styles.head}>
        <TextField
          id="plan-start"
          label="Empieza el"
          type="date"
          value={startDate}
          disabled={pending}
          onChange={(event) => setStartDate(event.target.value)}
        />
        <TextField
          id="plan-kcal-min"
          label="kcal mínimas"
          type="number"
          min="0"
          value={kcalMin}
          disabled={pending}
          onChange={(event) => setKcalMin(event.target.value)}
        />
        <TextField
          id="plan-kcal-max"
          label="kcal máximas"
          type="number"
          min="0"
          value={kcalMax}
          disabled={pending}
          onChange={(event) => setKcalMax(event.target.value)}
        />
      </div>
      {/* An empty target is not a target of zero: it means the day falls back to the plan's, and
          the plan's to the profile's. */}
      <p className={styles.hint}>
        Deja un objetivo en blanco y se hereda: el día toma el del plan, y el plan el de tu perfil.
      </p>

      <fieldset className={styles.week}>
        <legend className={styles.legend}>La semana</legend>
        {days.map((day, dayAt) => (
          <div className={styles.day} key={day.dayNumber}>
            <button
              type="button"
              className={styles.dayHead}
              aria-expanded={openDay === day.dayNumber}
              onClick={() => setOpenDay(openDay === day.dayNumber ? undefined : day.dayNumber)}
            >
              <span className={styles.dayName}>{WEEKDAYS[day.dayNumber - 1]}</span>
              <span className={styles.dayMeta}>
                {day.meals.length} {day.meals.length === 1 ? 'comida' : 'comidas'}
                {day.totalCalories !== undefined && ` · ${day.totalCalories} kcal`}
              </span>
              <Icon name="chevron" size={16} />
            </button>

            {openDay === day.dayNumber && (
              <div className={styles.dayBody}>
                <div className={styles.dayFields}>
                  <SelectField
                    id={`day-type-${day.dayNumber}`}
                    label="Tipo de día"
                    value={day.dayType}
                    disabled={pending}
                    onChange={(event) =>
                      patchDay(dayAt, { dayType: event.target.value as DayType | '' })
                    }
                  >
                    {DAY_TYPES.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </SelectField>
                  <TextField
                    id={`day-kcal-${day.dayNumber}`}
                    label="Objetivo kcal"
                    type="number"
                    min="0"
                    value={day.targetKcal}
                    disabled={pending}
                    onChange={(event) => patchDay(dayAt, { targetKcal: event.target.value })}
                  />
                </div>

                {day.meals.map((meal, mealAt) => (
                  <div className={styles.meal} key={mealAt}>
                    <div className={styles.mealHead}>
                      <SelectField
                        id={`meal-type-${day.dayNumber}-${mealAt}`}
                        label="Momento"
                        value={meal.mealType}
                        disabled={pending}
                        onChange={(event) =>
                          patchMeal(dayAt, mealAt, {
                            mealType: event.target.value as MealTypeCode,
                          })
                        }
                      >
                        {MEAL_TYPES.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </SelectField>
                      <TextField
                        id={`meal-name-${day.dayNumber}-${mealAt}`}
                        label="Nombre"
                        value={meal.name}
                        required
                        disabled={pending}
                        onChange={(event) => patchMeal(dayAt, mealAt, { name: event.target.value })}
                      />
                      <TextField
                        id={`meal-time-${day.dayNumber}-${mealAt}`}
                        label="Hora"
                        type="time"
                        value={meal.scheduledTime}
                        disabled={pending}
                        onChange={(event) =>
                          patchMeal(dayAt, mealAt, { scheduledTime: event.target.value })
                        }
                      />
                      <label className={styles.check}>
                        <input
                          type="checkbox"
                          checked={meal.optional}
                          disabled={pending}
                          onChange={(event) =>
                            patchMeal(dayAt, mealAt, { optional: event.target.checked })
                          }
                        />
                        Opcional
                      </label>
                      <button
                        type="button"
                        aria-label={`Quitar ${meal.name || 'comida'}`}
                        disabled={pending}
                        onClick={() =>
                          patchDay(dayAt, {
                            meals: day.meals.filter((_, j) => j !== mealAt),
                          })
                        }
                      >
                        <Icon name="trash" size={16} />
                      </button>
                    </div>

                    {meal.items.map((item, itemAt) => (
                      <div className={styles.line} key={itemAt}>
                        <SelectField
                          id={`item-food-${day.dayNumber}-${mealAt}-${itemAt}`}
                          label="Alimento"
                          value={item.foodId}
                          disabled={pending}
                          onChange={(event) => {
                            const foodId = event.target.value;
                            loadServings(foodId);
                            // The portion belonged to the food that was there before; keeping it
                            // would count slices of something that has no slices.
                            patchItem(dayAt, mealAt, itemAt, { foodId, servingId: '' });
                          }}
                        >
                          <option value="">Elige un alimento</option>
                          {foods.map((food) => (
                            <option key={food.id} value={food.id}>
                              {food.name}
                            </option>
                          ))}
                        </SelectField>
                        <TextField
                          id={`item-amount-${day.dayNumber}-${mealAt}-${itemAt}`}
                          label={item.servingId === '' ? 'Gramos' : 'Raciones'}
                          type="number"
                          min="0.1"
                          step="0.1"
                          value={item.amount}
                          disabled={pending}
                          onChange={(event) =>
                            patchItem(dayAt, mealAt, itemAt, { amount: event.target.value })
                          }
                        />
                        {(servings[item.foodId] ?? []).length > 0 && (
                          <SelectField
                            id={`item-serving-${day.dayNumber}-${mealAt}-${itemAt}`}
                            label="Ración"
                            value={item.servingId}
                            disabled={pending}
                            onChange={(event) =>
                              patchItem(dayAt, mealAt, itemAt, { servingId: event.target.value })
                            }
                          >
                            <option value="">En gramos</option>
                            {(servings[item.foodId] ?? []).map((serving) => (
                              <option key={serving.id} value={serving.id}>
                                {serving.name ?? 'Ración'} ({serving.grams} g)
                              </option>
                            ))}
                          </SelectField>
                        )}
                        {item.grams !== undefined && item.servingId !== '' && (
                          <span className={styles.resolved}>= {item.grams} g</span>
                        )}
                        <button
                          type="button"
                          aria-label={`Quitar alimento ${itemAt + 1}`}
                          disabled={pending}
                          onClick={() =>
                            patchMeal(dayAt, mealAt, {
                              items: meal.items.filter((_, k) => k !== itemAt),
                            })
                          }
                        >
                          <Icon name="trash" size={16} />
                        </button>
                      </div>
                    ))}

                    <Button
                      type="button"
                      variant="ghost"
                      disabled={pending}
                      onClick={() =>
                        patchMeal(dayAt, mealAt, { items: [...meal.items, EMPTY_ITEM] })
                      }
                    >
                      + Alimento
                    </Button>
                  </div>
                ))}

                <Button
                  type="button"
                  variant="ghost"
                  disabled={pending}
                  onClick={() =>
                    patchDay(dayAt, {
                      meals: [
                        ...day.meals,
                        {
                          mealType: 'BREAKFAST',
                          name: '',
                          scheduledTime: '',
                          optional: false,
                          instructions: '',
                          items: [EMPTY_ITEM],
                        },
                      ],
                    })
                  }
                >
                  + Comida
                </Button>
              </div>
            )}
          </div>
        ))}
      </fieldset>

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
          Cancelar
        </Button>
        <Button type="submit" disabled={pending}>
          Guardar
        </Button>
      </div>
    </form>
  );
}

/** Seven days, always. A plan missing Thursday is a plan somebody has not filled in yet. */
function draftDays(plan: NutritionPlan | undefined): DayDraft[] {
  return Array.from({ length: 7 }, (_, index) => {
    const dayNumber = index + 1;
    const stored = plan?.days.find((day) => day.dayNumber === dayNumber && day.weekNumber === 1);
    return {
      dayNumber,
      dayType: stored?.dayType ?? '',
      targetKcal: numberField(stored?.targets.calories ?? null),
      notes: stored?.notes ?? '',
      totalCalories: stored?.totals?.calories,
      meals: (stored?.meals ?? []).map((meal) => ({
        mealType: meal.mealType,
        name: meal.name,
        // The API sends HH:MM:SS and an <input type="time"> wants HH:MM.
        scheduledTime: meal.scheduledTime?.slice(0, 5) ?? '',
        optional: meal.optional,
        instructions: meal.instructions ?? '',
        items: meal.items.map((item) => ({
          foodId: item.foodId ?? '',
          servingId: item.servingId ?? '',
          amount: String(item.amount),
          optional: item.optional,
          grams: item.grams,
          calories: item.totals?.calories,
        })),
      })),
    };
  });
}

interface FormValues {
  readonly name: string;
  readonly description: string;
  readonly startDate: string;
  readonly kcalMin: string;
  readonly kcalMax: string;
  readonly days: DayDraft[];
}

/**
 * What gets sent. Days with no meals and lines nobody filled in are left out —
 * seven empty days is what the form shows, not what somebody wrote.
 */
function toPayload(values: FormValues): NewPlan {
  return {
    name: values.name.trim(),
    description: blankToUndefined(values.description),
    startDate: blankToUndefined(values.startDate),
    targets: {
      kcalMin: numberOrNull(values.kcalMin),
      kcalMax: numberOrNull(values.kcalMax),
      proteinG: null,
      carbsG: null,
      fatG: null,
    },
    days: values.days
      .map((day) => ({
        weekNumber: 1,
        dayNumber: day.dayNumber,
        dayType: day.dayType === '' ? undefined : day.dayType,
        targets: { calories: numberOrNull(day.targetKcal) },
        notes: blankToUndefined(day.notes),
        meals: day.meals
          .map((meal) => ({
            mealType: meal.mealType,
            name: meal.name.trim(),
            scheduledTime: meal.scheduledTime === '' ? undefined : `${meal.scheduledTime}:00`,
            optional: meal.optional,
            instructions: blankToUndefined(meal.instructions),
            items: meal.items
              .filter((item) => item.foodId !== '' && item.amount.trim() !== '')
              .map((item) => ({
                foodId: item.foodId,
                servingId: item.servingId === '' ? undefined : item.servingId,
                amount: Number(item.amount),
                optional: item.optional,
              })),
          }))
          .filter((meal) => meal.name !== '' && meal.items.length > 0),
      }))
      .filter((day) => day.meals.length > 0),
  };
}

function numberField(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value);
}

function numberOrNull(value: string): number | null {
  return value.trim() === '' ? null : Number(value);
}

function blankToUndefined(value: string): string | undefined {
  return value.trim() === '' ? undefined : value.trim();
}
