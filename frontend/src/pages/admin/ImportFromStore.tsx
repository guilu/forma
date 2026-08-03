import { useEffect, useState, type FormEvent } from 'react';
import { TextField } from '../../components/FormField';
import { Icon } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import {
  listStoreSuggestions,
  searchStoreProducts,
  type StoreId,
  type StoreProduct,
  type StoreSuggestion,
} from '../../api/storeProducts';
import { ProductThumbnail } from './ProductThumbnail';
import { priceLabel } from './storeDisplay';
import { useStores } from './useStores';
import styles from './ImportFromStore.module.css';

/**
 * Picks a product off a supermarket's own catalogue to seed a new catalog row
 * (FOR-194).
 *
 * <p>A confirmation step, not an import job. The shop can say what a product is
 * called, what it weighs and what it costs; it cannot say which food it is or
 * which of our six aisles it belongs to, and those are the two fields that make
 * the row useful. So this hands a filled-in draft to the ordinary create form and
 * gets out of the way — an imported product is stored by the same endpoint, and
 * ends up indistinguishable from a hand-typed one, because it should be.
 *
 * <p>Opened from a row of the Macros tab, so the food is settled before the
 * dialog exists. That is what keeps the link honest: you are always importing a
 * product *for* something in our catalog, so `foodId` is filled by construction
 * instead of being left for later — and it is why the action lives beside the
 * food and not beside the products.
 */
interface ImportFromStoreProps {
  readonly store: StoreId;
  /**
   * The food being shopped for, when the import started from a row of the Macros
   * tab. Absent means the admin is searching by name instead — the way in for
   * everything our own catalog cannot name.
   */
  readonly food?: CatalogFood;
  readonly onCancel: () => void;
  /** Hands over the draft for the create form to open on. */
  readonly onPicked: (draft: StoreProduct) => void;
}

type State =
  | { readonly status: 'idle' }
  | { readonly status: 'loading' }
  /**
   * The shop's own words when it gave any, or absent for "could not be reached".
   *
   * <p>What is kept is the failure, not the sentence. Naming the chain needs the
   * store list, which is a request of its own: a message frozen at the moment of
   * the error would still say MERCADONA after the names arrived, since nothing
   * re-renders a string sitting in state.
   */
  | { readonly status: 'error'; readonly message?: string }
  | { readonly status: 'ready'; readonly suggestions: StoreSuggestion[] };

export function ImportFromStore({ store, food, onCancel, onPicked }: ImportFromStoreProps) {
  const stores = useStores();
  const [state, setState] = useState<State>(
    // Nothing to show until something is typed: a search box that answers before
    // being asked would be listing the whole shop.
    food ? { status: 'loading' } : { status: 'idle' },
  );
  const [query, setQuery] = useState('');

  // The shop being unreachable is a 502 and reads as its own sentence: "no
  // existe" would be a different, wrong answer.
  const failed = (caught: unknown): State => ({
    status: 'error',
    message: caught instanceof ApiRequestError ? caught.message : undefined,
  });

  /** Built at render time so the chain reads by name as soon as the list lands. */
  const errorMessage = (message: string | undefined) =>
    message ?? `No se pudo consultar el catálogo de ${stores.label(store)}. Inténtalo de nuevo.`;

  function search(event: FormEvent) {
    event.preventDefault();
    if (query.trim() === '') {
      return;
    }
    setState({ status: 'loading' });
    searchStoreProducts(query.trim(), store)
      .then((suggestions) => setState({ status: 'ready', suggestions }))
      .catch((caught: unknown) => setState(failed(caught)));
  }

  useEffect(() => {
    if (!food) {
      return undefined;
    }
    let active = true;
    setState({ status: 'loading' });
    listStoreSuggestions(food.id, store)
      .then((suggestions) => {
        if (active) setState({ status: 'ready', suggestions });
      })
      .catch((caught: unknown) => {
        if (active) setState(failed(caught));
      });
    return () => {
      active = false;
    };
  }, [food, store]);

  /**
   * The draft handed to the create form. The id is derived from the shop's own,
   * which is stable across renames and reprices — ours would have to be invented
   * and would collide the second time the same product is imported.
   */
  const toDraft = (suggestion: StoreSuggestion): StoreProduct => ({
    id: `${store.toLowerCase()}-${suggestion.externalId}`,
    store,
    name: suggestion.name,
    foodId: food?.id,
    externalId: suggestion.externalId,
    // Echoed back untouched so the server can file the product on the shop's own
    // shelf. It resolves it against the aisles actually synced, and drops it
    // otherwise — this is a hint, not a foreign key the form is trusted with.
    storeCategoryExternalId: suggestion.storeCategoryExternalId,
    imageUrl: suggestion.imageUrl,
    packageSize: suggestion.packaging,
    priceEur: suggestion.priceEur,
    url: suggestion.url,
    // Their 151 shelves and our 6 aisles are different vocabularies; the admin
    // files it on the next screen rather than having a guess made for them.
    category: 'OTROS',
  });

  return (
    <div className={styles.wrapper}>
      {food ? (
        <p className={styles.lead}>{`Productos de ${stores.label(store)} para ${food.name}.`}</p>
      ) : (
        <form className={styles.search} onSubmit={search}>
          <TextField
            id="import-search"
            label={`Buscar en ${stores.label(store)}`}
            value={query}
            autoFocus
            placeholder="almendra natural"
            onChange={(event) => setQuery(event.target.value)}
          />
          <button type="submit" className={styles.searchButton} disabled={query.trim() === ''}>
            <Icon name="search" size={16} />
            Buscar
          </button>
        </form>
      )}

      {state.status === 'loading' && (
        <LoadingState message={`Buscando en ${stores.label(store)}…`} />
      )}

      {state.status === 'error' && (
        <p className={styles.error} role="alert">
          {errorMessage(state.message)}
        </p>
      )}

      {state.status === 'idle' && (
        <p className={styles.empty}>Escribe parte del nombre del producto y pulsa Buscar.</p>
      )}

      {state.status === 'ready' && state.suggestions.length === 0 && (
        <p className={styles.empty}>
          {`${stores.label(store)} no ha encontrado nada parecido. Puedes crear el producto a mano.`}
        </p>
      )}

      {state.status === 'ready' && state.suggestions.length > 0 && (
        <ul className={styles.results}>
          {state.suggestions.map((suggestion) => (
            <li key={suggestion.externalId} className={styles.result}>
              <ProductThumbnail url={suggestion.imageUrl} size={40} />
              <div className={styles.details}>
                <p className={styles.name}>{suggestion.name}</p>
                <p className={styles.meta}>
                  {[suggestion.packaging, priceLabel(suggestion.priceEur), suggestion.storeCategory]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
              </div>
              <button
                type="button"
                className={styles.pick}
                aria-label={`Usar ${suggestion.name}`}
                onClick={() => onPicked(toDraft(suggestion))}
              >
                Usar
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className={styles.actions}>
        <button type="button" className={styles.cancel} onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </div>
  );
}
