import { useEffect, useRef, type ReactNode } from 'react';
import styles from './Modal.module.css';

/**
 * Accessible modal dialog (FOR-18). Uses the native `<dialog>` element (rendered
 * with the `open` attribute rather than `showModal()`, which jsdom does not
 * implement) so it is a first-class interactive/accessible element. Dismissal is
 * available via the close button, the Escape key and a backdrop click.
 *
 * <p>FOR-61 accessibility hardening: focus is trapped inside the dialog while it
 * is open (`Tab`/`Shift+Tab` cycle through the dialog's own focusable elements
 * instead of escaping into the page behind it), and the element that had focus
 * before the modal opened regains it when the modal closes — otherwise focus
 * would silently drop back to `<body>`, disorienting keyboard/screen-reader
 * users (spec `specs/FOR-61/ui.md`: "Accessible modal: focus trap, restore
 * focus, Esc to close").
 */
interface ModalProps {
  readonly title: string;
  readonly onClose: () => void;
  readonly children: ReactNode;
}

/** Elements a keyboard user can land on; mirrors the standard focus-trap selector. */
const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'textarea:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

export function Modal({ title, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = 'modal-title';

  useEffect(() => {
    // Remember what had focus before the modal opened so it can be restored
    // when it closes (FOR-61: "restore focus").
    const previouslyFocused = document.activeElement as HTMLElement | null;

    function focusableElements(): HTMLElement[] {
      const dialog = dialogRef.current;
      return dialog ? Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)) : [];
    }

    function trapTab(event: KeyboardEvent) {
      const elements = focusableElements();
      if (elements.length === 0) {
        event.preventDefault();
        dialogRef.current?.focus();
        return;
      }
      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
        return;
      }
      if (event.key === 'Tab') {
        trapTab(event);
      }
    }
    document.addEventListener('keydown', onKeyDown);
    // Move focus into the dialog when it opens (accessibility).
    dialogRef.current?.focus();
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previouslyFocused?.focus();
    };
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
