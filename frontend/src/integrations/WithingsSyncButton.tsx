import { useState } from 'react';
import { Icon } from '../components/Icon';
import { IconButton } from '../components/IconButton';
import { useNotify } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import { syncIntegration } from '../api/integrations';
import { useIntegrations } from './IntegrationsContext';

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
 * <p>It is an {@link IconButton} — the app's icon-only square — rather than a
 * {@link Button} holding a glyph where its label belongs: the first draft was a
 * `secondary` Button with its label padding trimmed by hand, which is exactly
 * the per-caller re-declaration IconButton was consolidated to end. `lg` (44px)
 * matches the page action it sits beside on Mediciones; `sm` (32px) is the
 * dense square for the sidebar's status card. The label lives in `label`
 * (the accessible name) and in `title` (the hover tooltip); the glyph itself is
 * `aria-hidden` via {@link Icon}. Both must say the same words — the tooltip is
 * the only text a sighted user gets.
 *
 * <p>A sync in flight disables the control and announces `aria-busy` (no
 * spinner: IconButton has no room for one beside its glyph, and swapping the
 * icon out would drop the only thing identifying the button).
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
    <IconButton
      label={LABEL}
      title={LABEL}
      variant="surface"
      // `lg` is the 44px page-action square — the height of the register
      // action it sits beside. `sm` is the dense one, for the sidebar card.
      size={size === 'sm' ? 'sm' : 'lg'}
      disabled={pending}
      aria-busy={pending || undefined}
      onClick={handleSync}
    >
      <Icon name="refresh" size={size === 'sm' ? 16 : 18} />
    </IconButton>
  );
}
