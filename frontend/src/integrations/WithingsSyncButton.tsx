import { useState } from 'react';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { useNotify } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import { syncIntegration } from '../api/integrations';
import { useIntegrations } from './IntegrationsContext';
import styles from './WithingsSyncButton.module.css';

/**
 * "Sincronizar Withings" — the manual-sync control the settings screen already
 * offered, lifted out so the screens that show Withings data can offer it where
 * the data is, instead of sending the user to Ajustes to press it.
 *
 * <p>It renders only while the shared {@link useIntegrations} store says
 * Withings is CONNECTED: with no connection there is nothing to sync, and a
 * control that cannot work is worse than no control. While the state is
 * unknown — in flight, or the request failed — it stays absent for the same
 * reason the sidebar's status card does (FOR-189).
 *
 * <p>Icon-only, so the label lives in `aria-label` (the accessible name) and in
 * `title` (the hover tooltip); the glyph itself is `aria-hidden` via
 * {@link Icon}. Both must say the same words — the tooltip is the only text a
 * sighted user gets.
 *
 * <p>Outcome handling mirrors `IntegrationsSection.handleSync`, deliberately:
 * a resolved sync with `result: 'NOT_CONNECTED'` is *not* a success and must
 * not produce a success toast (FOR-123 — never fabricate feedback for a call
 * that synced nothing). The difference is where the failure lands: this button
 * has no section around it to hold an inline error, so it reports through the
 * shared toast region instead.
 *
 * <p>A completed sync calls `refresh()` rather than patching anything locally —
 * `lastSyncAt` changed server-side, and the settings screen and sidebar card
 * read the same store.
 */
const LABEL = 'Sincronizar Withings';

export function WithingsSyncButton({ size = 'md' }: { readonly size?: 'md' | 'sm' }) {
  const notify = useNotify();
  const { status, connections, refresh } = useIntegrations();
  const [pending, setPending] = useState(false);

  const connected =
    status === 'ready' &&
    connections.some((c) => c.providerId === 'WITHINGS' && c.status === 'CONNECTED');

  if (!connected) return null;

  async function handleSync() {
    setPending(true);
    try {
      const outcome = await syncIntegration('WITHINGS');
      if (outcome.result === 'NOT_CONNECTED') {
        notify.error('No se pudo sincronizar Withings: ya no está conectado.');
      } else {
        notify.success('Sincronizado con Withings.');
        refresh();
      }
    } catch (error) {
      notify.error(
        error instanceof ApiRequestError ? error.message : 'No se pudo sincronizar Withings.',
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <Button
      variant="secondary"
      type="button"
      aria-label={LABEL}
      title={LABEL}
      className={[styles.syncButton, size === 'sm' ? styles.small : ''].filter(Boolean).join(' ')}
      loading={pending}
      onClick={handleSync}
    >
      <Icon name="refresh" size={size === 'sm' ? 14 : 18} />
    </Button>
  );
}
