import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import {
  deleteProgressPhoto,
  fetchProgressPhotoBlob,
  listProgressPhotos,
  uploadProgressPhoto,
  type ProgressPhoto,
} from '../../api/progress';
import styles from './ProgressPhotosSection.module.css';

/**
 * Progress-photos gallery/upload section (FOR-144), consuming the FOR-140
 * backend. Mockup: "FOTOS DE PROGRESO" in `docs/6-progreso.png`, mounted on
 * the Progreso page (`ProgressPage.tsx`) alongside the measurement charts and
 * insights sections — the mockup shows no equivalent card on the Objetivos
 * page (`docs/7-objetivos.png`).
 *
 * <p>Loads/lists/deletes metadata through the shared {@link apiClient}
 * boundary (ADR-006). Each thumbnail's binary is never a public/static
 * `<img src>` — it is fetched individually through the owner-scoped
 * `fetchProgressPhotoBlob` (authenticated fetch → `Blob`), turned into an
 * object URL, and that object URL is revoked when the thumbnail unmounts
 * (deleted, or the gallery reloads) to avoid leaking memory or exposing the
 * bytes past the component's life (spec FOR-140: privacy-sensitive,
 * access-controlled binary).
 *
 * <p>States follow FOR-60: {@link WidgetLoading} skeleton while the list
 * request is in flight (this is a content-area widget within the page, same
 * convention as `TrainingPage`'s "músculos trabajados"/"racha" sections, not
 * a full-page {@link LoadingState}), {@link EmptyState} for the normal
 * "no photos yet" case (never treated as an error), {@link ErrorState} with
 * retry on a failed list load. Delete goes through the shared FOR-63
 * {@link ConfirmDialog} destructive-confirmation pattern.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly photos: ProgressPhoto[] };

/** Matches the FOR-140 content-type allow-list (`ProgressPhotoService`); narrows the file picker only. */
const ACCEPTED_TYPES = 'image/jpeg,image/png';

function messageFromError(error: unknown, fallback: string): string {
  return error instanceof ApiRequestError ? error.message : fallback;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function ProgressPhotosSection() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [deleteTarget, setDeleteTarget] = useState<ProgressPhoto | undefined>(undefined);
  const [deletePending, setDeletePending] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    listProgressPhotos()
      .then((photos) => {
        if (active) {
          setState({ status: 'ready', photos });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [reloadToken]);

  async function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Reset so selecting the exact same file again still fires onChange.
    event.target.value = '';
    if (!file) {
      return;
    }
    setActionError(undefined);
    setUploading(true);
    try {
      const photo = await uploadProgressPhoto(file);
      setState((prev) => ({
        status: 'ready',
        photos: [photo, ...(prev.status === 'ready' ? prev.photos : [])],
      }));
      notify.success('Foto de progreso subida.');
    } catch (error) {
      setActionError(messageFromError(error, 'No se pudo subir la foto.'));
    } finally {
      setUploading(false);
    }
  }

  async function handleDeleteConfirm() {
    if (!deleteTarget) {
      return;
    }
    const target = deleteTarget;
    setActionError(undefined);
    setDeletePending(true);
    try {
      await deleteProgressPhoto(target.id);
      setState((prev) =>
        prev.status === 'ready'
          ? { status: 'ready', photos: prev.photos.filter((p) => p.id !== target.id) }
          : prev,
      );
      notify.success('Foto de progreso eliminada.');
    } catch (error) {
      setActionError(messageFromError(error, 'No se pudo eliminar la foto.'));
    } finally {
      setDeletePending(false);
      setDeleteTarget(undefined);
    }
  }

  return (
    <Card
      title="Fotos de progreso"
      headingLevel={2}
      action={
        <>
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPTED_TYPES}
            className={styles.hiddenInput}
            onChange={handleFileSelected}
            aria-label="Subir foto de progreso"
          />
          <Button
            type="button"
            variant="secondary"
            loading={uploading}
            onClick={() => fileInputRef.current?.click()}
          >
            <Icon name="upload" size={16} />
            Subir foto
          </Button>
        </>
      }
    >
      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}

      {renderContent(state, setDeleteTarget, () => setReloadToken((t) => t + 1))}

      {deleteTarget && (
        <ConfirmDialog
          title="Eliminar foto de progreso"
          message="¿Seguro que quieres eliminar esta foto? Esta acción no se puede deshacer."
          confirmLabel="Eliminar"
          pending={deletePending}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteTarget(undefined)}
        />
      )}
    </Card>
  );
}

function renderContent(
  state: State,
  onRequestDelete: (photo: ProgressPhoto) => void,
  onRetry: () => void,
) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando fotos de progreso…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudieron cargar tus fotos de progreso. Inténtalo de nuevo."
        onRetry={onRetry}
      />
    );
  }

  if (state.photos.length === 0) {
    return (
      <EmptyState
        title="Aún no tienes fotos de progreso."
        description="Sube tu primera foto para comparar tu evolución visual."
      />
    );
  }

  return (
    <ul className={styles.grid}>
      {state.photos.map((photo) => (
        <li key={photo.id}>
          <ProgressPhotoThumbnail photo={photo} onDelete={() => onRequestDelete(photo)} />
        </li>
      ))}
    </ul>
  );
}

type ThumbnailState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly objectUrl: string };

function ProgressPhotoThumbnail({
  photo,
  onDelete,
}: {
  readonly photo: ProgressPhoto;
  readonly onDelete: () => void;
}) {
  const [state, setState] = useState<ThumbnailState>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    let objectUrl: string | undefined;
    setState({ status: 'loading' });
    fetchProgressPhotoBlob(photo.id)
      .then((blob) => {
        if (!active) {
          return;
        }
        objectUrl = URL.createObjectURL(blob);
        setState({ status: 'ready', objectUrl });
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
      // Revoke on unmount (deleted photo, or a reload replacing this
      // thumbnail) — never let an object URL for privacy-sensitive photo
      // bytes outlive the component that displays it.
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [photo.id]);

  const dateLabel = formatDate(photo.createdAt);

  return (
    <figure className={styles.thumbnail}>
      {state.status === 'ready' && (
        <img
          src={state.objectUrl}
          alt={`Foto de progreso del ${dateLabel}`}
          className={styles.image}
        />
      )}
      {state.status === 'error' && (
        <div className={styles.thumbnailError} role="alert">
          No se pudo cargar esta foto.
        </div>
      )}
      {state.status === 'loading' && (
        <div className={styles.thumbnailLoading} role="status" aria-label="Cargando foto…" />
      )}
      <figcaption className={styles.caption}>
        <span>{dateLabel}</span>
        <button
          type="button"
          className={styles.deleteButton}
          aria-label={`Eliminar foto del ${dateLabel}`}
          onClick={onDelete}
        >
          <Icon name="trash" size={16} />
        </button>
      </figcaption>
    </figure>
  );
}
