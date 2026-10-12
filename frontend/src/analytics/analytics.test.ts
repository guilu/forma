import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ANALYTICS_CONSENT_KEY,
  GA_SCRIPT_ID,
  grantAnalyticsConsent,
  initializeAnalyticsFromConsent,
  revokeAnalyticsConsent,
  trackPageView,
} from './analytics';

const MEASUREMENT_ID = 'G-GGBK0B9LNN';

function calls() {
  return (window.dataLayer ?? []) as unknown[][];
}

describe('consent-safe GA4 analytics', () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie.split(';').forEach((cookie) => {
      document.cookie = `${cookie.split('=')[0]?.trim()}=; Max-Age=0; path=/`;
    });
    document.getElementById(GA_SCRIPT_ID)?.remove();
    delete window.dataLayer;
    delete window.gtag;
  });

  it('does not load or configure GA4 without an explicit persisted grant', () => {
    initializeAnalyticsFromConsent(MEASUREMENT_ID);

    expect(document.getElementById(GA_SCRIPT_ID)).not.toBeInTheDocument();
    expect(window.dataLayer).toBeUndefined();
  });

  it('persists consent and appends the GA4 loader only once', () => {
    grantAnalyticsConsent(MEASUREMENT_ID);
    grantAnalyticsConsent(MEASUREMENT_ID);

    expect(localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('granted');
    const scripts = document.querySelectorAll(`#${GA_SCRIPT_ID}`);
    expect(scripts).toHaveLength(1);
    expect(scripts[0]).toHaveAttribute(
      'src',
      `https://www.googletagmanager.com/gtag/js?id=${MEASUREMENT_ID}`,
    );
    expect(calls()).toContainEqual(['config', MEASUREMENT_ID, { send_page_view: false }]);
  });

  it('loads GA4 on a later visit only for a persisted grant', () => {
    localStorage.setItem(ANALYTICS_CONSENT_KEY, 'granted');

    initializeAnalyticsFromConsent(MEASUREMENT_ID);

    expect(document.getElementById(GA_SCRIPT_ID)).toBeInTheDocument();
  });

  it('dispatches page_view only with live consent and without query or hash data', () => {
    expect(trackPageView('/plan?email=persona@example.com#resultado')).toBe(false);

    grantAnalyticsConsent(MEASUREMENT_ID);
    expect(trackPageView('/plan?email=persona@example.com#resultado')).toBe(true);

    expect(calls()).toContainEqual(['event', 'page_view', { page_path: '/plan' }]);
  });

  it('withdraws consent, removes the loader and expires only GA cookies', () => {
    grantAnalyticsConsent(MEASUREMENT_ID);
    document.cookie = '_ga=GA1.1.123; path=/';
    document.cookie = '_ga_TEST=GS1.1.123; path=/';
    document.cookie = 'XSRF-TOKEN=keep; path=/';
    const cookieSetter = vi.spyOn(Document.prototype, 'cookie', 'set');

    revokeAnalyticsConsent();

    expect(localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('denied');
    expect(document.getElementById(GA_SCRIPT_ID)).not.toBeInTheDocument();
    expect(document.cookie).toContain('XSRF-TOKEN=keep');
    expect(document.cookie).not.toContain('_ga=');
    expect(document.cookie).not.toContain('_ga_TEST=');
    expect(cookieSetter.mock.calls.flat().join('\n')).toContain('Domain=localhost');
    expect(calls()).toContainEqual([
      'consent',
      'update',
      expect.objectContaining({ analytics_storage: 'denied' }),
    ]);
  });
});
