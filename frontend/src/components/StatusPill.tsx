import { Badge } from './Badge';
import { TABLES, type StatusKind } from './statusLabels';

export type { StatusKind } from './statusLabels';

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
