import { useEffect, useState } from 'react';
import { LoadingState } from '../../components/LoadingState';
import { ApiRequestError } from '../../api/client';
import type { CatalogFood } from '../../api/foods';
import {
  listStoreSuggestions,
  type Store,
  type StoreProduct,
  type StoreSuggestion,
} from '../../api/storeProducts';
import { ProductThumbnail } from './ProductThumbnail';
import { STORE_LABELS, priceLabel } from './storeDisplay';
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
  readonly store: Store;
  /** The food being shopped for. */
  readonly food: CatalogFood;
  readonly onCancel: () => void;
  /** Hands over the draft for the create form to open on. */
  readonly onPicked: (draft: StoreProduct) => void;
}

type State =
  | { readonly status: 'idle' }
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly suggestions: StoreSuggestion[] };

export function ImportFromStore({ store, food, onCancel, onPicked }: ImportFromStoreProps) {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    listStoreSuggestions(food.id, store)
      .then((suggestions) => {
        if (active) setState({ status: 'ready', suggestions });
      })
      .catch((caught: unknown) => {
        if (!active) return;
        setState({
          status: 'error',
          // The shop being unreachable is a 502 and reads as its own sentence:
          // "no existe" would be a different, wrong answer.
          message:
            caught instanceof ApiRequestError
              ? caught.message
              : `No se pudo consultar el catálogo de ${STORE_LABELS[store]}. Inténtalo de nuevo.`,
        });
      });
    return () => {
      active = false;
    };
  }, [food.id, store]);

  /**
   * The draft handed to the create form. The id is derived from the shop's own,
   * which is stable across renames and reprices — ours would have to be invented
   * and would collide the second time the same product is imported.
   */
  const toDraft = (suggestion: StoreSuggestion): StoreProduct => ({
    id: `${store.toLowerCase()}-${suggestion.externalId}`,
    store,
    name: suggestion.name,
    foodId: food.id,
    externalId: suggestion.externalId,
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
      <p className={styles.lead}>{`Productos de ${STORE_LABELS[store]} para ${food.name}.`}</p>

      {state.status === 'loading' && (
        <LoadingState message={`Buscando en ${STORE_LABELS[store]}…`} />
      )}

      {state.status === 'error' && (
        <p className={styles.error} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'ready' && state.suggestions.length === 0 && (
        <p className={styles.empty}>
          {`${STORE_LABELS[store]} no ha encontrado nada parecido. Puedes crear el producto a mano.`}
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
