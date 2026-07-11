import { Badge, type BadgeTone } from './Badge';

export type StatusKind = 'severity' | 'connection' | 'plazo';

interface StatusMapping {
  readonly tone: BadgeTone;
  readonly label: string;
}

/**
 * Recommendation severity (backend `RecommendationSeverity`, FOR-41): INFO is a
 * neutral observation, WARNING deserves attention without alarm, ACTION is a
 * concrete suggested adjustment. ui-guidelines.md says the accent should
 * "highlight actions" — so ACTION maps to accent rather than danger; nothing
 * here should read as alarming (docs/ui-guidelines.md interaction style).
 */
const SEVERITY_TONES: Record<string, StatusMapping> = {
  INFO: { tone: 'neutral', label: 'Info' },
  WARNING: { tone: 'warning', label: 'Atención' },
  ACTION: { tone: 'accent', label: 'Acción' },
};

/** Integration connection status (e.g. Settings/integrations screens). */
const CONNECTION_TONES: Record<string, StatusMapping> = {
  Conectado: { tone: 'accent', label: 'Conectado' },
  'No conectado': { tone: 'neutral', label: 'No conectado' },
};

/** Plazo (term) tags — purely categorical, no severity implied. */
const PLAZO_TONES: Record<string, StatusMapping> = {
  'Corto plazo': { tone: 'neutral', label: 'Corto plazo' },
  'Medio plazo': { tone: 'neutral', label: 'Medio plazo' },
  'Largo plazo': { tone: 'neutral', label: 'Largo plazo' },
};

const TABLES: Record<StatusKind, Record<string, StatusMapping>> = {
  severity: SEVERITY_TONES,
  connection: CONNECTION_TONES,
  plazo: PLAZO_TONES,
};

interface StatusPillProps {
  readonly kind: StatusKind;
  readonly value: string;
}

/**
 * Domain-aware {@link Badge} wrapper (FOR-50) for the three tag families every
 * screen needs: severity, connection and plazo. This is presentation-only
 * mapping (label + tone), not a business rule — the domain values themselves
 * come from the backend (ADR-006: frontend consumes read models). An
 * unrecognized value always falls back to a neutral badge showing the raw
 * value, so a status can never render broken/unstyled.
 */
export function StatusPill({ kind, value }: StatusPillProps) {
  const resolved = TABLES[kind][value] ?? { tone: 'neutral', label: value };
  return <Badge tone={resolved.tone}>{resolved.label}</Badge>;
}
