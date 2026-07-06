import { MeasurementForm } from '../components/MeasurementForm';
import styles from './MeasurementsPage.module.css';

/**
 * Measurements page (FOR-18). Replaces the FOR-81 placeholder with the body
 * measurement entry form. A measurement list/dashboard is a later story
 * (FOR-19); until then, a successful save is confirmed inline by the form.
 */
export function MeasurementsPage() {
  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Mediciones</h1>
      </header>
      <MeasurementForm />
    </div>
  );
}
