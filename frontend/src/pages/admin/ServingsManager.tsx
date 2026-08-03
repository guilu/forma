import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
import { TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import {
  createServing,
  deleteServing,
  listServings,
  updateServing,
  type FoodServing,
} from '../../api/servings';
import styles from './ServingsManager.module.css';

/**
 * A food's portions (V49).
 *
 * <p>Its own screen rather than a field on the food form, and that is not a
 * layout decision. The form used to carry a single `servingSizeG`, which is the
 * shape the catalog had before V49; leaving it there beside this would give one
 * row two editors, and a form opened before a portion was changed here would
 * quietly put the old number back on save.
 *
 * <p>One portion at most is the default — what "one serving" means. Claiming it
 * takes it from whichever held it, so there is no "unset the old one" step and
 * no moment where a food has two or none by accident.
 */
interface ServingsManagerProps {
  readonly food: CatalogFood;
  readonly onClose: () => void;
}

interface Draft {
  /** Absent when adding; the portion being replaced when editing. */
  readonly id?: string;
  readonly name: string;
  readonly grams: string;
  readonly isDefault: boolean;
}

const EMPTY: Draft = { name: '', grams: '', isDefault: false };

/** The plain portion a food starts with has no name; the list still has to call it something. */
const nameOf = (serving: FoodServing) => serving.name ?? 'Ración';

export function ServingsManager({ food, onClose }: ServingsManagerProps) {
  const notify = useNotify();
  const [servings, setServings] = useState<FoodServing[] | undefined>(undefined);
  const [loadError, setLoadError] = useState(false);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const reload = useCallback(async () => {
    try {
      setServings(await listServings(food.id));
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
    const payload = {
      // A portion with no name is the plain one. Forcing a name on the only
      // portion a food has would mean inventing "Normal".
      name: draft.name.trim() === '' ? undefined : draft.name.trim(),
      grams: Number(draft.grams),
      isDefault: draft.isDefault,
      sortOrder: 0,
    };
    try {
      if (draft.id === undefined) {
        await createServing(food.id, payload);
        notify.success('Ración añadida.');
      } else {
        await updateServing(food.id, draft.id, payload);
        notify.success('Ración actualizada.');
      }
      setDraft(EMPTY);
      await reload();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar la ración. Inténtalo de nuevo.',
      );
    } finally {
      setPending(false);
    }
  }

  async function remove(serving: FoodServing) {
    setError(undefined);
    try {
      await deleteServing(food.id, serving.id);
      notify.success(`${nameOf(serving)} eliminada.`);
      await reload();
    } catch {
      setError('No se pudo eliminar la ración. Inténtalo de nuevo.');
    }
  }

  if (loadError) {
    return (
      <ErrorState message="No se pudieron cargar las raciones." onRetry={() => void reload()} />
    );
  }
  if (servings === undefined) {
    return <LoadingState message="Cargando raciones…" />;
  }

  return (
    <div className={styles.wrapper}>
      <p className={styles.lead}>
        {`Cómo se mide ${food.name}. La marcada por defecto es la que significa "una ración".`}
      </p>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      {servings.length === 0 ? (
        <p className={styles.empty}>
          Nadie ha decidido una porción para este alimento. Se puede registrar por gramos
          igualmente.
        </p>
      ) : (
        <ul className={styles.list}>
          {servings.map((serving) => (
            <li
              key={serving.id}
              className={serving.isDefault ? `${styles.row} ${styles.rowDefault}` : styles.row}
            >
              <span className={styles.name}>
                {nameOf(serving)}
                {serving.isDefault && ' · por defecto'}
              </span>
              <span className={styles.grams}>{`${serving.grams} g`}</span>
              <span className={styles.rowActions}>
                <button
                  type="button"
                  aria-label={`Editar ${nameOf(serving)}`}
                  onClick={() =>
                    setDraft({
                      id: serving.id,
                      name: serving.name ?? '',
                      grams: String(serving.grams),
                      isDefault: serving.isDefault,
                    })
                  }
                >
                  <Icon name="edit" size={16} />
                </button>
                <button
                  type="button"
                  aria-label={`Eliminar ${nameOf(serving)}`}
                  onClick={() => void remove(serving)}
                >
                  <Icon name="trash" size={16} />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}

      <form className={styles.form} onSubmit={submit} aria-busy={pending || undefined}>
        <div className={styles.formRow}>
          <TextField
            id="serving-name"
            label="Nombre"
            value={draft.name}
            disabled={pending}
            placeholder="Mediano"
            onChange={(event) => setDraft({ ...draft, name: event.target.value })}
          />
          <TextField
            id="serving-grams"
            label="Gramos"
            type="number"
            min="0.1"
            step="0.1"
            required
            value={draft.grams}
            disabled={pending}
            onChange={(event) => setDraft({ ...draft, grams: event.target.value })}
          />
        </div>
        <label className={styles.defaultToggle}>
          <input
            type="checkbox"
            checked={draft.isDefault}
            disabled={pending}
            onChange={(event) => setDraft({ ...draft, isDefault: event.target.checked })}
          />
          Es la ración por defecto
        </label>
        <div className={styles.actions}>
          {draft.id !== undefined && (
            <Button type="button" variant="ghost" onClick={() => setDraft(EMPTY)}>
              Cancelar
            </Button>
          )}
          <Button type="submit" disabled={pending}>
            {draft.id === undefined ? 'Añadir' : 'Guardar'}
          </Button>
          <Button type="button" variant="ghost" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </form>
    </div>
  );
}
