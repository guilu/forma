import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import {
  createEquivalence,
  deleteEquivalence,
  listEquivalences,
  type EquivalenceBasis,
  type FoodEquivalence,
} from '../../api/equivalences';
import styles from './EquivalencesManager.module.css';

/**
 * What may stand in for a food (V47).
 *
 * <p>The grams are the answer, not the ask. A curator states which nutrient the
 * swap holds equal and how big a portion to talk about; how much of the other
 * food that works out to is computed from the catalog on every read, and moves
 * when somebody corrects a macro. So this screen shows a number nobody typed,
 * and that is the point.
 *
 * <p>One direction only. That rice may be replaced by potato says nothing about
 * the reverse, so the opposite advice is written separately or not at all.
 */
interface EquivalencesManagerProps {
  readonly food: CatalogFood;
  /** The catalog, for choosing what may replace this food. */
  readonly foods: readonly CatalogFood[];
  readonly onClose: () => void;
}

const BASIS_LABELS: readonly { readonly value: EquivalenceBasis; readonly label: string }[] = [
  { value: 'CALORIES', label: 'Calorías' },
  { value: 'PROTEIN', label: 'Proteínas' },
  { value: 'CARBS', label: 'Hidratos' },
  { value: 'FAT', label: 'Grasas' },
];

const basisLabel = (basis: EquivalenceBasis) =>
  BASIS_LABELS.find((candidate) => candidate.value === basis)?.label ?? basis;

/**
 * The macros that drifted, as chips.
 *
 * <p>An absent deviation is left out rather than shown as 0 %, and the difference
 * matters: the nutrient being held equal has no drift by construction, and one
 * the source portion carries none of has no baseline to drift from. Printing
 * "0 %" for either would claim a measurement nobody made.
 */
interface Drift {
  readonly macro: string;
  readonly pct: number;
}

const driftsOf = (equivalence: FoodEquivalence): Drift[] => {
  const candidates: readonly { readonly macro: string; readonly pct?: number }[] = [
    { macro: 'kcal', pct: equivalence.caloriesDeviationPct },
    { macro: 'proteína', pct: equivalence.proteinDeviationPct },
    { macro: 'hidratos', pct: equivalence.carbsDeviationPct },
    { macro: 'grasa', pct: equivalence.fatDeviationPct },
  ];
  return candidates.flatMap(({ macro, pct }) => (pct === undefined ? [] : [{ macro, pct }]));
};

export function EquivalencesManager({ food, foods, onClose }: EquivalencesManagerProps) {
  const notify = useNotify();
  const [equivalences, setEquivalences] = useState<FoodEquivalence[] | undefined>(undefined);
  const [loadError, setLoadError] = useState(false);
  const [targetFoodId, setTargetFoodId] = useState('');
  const [basis, setBasis] = useState<EquivalenceBasis>('CALORIES');
  const [sourceReferenceG, setSourceReferenceG] = useState('100');
  const [tolerance, setTolerance] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const reload = useCallback(async () => {
    try {
      setEquivalences(await listEquivalences(food.id));
      setLoadError(false);
    } catch {
      setLoadError(true);
    }
  }, [food.id]);

  useEffect(() => {
    void reload();
  }, [reload]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      await createEquivalence({
        sourceFoodId: food.id,
        targetFoodId,
        basis,
        sourceReferenceG: Number(sourceReferenceG),
        maxMacroDeviationPct: tolerance.trim() === '' ? undefined : Number(tolerance),
      });
      notify.success('Equivalencia añadida.');
      setTargetFoodId('');
      setTolerance('');
      await reload();
    } catch (caught) {
      // The backend refuses a swap it cannot work out — either food carrying none
      // of the chosen nutrient — and says which. Its sentence beats ours.
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar la equivalencia. Inténtalo de nuevo.',
      );
    } finally {
      setPending(false);
    }
  }

  async function remove(equivalence: FoodEquivalence) {
    setError(undefined);
    try {
      await deleteEquivalence(equivalence.id);
      notify.success(`Equivalencia con ${equivalence.targetName} eliminada.`);
      await reload();
    } catch {
      setError('No se pudo eliminar la equivalencia. Inténtalo de nuevo.');
    }
  }

  if (loadError) {
    return (
      <ErrorState
        message="No se pudieron cargar las equivalencias."
        onRetry={() => void reload()}
      />
    );
  }
  if (equivalences === undefined) {
    return <LoadingState message="Cargando equivalencias…" />;
  }

  // A food cannot stand in for itself, and the backend refuses it — so it is not
  // offered rather than offered and rejected.
  const candidates = foods.filter((candidate) => candidate.id !== food.id);

  return (
    <div className={styles.wrapper}>
      <p className={styles.lead}>
        {`Qué puede sustituir a ${food.name}. Los gramos se calculan del catálogo, no se guardan.`}
      </p>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      {equivalences.length === 0 ? (
        <p className={styles.empty}>Nadie ha dicho todavía qué puede sustituir a este alimento.</p>
      ) : (
        <ul className={styles.list}>
          {equivalences.map((equivalence) => (
            <li key={equivalence.id} className={styles.row}>
              <span className={styles.head}>
                <span className={styles.amount}>
                  {`${equivalence.sourceReferenceG} g → ${equivalence.targetReferenceG} g`}
                </span>
                <span className={styles.target}>{equivalence.targetName}</span>
                <span className={styles.basis}>{basisLabel(equivalence.basis)}</span>
                <button
                  type="button"
                  aria-label={`Eliminar equivalencia con ${equivalence.targetName}`}
                  onClick={() => void remove(equivalence)}
                >
                  <Icon name="trash" size={16} />
                </button>
              </span>
              {driftsOf(equivalence).length > 0 && (
                <span className={styles.drifts}>
                  {driftsOf(equivalence).map(({ macro, pct }) => (
                    <span
                      key={macro}
                      className={
                        equivalence.exceedsTolerance &&
                        equivalence.maxMacroDeviationPct !== undefined &&
                        Math.abs(pct) > equivalence.maxMacroDeviationPct
                          ? `${styles.drift} ${styles.driftOver}`
                          : styles.drift
                      }
                    >
                      {`${macro} ${pct > 0 ? '+' : ''}${pct} %`}
                    </span>
                  ))}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}

      <form className={styles.form} onSubmit={submit} aria-busy={pending || undefined}>
        <SelectField
          id="equivalence-target"
          label="Se puede sustituir por"
          value={targetFoodId}
          required
          disabled={pending}
          onChange={(event) => setTargetFoodId(event.target.value)}
        >
          <option value="">Elige un alimento</option>
          {candidates.map((candidate) => (
            <option key={candidate.id} value={candidate.id}>
              {candidate.name}
            </option>
          ))}
        </SelectField>
        <div className={styles.formRow}>
          <SelectField
            id="equivalence-basis"
            label="Igualando"
            value={basis}
            disabled={pending}
            onChange={(event) => setBasis(event.target.value as EquivalenceBasis)}
          >
            {BASIS_LABELS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
          <TextField
            id="equivalence-portion"
            label="Porción de referencia (g)"
            type="number"
            min="0.1"
            step="0.1"
            required
            value={sourceReferenceG}
            disabled={pending}
            onChange={(event) => setSourceReferenceG(event.target.value)}
          />
          <TextField
            id="equivalence-tolerance"
            label="Aviso si otro macro varía (%)"
            type="number"
            min="0.1"
            step="0.1"
            value={tolerance}
            disabled={pending}
            onChange={(event) => setTolerance(event.target.value)}
          />
        </div>
        <div className={styles.actions}>
          <Button type="submit" disabled={pending}>
            Añadir
          </Button>
          <Button type="button" variant="ghost" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </form>
    </div>
  );
}
