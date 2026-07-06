import { useState } from 'react';
import { MeasurementForm } from '../components/MeasurementForm';
import { Modal } from '../components/Modal';
import styles from './MeasurementsPage.module.css';

/**
 * Measurements page (FOR-18). Header with a "+ Registrar medición" action that
 * opens the entry form in a modal. The composition dashboard (summary cards,
 * charts, history — per the product mockup) is intentionally left empty here and
 * built by later stories (FOR-19/FOR-20).
 */
export function MeasurementsPage() {
  const [formOpen, setFormOpen] = useState(false);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Mediciones</h1>
          <p className={styles.subtitle}>Controla tu composición corporal y evolución.</p>
        </div>
        <button className={styles.action} type="button" onClick={() => setFormOpen(true)}>
          + Registrar medición
        </button>
      </header>

      {formOpen && (
        <Modal title="Registrar medición" onClose={() => setFormOpen(false)}>
          <MeasurementForm onCancel={() => setFormOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
