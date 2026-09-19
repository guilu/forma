import { useId, useState } from 'react';
import { Button } from './Button';
import { TextField } from './FormField';
import { Modal } from './Modal';
import styles from './TypedConfirmDialog.module.css';

/**
 * A stronger confirmation than {@link ConfirmDialog}: instead of one click, the caller must
 * type an exact word before the destructive action can even be reached. Built for deleting a
 * nutrition plan (PlansPage): a click-to-confirm dialog is the right amount of friction for
 * archiving or disconnecting something recoverable, but a plan and everything under it is gone
 * for good, and nothing here asks "are you sure?" twice — it asks once, in a way that is hard to
 * do by reflex.
 *
 * <p>Same shape as {@link ConfirmDialog} on purpose (same {@link Modal}, same cancel-has-no-effect
 * guarantee inherited from it), so the two read as one family rather than two unrelated dialogs.
 * {@link ConfirmDialog} itself is untouched — its other callers (disconnect an integration,
 * archive a plan) stay on the lighter pattern; this component is additive, not a replacement.
 *
 * <p>The match is case-insensitive and trimmed: {@code confirmWord} states what must be typed in
 * its canonical form, and a caller should not be blocked by Caps Lock or a trailing space.
 */
interface TypedConfirmDialogProps {
  readonly title: string;
  readonly message: string;
  /** The word the user must type, case-insensitively and trimmed, to enable the confirm action. */
  readonly confirmWord: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  /** Disables + shows a spinner on the confirm action while the request is in flight. */
  readonly pending?: boolean;
  readonly onConfirm: () => void;
  readonly onCancel: () => void;
}

export function TypedConfirmDialog({
  title,
  message,
  confirmWord,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  pending = false,
  onConfirm,
  onCancel,
}: TypedConfirmDialogProps) {
  const [typed, setTyped] = useState('');
  const inputId = useId();
  const matches = typed.trim().toLowerCase() === confirmWord.trim().toLowerCase();

  return (
    <Modal title={title} onClose={onCancel}>
      <p className={styles.message}>{message}</p>
      <div className={styles.field}>
        <TextField
          id={inputId}
          label={`Escribe "${confirmWord}" para confirmar`}
          value={typed}
          onChange={(event) => setTyped(event.target.value)}
          autoComplete="off"
          autoFocus
          disabled={pending}
        />
      </div>
      <div className={styles.actions}>
        <Button variant="secondary" type="button" onClick={onCancel} disabled={pending}>
          {cancelLabel}
        </Button>
        <Button
          variant="destructive"
          type="button"
          loading={pending}
          disabled={!matches}
          onClick={onConfirm}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
