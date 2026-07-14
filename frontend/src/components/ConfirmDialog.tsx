import { Button } from './Button';
import { Modal } from './Modal';
import styles from './ConfirmDialog.module.css';

/**
 * Shared destructive-action confirmation pattern (FOR-63): generalizes the
 * ad-hoc "¿Seguro que quieres desconectar…?" confirm modal `IntegrationsSection`
 * (FOR-57) built directly on {@link Modal}, so every delete/disconnect flow
 * gets the same explicit-confirm behavior instead of re-deriving it (spec
 * `specs/FOR-63/spec.md`: "Destructive-action confirmation: explicit confirm
 * for delete/disconnect (reuse Modal.tsx)").
 *
 * <p>Cancelling (close button, Escape, backdrop click — all wired by
 * {@link Modal}, FOR-61) only calls `onCancel`; it never calls `onConfirm`,
 * so a cancelled destructive action always has no side effect (spec
 * `specs/FOR-63/tests.md` edge case).
 */
interface ConfirmDialogProps {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  /** Disables + shows a spinner on the confirm action while the request is in flight. */
  readonly pending?: boolean;
  readonly onConfirm: () => void;
  readonly onCancel: () => void;
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  pending = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <Button variant="secondary" type="button" onClick={onCancel} disabled={pending}>
          {cancelLabel}
        </Button>
        <Button variant="destructive" type="button" loading={pending} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
