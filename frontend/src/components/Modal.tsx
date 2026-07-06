import { useEffect, useRef, type ReactNode } from 'react';
import styles from './Modal.module.css';

/**
 * Accessible modal dialog (FOR-18). Uses the native `<dialog>` element (rendered
 * with the `open` attribute rather than `showModal()`, which jsdom does not
 * implement) so it is a first-class interactive/accessible element. Dismissal is
 * available via the close button, the Escape key and a backdrop click.
 */
interface ModalProps {
  readonly title: string;
  readonly onClose: () => void;
  readonly children: ReactNode;
}

export function Modal({ title, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = 'modal-title';

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    // Move focus into the dialog when it opens (accessibility).
    dialogRef.current?.focus();
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <dialog
      ref={dialogRef}
      open
      className={styles.dialog}
      aria-labelledby={titleId}
      tabIndex={-1}
      // The dialog fills the viewport and acts as its own backdrop; a click that
      // lands on it (not on the inner panel) dismisses the modal.
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div className={styles.panel}>
        <header className={styles.header}>
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
          <button type="button" className={styles.close} onClick={onClose} aria-label="Cerrar">
            ×
          </button>
        </header>
        {children}
      </div>
    </dialog>
  );
}
