import { useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { Icon } from '../../components/Icon';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import { createRecipe, updateRecipe, type Recipe } from '../../api/recipes';
import styles from './RecipeForm.module.css';

/**
 * Create/edit form for a recipe (V52).
 *
 * <p>No macro fields, and that is the point: a dish's totals are the sum over its ingredients of
 * what the catalog holds, so there is nothing here to type them into. What an existing recipe adds
 * up to is shown under the list, read back from the server rather than computed here — a second copy
 * of that arithmetic is exactly what the backend refused to store.
 *
 * <p>The id is only editable while creating. It is the dish's stable handle, so an edit shows it
 * read-only rather than offering a rename the backend would ignore.
 */
interface RecipeFormProps {
  /** Absent when creating. */
  readonly recipe?: Recipe;
  /** The catalog, for choosing what goes in. */
  readonly foods: readonly CatalogFood[];
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

interface Line {
  readonly foodId: string;
  readonly grams: string;
}

const EMPTY_LINE: Line = { foodId: '', grams: '' };

export function RecipeForm({ recipe, foods, onCancel, onSaved }: RecipeFormProps) {
  const notify = useNotify();
  const creating = recipe === undefined;
  const [id, setId] = useState(recipe?.id ?? '');
  const [name, setName] = useState(recipe?.name ?? '');
  const [servings, setServings] = useState(String(recipe?.servings ?? 1));
  const [notes, setNotes] = useState(recipe?.notes ?? '');
  const [lines, setLines] = useState<Line[]>(
    recipe === undefined
      ? [EMPTY_LINE]
      : recipe.ingredients.map((line) => ({ foodId: line.foodId, grams: String(line.grams) })),
  );
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const setLine = (index: number, line: Line) =>
    setLines((current) => current.map((existing, at) => (at === index ? line : existing)));

  const removeLine = (index: number) =>
    setLines((current) => current.filter((_, at) => at !== index));

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    const payload = {
      id: id.trim(),
      name: name.trim(),
      servings: Number(servings || 1),
      notes: notes.trim() === '' ? undefined : notes.trim(),
      // Blank lines are the row somebody added and did not fill, not an ingredient of nothing.
      ingredients: lines
        .filter((line) => line.foodId !== '' && line.grams.trim() !== '')
        .map((line) => ({ foodId: line.foodId, grams: Number(line.grams) })),
    };
    try {
      if (creating) {
        await createRecipe(payload);
        notify.success('Receta creada.');
      } else {
        await updateRecipe(recipe.id, payload);
        notify.success('Receta actualizada.');
      }
      onSaved();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar la receta. Inténtalo de nuevo.',
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
        id="recipe-id"
        label="Identificador"
        value={id}
        required
        disabled={!creating || pending}
        onChange={(event) => setId(event.target.value)}
      />
      <div className={styles.head}>
        <TextField
          id="recipe-name"
          label="Nombre"
          value={name}
          required
          disabled={pending}
          onChange={(event) => setName(event.target.value)}
        />
        <TextField
          id="recipe-servings"
          label="Raciones que salen"
          type="number"
          min="1"
          step="1"
          required
          value={servings}
          disabled={pending}
          onChange={(event) => setServings(event.target.value)}
        />
      </div>

      <fieldset className={styles.ingredients}>
        <legend className={styles.legend}>Ingredientes</legend>
        {lines.map((line, index) => (
          <div className={styles.line} key={index}>
            <SelectField
              id={`recipe-food-${index}`}
              label="Alimento"
              value={line.foodId}
              disabled={pending}
              onChange={(event) => setLine(index, { ...line, foodId: event.target.value })}
            >
              <option value="">Elige un alimento</option>
              {foods.map((food) => (
                <option key={food.id} value={food.id}>
                  {food.name}
                </option>
              ))}
            </SelectField>
            <TextField
              id={`recipe-grams-${index}`}
              label="Gramos"
              type="number"
              min="0.1"
              step="0.1"
              value={line.grams}
              disabled={pending}
              onChange={(event) => setLine(index, { ...line, grams: event.target.value })}
            />
            <button
              type="button"
              aria-label={`Quitar ingrediente ${index + 1}`}
              disabled={pending || lines.length === 1}
              onClick={() => removeLine(index)}
            >
              <Icon name="trash" size={16} />
            </button>
          </div>
        ))}
        <Button
          type="button"
          variant="ghost"
          disabled={pending}
          onClick={() => setLines((current) => [...current, EMPTY_LINE])}
        >
          + Ingrediente
        </Button>
        {/* The amounts are read as the catalog records each food (V51), and that is
            not obvious to somebody typing 80 next to "Arroz". */}
        <p className={styles.hint}>
          Los gramos son del alimento tal y como está en el catálogo: si el arroz está registrado
          seco, aquí se pone seco.
        </p>
      </fieldset>

      {recipe && (
        <div className={styles.totals}>
          <span className={styles.totalsLabel}>Por ración:</span>
          <span>{`${recipe.perServing.calories} kcal`}</span>
          <span>{`P ${recipe.perServing.proteinG} g`}</span>
          <span>{`HC ${recipe.perServing.carbsG} g`}</span>
          <span>{`G ${recipe.perServing.fatG} g`}</span>
        </div>
      )}

      <TextField
        id="recipe-notes"
        label="Notas"
        value={notes}
        disabled={pending}
        onChange={(event) => setNotes(event.target.value)}
      />

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
