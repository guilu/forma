export const ANALYTICS_CONSENT_KEY = 'forma.analytics-consent';
export const GA_SCRIPT_ID = 'forma-ga4-loader';

type ConsentDecision = 'granted' | 'denied';
type GtagArguments = [command: string, ...args: unknown[]];

declare global {
  interface Window {
    dataLayer?: GtagArguments[];
    gtag?: (...args: GtagArguments) => void;
  }
}

function readConsent(): ConsentDecision | null {
  const decision = window.localStorage.getItem(ANALYTICS_CONSENT_KEY);
  return decision === 'granted' || decision === 'denied' ? decision : null;
}

function ensureGtag() {
  window.dataLayer ??= [];
  window.gtag ??= (...args: GtagArguments) => {
    window.dataLayer?.push(args);
  };
}

function loadGa4(measurementId: string) {
  if (!measurementId || document.getElementById(GA_SCRIPT_ID)) {
    return;
  }

  ensureGtag();
  window.gtag?.('consent', 'default', {
    analytics_storage: 'denied',
    ad_storage: 'denied',
    ad_user_data: 'denied',
    ad_personalization: 'denied',
  });
  window.gtag?.('consent', 'update', { analytics_storage: 'granted' });
  window.gtag?.('js', new Date());
  window.gtag?.('config', measurementId, { send_page_view: false });

  const script = document.createElement('script');
  script.id = GA_SCRIPT_ID;
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`;
  document.head.append(script);
}

export function initializeAnalyticsFromConsent(measurementId: string) {
  if (readConsent() === 'granted') {
    loadGa4(measurementId);
  }
}

export function grantAnalyticsConsent(measurementId: string) {
  window.localStorage.setItem(ANALYTICS_CONSENT_KEY, 'granted');
  loadGa4(measurementId);
}

function cookieDomains() {
  const hostname = window.location.hostname;
  const labels = hostname.split('.').filter(Boolean);
  const domains = new Set<string>([hostname]);
  for (let index = 0; index < labels.length - 1; index += 1) {
    domains.add(labels.slice(index).join('.'));
    domains.add(`.${labels.slice(index).join('.')}`);
  }
  return domains;
}

function expireAnalyticsCookies() {
  const names = document.cookie
    .split(';')
    .map((cookie) => cookie.trim().split('=')[0])
    .filter((name): name is string => Boolean(name?.startsWith('_ga')));

  for (const name of names) {
    document.cookie = `${name}=; Max-Age=0; path=/; SameSite=Lax`;
    for (const domain of cookieDomains()) {
      document.cookie = `${name}=; Max-Age=0; path=/; Domain=${domain}; SameSite=Lax`;
    }
  }
}

export function revokeAnalyticsConsent() {
  window.localStorage.setItem(ANALYTICS_CONSENT_KEY, 'denied');
  window.gtag?.('consent', 'update', {
    analytics_storage: 'denied',
    ad_storage: 'denied',
    ad_user_data: 'denied',
    ad_personalization: 'denied',
  });
  document.getElementById(GA_SCRIPT_ID)?.remove();
  expireAnalyticsCookies();
}

export function trackPageView(path: string) {
  if (readConsent() !== 'granted' || !window.gtag) {
    return false;
  }

  const pagePath = path.split(/[?#]/, 1)[0] || '/';
  window.gtag('event', 'page_view', { page_path: pagePath });
  return true;
}

export function getAnalyticsConsent() {
  return readConsent();
}
