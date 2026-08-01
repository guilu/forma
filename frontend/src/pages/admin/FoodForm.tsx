import { useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { createFood, updateFood, type CatalogFood, type FoodCategory } from '../../api/foods';
import { CATEGORY_LABELS, CATEGORY_OPTIONS } from './foodDisplay';
import styles from './FoodForm.module.css';

/**
 * Create/edit form for a catalog food (FOR-190).
 *
 * <p>Macros are per 100 g — the unit the catalog stores and the source sheet
 * uses — and the ration is the suggested portion, not a second set of macros:
 * the per-ration figures in the sheet are derived from these two, so entering
 * them separately would create a second source of truth that can disagree.
 *
 * <p>The id is only editable while creating. It is the catalog's stable handle,
 * referenced by shopping products through a foreign key, so an edit shows it
 * read-only rather than offering a rename the backend would ignore.
 */
interface FoodFormProps {
  /** Absent when creating. */
  readonly food?: CatalogFood;
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

/** Empty string for an absent optional, so the input renders blank rather than "undefined". */
const text = (value: number | undefined) => (value === undefined ? '' : String(value));

export function FoodForm({ food, onCancel, onSaved }: FoodFormProps) {
  const notify = useNotify();
  const creating = food === undefined;
  const [id, setId] = useState(food?.id ?? '');
  const [name, setName] = useState(food?.name ?? '');
  const [category, setCategory] = useState<string>(food?.category ?? '');
  const [kcal, setKcal] = useState(text(food?.kcal));
  const [proteinG, setProteinG] = useState(text(food?.proteinG));
  const [carbsG, setCarbsG] = useState(text(food?.carbsG));
  const [fatG, setFatG] = useState(text(food?.fatG));
  const [servingSizeG, setServingSizeG] = useState(text(food?.servingSizeG));
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    // A blank optional stays absent rather than becoming 0 — the catalog is full
    // of foods whose values nobody has looked up, and a 0 would be a claim.
    const optional = (value: string) => (value.trim() === '' ? undefined : Number(value));
    const payload: CatalogFood = {
      id: id.trim(),
      name: name.trim(),
      kcal: Number(kcal || 0),
      proteinG: Number(proteinG || 0),
      carbsG: Number(carbsG || 0),
      fatG: Number(fatG || 0),
      servingSizeG: optional(servingSizeG),
      category: category === '' ? undefined : (category as FoodCategory),
    };
    try {
      if (creating) {
        await createFood(payload);
        notify.success('Alimento creado.');
      } else {
        await updateFood(food.id, payload);
        notify.success('Alimento actualizado.');
      }
      onSaved();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar el alimento. Inténtalo de nuevo.',
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
        id="food-id"
        label="Identificador"
        value={id}
        required
        // Read-only on edit: shopping products reference it by foreign key.
        disabled={!creating || pending}
        pattern="[a-z0-9-]+"
        onChange={(event) => setId(event.target.value)}
      />
      <TextField
        id="food-name"
        label="Nombre"
        value={name}
        required
        disabled={pending}
        onChange={(event) => setName(event.target.value)}
      />
      <SelectField
        id="food-category"
        label="Categoría"
        value={category}
        disabled={pending}
        onChange={(event) => setCategory(event.target.value)}
      >
        <option value="">Sin clasificar</option>
        {CATEGORY_OPTIONS.map((option) => (
          <option key={option} value={option}>
            {CATEGORY_LABELS[option]}
          </option>
        ))}
      </SelectField>

      <div className={styles.macros}>
        <TextField
          id="food-kcal"
          label="kcal / 100 g"
          type="number"
          min="0"
          value={kcal}
          required
          disabled={pending}
          onChange={(event) => setKcal(event.target.value)}
        />
        <TextField
          id="food-protein"
          label="Proteína / 100 g"
          type="number"
          min="0"
          step="0.1"
          value={proteinG}
          required
          disabled={pending}
          onChange={(event) => setProteinG(event.target.value)}
        />
        <TextField
          id="food-carbs"
          label="HC / 100 g"
          type="number"
          min="0"
          step="0.1"
          value={carbsG}
          required
          disabled={pending}
          onChange={(event) => setCarbsG(event.target.value)}
        />
        <TextField
          id="food-fat"
          label="Grasa / 100 g"
          type="number"
          min="0"
          step="0.1"
          value={fatG}
          required
          disabled={pending}
          onChange={(event) => setFatG(event.target.value)}
        />
        <TextField
          id="food-serving"
          label="Ración sugerida (g)"
          type="number"
          min="0"
          step="0.1"
          value={servingSizeG}
          disabled={pending}
          onChange={(event) => setServingSizeG(event.target.value)}
        />
      </div>

      <div className={styles.actions}>
        <Button variant="secondary" type="button" onClick={onCancel} disabled={pending}>
          Cancelar
        </Button>
        <Button variant="accent" type="submit" loading={pending}>
          Guardar
        </Button>
      </div>
    </form>
  );
}
