import { useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import {
  createStoreProduct,
  updateStoreProduct,
  type ShoppingCategory,
  type Store,
  type StoreProduct,
} from '../../api/storeProducts';
import {
  SHOPPING_CATEGORY_LABELS,
  SHOPPING_CATEGORY_OPTIONS,
  STORE_LABELS,
  STORE_OPTIONS,
} from './storeDisplay';
import styles from './FoodForm.module.css';

/**
 * Create/edit form for a store catalog product (FOR-191).
 *
 * <p>The price is the price of the package named beside it, not a weekly cost:
 * what a person spends on a product in a week depends on their plan and belongs
 * to their list, not to the catalog everyone shares.
 *
 * <p>The linked food is a select over the food catalog rather than a free-text
 * id — it is a foreign key, and a typo would be refused by the database with an
 * error nobody can act on. "Sin enlazar" is a legitimate choice: a product with
 * no nutritional counterpart is still buyable.
 *
 * <p>The id is only editable while creating, like the food form: it is the
 * catalog's stable handle and an edit would rename a row out from under whatever
 * points at it.
 */
interface StoreProductFormProps {
  /** Absent when creating. */
  readonly product?: StoreProduct;
  /**
   * Initial values for a NEW product, e.g. one picked off a store's catalogue.
   * Distinct from `product`: this still creates, so the id stays editable and
   * the save is a POST — a draft is not a row yet.
   */
  readonly draft?: StoreProduct;
  /** The food catalog, for the link select. */
  readonly foods: readonly CatalogFood[];
  readonly onCancel: () => void;
  readonly onSaved: () => void;
}

export function StoreProductForm({
  product,
  draft,
  foods,
  onCancel,
  onSaved,
}: StoreProductFormProps) {
  const notify = useNotify();
  const creating = product === undefined;
  // The row being edited when there is one, the draft when creating from an
  // import, and empty when creating from scratch.
  const initial = product ?? draft;
  const [id, setId] = useState(initial?.id ?? '');
  const [store, setStore] = useState<Store>(initial?.store ?? 'MERCADONA');
  const [name, setName] = useState(initial?.name ?? '');
  const [foodId, setFoodId] = useState(initial?.foodId ?? '');
  const [packageSize, setPackageSize] = useState(initial?.packageSize ?? '');
  const [priceEur, setPriceEur] = useState(
    initial?.priceEur === undefined ? '' : String(initial.priceEur),
  );
  const [url, setUrl] = useState(initial?.url ?? '');
  const [category, setCategory] = useState<ShoppingCategory>(initial?.category ?? 'OTROS');
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    // A blank optional stays absent rather than becoming an empty string or a
    // zero: "not priced yet" and "free" are different facts.
    const optionalText = (value: string) => (value.trim() === '' ? undefined : value.trim());
    const payload: StoreProduct = {
      id: id.trim(),
      store,
      name: name.trim(),
      foodId: optionalText(foodId),
      packageSize: optionalText(packageSize),
      priceEur: priceEur.trim() === '' ? undefined : Number(priceEur),
      url: optionalText(url),
      category,
      notes: optionalText(notes),
    };
    try {
      if (creating) {
        await createStoreProduct(payload);
        notify.success('Producto creado.');
      } else {
        await updateStoreProduct(product.id, payload);
        notify.success('Producto actualizado.');
      }
      onSaved();
    } catch (caught) {
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo guardar el producto. Inténtalo de nuevo.',
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
        id="product-id"
        label="Identificador"
        value={id}
        required
        // Read-only on edit: it is what a link to this product points at.
        disabled={!creating || pending}
        pattern="[a-z0-9-]+"
        onChange={(event) => setId(event.target.value)}
      />
      <TextField
        id="product-name"
        label="Nombre"
        value={name}
        required
        disabled={pending}
        onChange={(event) => setName(event.target.value)}
      />
      <SelectField
        id="product-store"
        label="Tienda"
        value={store}
        disabled={pending}
        onChange={(event) => setStore(event.target.value as Store)}
      >
        {STORE_OPTIONS.map((option) => (
          <option key={option} value={option}>
            {STORE_LABELS[option]}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="product-category"
        label="Categoría"
        value={category}
        disabled={pending}
        onChange={(event) => setCategory(event.target.value as ShoppingCategory)}
      >
        {SHOPPING_CATEGORY_OPTIONS.map((option) => (
          <option key={option} value={option}>
            {SHOPPING_CATEGORY_LABELS[option]}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="product-food"
        label="Alimento enlazado"
        value={foodId}
        disabled={pending}
        onChange={(event) => setFoodId(event.target.value)}
      >
        <option value="">Sin enlazar</option>
        {/* A select whose value matches no option renders blank, so a product
            whose food is missing from the list — the catalog request failed, or
            the food was deleted — would read as "Sin enlazar" over a link that
            is still there. The control would be lying about the data. */}
        {foodId !== '' && !foods.some((food) => food.id === foodId) && (
          <option value={foodId}>{foodId} (no está en el catálogo)</option>
        )}
        {foods.map((food) => (
          <option key={food.id} value={food.id}>
            {food.name}
          </option>
        ))}
      </SelectField>

      <div className={styles.macros}>
        <TextField
          id="product-package"
          label="Formato"
          value={packageSize}
          disabled={pending}
          placeholder="500 g"
          onChange={(event) => setPackageSize(event.target.value)}
        />
        <TextField
          id="product-price"
          label="Precio (€)"
          type="number"
          min="0"
          step="0.01"
          value={priceEur}
          disabled={pending}
          onChange={(event) => setPriceEur(event.target.value)}
        />
      </div>

      <TextField
        id="product-url"
        label="Enlace"
        type="url"
        value={url}
        disabled={pending}
        onChange={(event) => setUrl(event.target.value)}
      />
      <TextField
        id="product-notes"
        label="Notas"
        value={notes}
        disabled={pending}
        onChange={(event) => setNotes(event.target.value)}
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
