import { Link } from 'react-router-dom';
import { PagePlaceholder } from '../components/PagePlaceholder';

export function NotFoundPage() {
  return (
    <div>
      <PagePlaceholder title="Página no encontrada" description="La ruta solicitada no existe." />
      <p style={{ marginTop: 'var(--space-4)' }}>
        <Link to="/">Volver al Dashboard</Link>
      </p>
    </div>
  );
}
