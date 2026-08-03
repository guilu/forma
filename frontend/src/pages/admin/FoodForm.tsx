import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import {
  createFood,
  updateFood,
  type CatalogFood,
  type Preparation,
  type PrimaryMacro,
} from '../../api/foods';
import type { CategoryDisplay } from '../../api/categories';
import { listFoodTags, setFoodTags } from '../../api/tags';
import { useTags } from './useTags';
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
  /**
   * The food groups to offer, as the backend serves them. Passed in rather than
   * fetched here: the panel already asked, and a modal that asks again would
   * open with an empty select every time.
   */
  readonly groups: readonly CategoryDisplay[];
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

/**
 * How each macronutrient reads, in the order a macro row is usually written.
 *
 * <p>Hardcoded, and that is not the mistake V43 undid for food groups: this set
 * cannot grow. A fourth macronutrient would be news.
 */
/**
 * How each state reads. Hardcoded like the macros and unlike the food groups:
 * a food goes into the kitchen, comes out of it, or never passes through, and no
 * amount of curating produces a fourth.
 */
const PREPARATION_LABELS: readonly { readonly value: Preparation; readonly label: string }[] = [
  { value: 'CRUDO', label: 'Crudo (hay que cocinarlo)' },
  { value: 'COCINADO', label: 'Cocinado' },
  { value: 'TAL_CUAL', label: 'Tal cual (no se cocina)' },
];

const MACRO_LABELS: readonly { readonly value: PrimaryMacro; readonly label: string }[] = [
  { value: 'PROTEIN', label: 'Proteínas' },
  { value: 'CARBS', label: 'Hidratos' },
  { value: 'FAT', label: 'Grasas' },
];

/**
 * The groups to offer, with the food's own always among them.
 *
 * <p>The list is a request: empty while in flight, empty if it failed, and one
 * short if the food's group has since been retired. A select that cannot show
 * its own value would display something else and save that instead — silently
 * reclassifying a food nobody asked to reclassify. So the current value is added
 * back, labelled with its code, which is at least true.
 */
function optionsFor(
  groups: readonly CategoryDisplay[],
  current: string,
): readonly { readonly code: string; readonly label: string }[] {
  const offered = groups.map((group) => ({ code: group.code, label: group.label }));
  if (current === '' || offered.some((option) => option.code === current)) {
    return offered;
  }
  return [...offered, { code: current, label: current }];
}

/** Empty string for an absent optional, so the input renders blank rather than "undefined". */
const text = (value: number | undefined) => (value === undefined ? '' : String(value));

export function FoodForm({ food, groups, onCancel, onSaved }: FoodFormProps) {
  const notify = useNotify();
  const creating = food === undefined;
  const [id, setId] = useState(food?.id ?? '');
  const [name, setName] = useState(food?.name ?? '');
  const [category, setCategory] = useState<string>(food?.foodGroupId ?? '');
  // Empty means "let the macros decide": the backend derives it when the field
  // is absent. An existing classification is shown as stored, never reset on
  // open — resetting would discard a curator's choice for opening a form.
  const [primaryMacro, setPrimaryMacro] = useState<string>(food?.primaryMacro ?? '');
  // Empty is "nobody has decided", which is not the same as TAL_CUAL. The
  // question not applying to oil is an answer; not having been asked is not.
  const [preparation, setPreparation] = useState<string>(food?.preparation ?? '');
  const [kcal, setKcal] = useState(text(food?.kcal));
  const [proteinG, setProteinG] = useState(text(food?.proteinG));
  const [carbsG, setCarbsG] = useState(text(food?.carbsG));
  const [fatG, setFatG] = useState(text(food?.fatG));
  const tags = useTags();
  // Which labels are ticked. Loaded separately from the food because they live
  // behind their own endpoint, and empty for a food that does not exist yet.
  const [tagIds, setTagIds] = useState<readonly string[]>([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (food === undefined) return;
    let active = true;
    listFoodTags(food.id)
      .then((carried) => {
        if (active) setTagIds(carried.map((tag) => tag.id));
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [food]);

  const toggleTag = (tagId: string) =>
    setTagIds((current) =>
      current.includes(tagId)
        ? current.filter((candidate) => candidate !== tagId)
        : [...current, tagId],
    );

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    const payload: CatalogFood = {
      id: id.trim(),
      name: name.trim(),
      kcal: Number(kcal || 0),
      proteinG: Number(proteinG || 0),
      carbsG: Number(carbsG || 0),
      fatG: Number(fatG || 0),
      foodGroupId: category === '' ? undefined : category,
      primaryMacro: primaryMacro === '' ? undefined : (primaryMacro as PrimaryMacro),
      preparation: preparation === '' ? undefined : (preparation as Preparation),
    };
    try {
      // The food first, then its labels: a new one has no id to hang them off
      // until it exists, so the two cannot be one request whatever the edit.
      if (creating) {
        await createFood(payload);
      } else {
        await updateFood(food.id, payload);
      }
      try {
        await setFoodTags(payload.id, tagIds);
        notify.success(creating ? 'Alimento creado.' : 'Alimento actualizado.');
      } catch {
        // The food is saved and the labels are not, which is a different thing
        // from "nothing happened" and has to read as one — telling somebody the
        // save failed would send them to do it again.
        notify.warning(
          creating
            ? 'Alimento creado, pero no se pudieron guardar sus etiquetas.'
            : 'Alimento actualizado, pero no se pudieron guardar sus etiquetas.',
        );
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
        {optionsFor(groups, category).map((option) => (
          <option key={option.code} value={option.code}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="food-primary-macro"
        label="Macro principal"
        value={primaryMacro}
        disabled={pending}
        onChange={(event) => setPrimaryMacro(event.target.value)}
      >
        <option value="">Automático (según los macros)</option>
        {MACRO_LABELS.map((macro) => (
          <option key={macro.value} value={macro.value}>
            {macro.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="food-preparation"
        label="Estado"
        value={preparation}
        disabled={pending}
        onChange={(event) => setPreparation(event.target.value)}
      >
        <option value="">Sin decidir</option>
        {PREPARATION_LABELS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>

      {/* Absent rather than empty while the vocabulary is in flight: a legend
          over nothing reads as "this food can carry no labels". */}
      {tags.options.length > 0 && (
        <fieldset className={styles.tags}>
          <legend className={styles.tagsLegend}>Etiquetas</legend>
          <div className={styles.tagList}>
            {tags.options.map((tag) => (
              <label key={tag.id} className={styles.tagOption}>
                <input
                  type="checkbox"
                  checked={tagIds.includes(tag.id)}
                  disabled={pending}
                  onChange={() => toggleTag(tag.id)}
                />
                {tag.name}
              </label>
            ))}
          </div>
        </fieldset>
      )}

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
