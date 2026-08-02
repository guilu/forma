import { useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { updateCategory, type CategoryDisplay } from '../../api/categories';
import styles from './FoodForm.module.css';

/**
 * Renames a category and changes its glyph (FOR-197).
 *
 * <p>No id field and no delete: the code is what every food and every product
 * points at, and the set of codes is closed in the backend's enums and in the
 * database's CHECK constraints. It is shown, read-only, because an admin editing
 * "Lácteo" should see what the rows underneath actually store.
 *
 * <p>The icon is optional. A category with no glyph renders as a name, which is
 * a perfectly good category — an empty box in its place would not be.
 */
interface CategoryFormProps {
  readonly category: CategoryDisplay;
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

export function CategoryForm({ category, onCancel, onSaved }: CategoryFormProps) {
  const notify = useNotify();
  const [label, setLabel] = useState(category.label);
  const [icon, setIcon] = useState(category.icon ?? '');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      await updateCategory(category.scope, category.code, {
        label: label.trim(),
        icon: icon.trim() === '' ? undefined : icon.trim(),
      });
      notify.success('Categoría actualizada.');
      onSaved();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar la categoría. Inténtalo de nuevo.',
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

      <TextField id="category-code" label="Código" value={category.code} disabled readOnly />
      <TextField
        id="category-label"
        label="Nombre"
        value={label}
        required
        disabled={pending}
        onChange={(event) => setLabel(event.target.value)}
      />
      <TextField
        id="category-icon"
        label="Icono"
        value={icon}
        disabled={pending}
        maxLength={4}
        placeholder="🥛"
        onChange={(event) => setIcon(event.target.value)}
      />

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
