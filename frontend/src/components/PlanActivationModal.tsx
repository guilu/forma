import { Button } from './Button';
import { Modal } from './Modal';
import styles from './PlanActivationModal.module.css';

/**
 * The first question after logging in: your plan is written, do you want to start it?
 *
 * <p>Built on {@link Modal} rather than on {@link ConfirmDialog}, because the shape is the other
 * way round. There the confirm button IS the destructive one; here the main action is accepting a
 * plan, and declining is what carries the warning colour — asked for deliberately, so that "later"
 * reads as the answer that leaves the app empty rather than as the easy way out.
 *
 * <p>Declining persists nothing. The question comes back next session, which is the point: an
 * account with a plan it never started should keep being asked rather than be quietly left with
 * three blank screens and no way back to them.
 */
interface PlanActivationModalProps {
  readonly planName?: string;
  /** Disables both answers while the activation request is in flight. */
  readonly pending?: boolean;
  /** Shown inside the modal; the modal stays open so the answer is not lost. */
  readonly error?: string;
  readonly onAccept: () => void;
  readonly onDismiss: () => void;
}

export function PlanActivationModal({
  planName,
  pending = false,
  error,
  onAccept,
  onDismiss,
}: PlanActivationModalProps) {
  return (
    <Modal title="Tu plan está listo" onClose={onDismiss}>
      <p className={styles.message}>
        {planName ? (
          <>
            Hemos preparado <strong>{planName}</strong> para ti. Actívalo y lo verás en
            entrenamiento, nutrición y tu lista de compra.
          </>
        ) : (
          <>
            Hemos preparado un plan para ti. Actívalo y lo verás en entrenamiento, nutrición y tu
            lista de compra.
          </>
        )}
      </p>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <div className={styles.actions}>
        <Button type="button" loading={pending} onClick={onAccept}>
          Sí, activa mi plan
        </Button>
        <Button variant="destructive" type="button" disabled={pending} onClick={onDismiss}>
          No, lo haré en otro momento
        </Button>
      </div>
    </Modal>
  );
}
