import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/Button';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import {
  listStoreCategories,
  syncStoreCategories,
  type StoreCategory,
} from '../../api/storeCategories';
import styles from './StoreAislesManager.module.css';

/**
 * A shop's own aisles (V46).
 *
 * <p>Read only, and that is not an omission. These rows are a copy of somebody
 * else's words: renaming one here would be editing what Mercadona calls its own
 * shelf, and the next sync would undo it. Our six aisles are the ones a person
 * chooses, and they live on the product.
 *
 * <p>The one action is asking the shop again. It hits somebody else's server, so
 * it is admin only and says how many aisles came back.
 */
interface StoreAislesManagerProps {
  readonly storeId: string;
  readonly storeName: string;
  readonly onClose: () => void;
}

/** Indented by the level the row carries rather than by walking the parent chain. */
const levelClass = (level: number) =>
  level === 0 ? styles.level0 : level === 1 ? styles.level1 : styles.level2;

export function StoreAislesManager({ storeId, storeName, onClose }: StoreAislesManagerProps) {
  const notify = useNotify();
  const [aisles, setAisles] = useState<StoreCategory[] | undefined>(undefined);
  const [loadError, setLoadError] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const reload = useCallback(async () => {
    try {
      setAisles(await listStoreCategories(storeId));
      setLoadError(false);
    } catch {
      setLoadError(true);
    }
  }, [storeId]);

  useEffect(() => {
    void reload();
  }, [reload]);

  async function sync() {
    if (syncing) return;
    setSyncing(true);
    setError(undefined);
    try {
      const fresh = await syncStoreCategories(storeId);
      setAisles(fresh);
      notify.success(`${storeName}: ${fresh.length} pasillos.`);
    } catch (caught) {
      // A chain with no catalogue behind it answers 404, and that is a real
      // answer rather than a failure — OTRAS is where things bought at a market
      // stall go, and there is nothing to ask it.
      setError(
        caught instanceof ApiRequestError
          ? caught.message
          : `No se pudo consultar los pasillos de ${storeName}. Inténtalo de nuevo.`,
      );
    } finally {
      setSyncing(false);
    }
  }

  if (loadError) {
    return (
      <ErrorState message="No se pudieron cargar los pasillos." onRetry={() => void reload()} />
    );
  }
  if (aisles === undefined) {
    return <LoadingState message="Cargando pasillos…" />;
  }

  return (
    <div className={styles.wrapper}>
      <p className={styles.lead}>
        {`Cómo ${storeName} ordena sus estantes. Se copia tal cual y no se edita aquí: la próxima sincronización lo devolvería a como lo tenga la tienda.`}
      </p>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      {aisles.length === 0 ? (
        <p className={styles.empty}>{`Todavía no se le han pedido los pasillos a ${storeName}.`}</p>
      ) : (
        <ul className={styles.list}>
          {aisles.map((aisle) => (
            <li key={aisle.id} className={`${styles.row} ${levelClass(aisle.level)}`}>
              <span className={styles.name}>{aisle.name}</span>
              {/* The shop's own id: it is the identity these rows are keyed on, and
                  seeing it is what makes a re-sync explicable when a name changes. */}
              <span className={styles.externalId}>{aisle.externalId}</span>
            </li>
          ))}
        </ul>
      )}

      <div className={styles.actions}>
        <Button type="button" onClick={() => void sync()} disabled={syncing}>
          {syncing ? 'Consultando…' : 'Sincronizar'}
        </Button>
        <Button type="button" variant="ghost" onClick={onClose}>
          Cerrar
        </Button>
      </div>
    </div>
  );
}
