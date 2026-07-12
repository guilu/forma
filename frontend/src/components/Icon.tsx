/**
 * Minimal inline-SVG icon set for the shell (FOR-81). Stroke-based line icons
 * keep the "technical dashboard" tone from docs/ui-guidelines.md without pulling
 * an icon dependency into the skeleton. Icons inherit `currentColor`.
 */

export type IconName =
  | 'dashboard'
  | 'measurements'
  | 'training'
  | 'nutrition'
  | 'shopping'
  | 'progress'
  | 'goals'
  | 'settings'
  | 'bell'
  | 'menu'
  | 'more'
  | 'user'
  | 'heart'
  | 'edit'
  | 'activity'
  | 'cross'
  | 'chevron'
  | 'inbox'
  | 'alertTriangle'
  | 'lock'
  | 'sun'
  | 'moon';

const PATHS: Record<IconName, string> = {
  dashboard: 'M3 3h7v7H3zM14 3h7v4h-7zM14 10h7v11h-7zM3 13h7v8H3z',
  measurements: 'M3 12h4l3 7 4-14 3 7h4',
  training: 'M6 6v12M18 6v12M4 9h2v6H4zM18 9h2v6h-2zM6 12h12',
  nutrition: 'M12 3c4 0 6 3 6 7a6 6 0 0 1-12 0c0-4 2-7 6-7zM12 3v6',
  shopping:
    'M4 4h2l2 12h10l2-8H7M9 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2zM17 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2z',
  progress: 'M4 20V4M4 20h16M8 16l4-5 3 3 5-7',
  goals: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM12 3v3M12 18v3',
  settings:
    'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19 12a7 7 0 0 0-.1-1l2-1.6-2-3.4-2.4 1a7 7 0 0 0-1.7-1l-.3-2.5H10l-.3 2.5a7 7 0 0 0-1.7 1l-2.4-1-2 3.4 2 1.6a7 7 0 0 0 0 2l-2 1.6 2 3.4 2.4-1a7 7 0 0 0 1.7 1l.3 2.5h3.4l.3-2.5a7 7 0 0 0 1.7-1l2.4 1 2-3.4-2-1.6a7 7 0 0 0 .1-1z',
  bell: 'M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0',
  menu: 'M4 6h16M4 12h16M4 18h16',
  more: 'M5 12h.01M12 12h.01M19 12h.01',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM5 20a7 7 0 0 1 14 0',
  heart: 'M12 20s-7-4.35-9.5-8A5 5 0 0 1 12 6a5 5 0 0 1 9.5 6c-2.5 3.65-9.5 8-9.5 8z',
  edit: 'M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z',
  // Generic activity/heartbeat line — used for fitness-tracking providers
  // (e.g. Google Fit, FOR-57). Not a brand logo.
  activity: 'M3 12h4l2 5 4-14 2 9 2-5h4',
  // Generic medical cross — used for health-data providers (e.g. Apple
  // Health, FOR-57). Not a brand logo.
  cross: 'M12 4v16M4 12h16',
  // Right-pointing chevron — decorative affordance for navigable/entry-point
  // rows (e.g. Ajustes, FOR-58).
  chevron: 'M9 6l6 6-6 6',
  // Empty tray — shared empty-state component (FOR-60).
  inbox: 'M3 12l2-7h14l2 7M3 12v6a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-6M3 12h5l1 2h6l1-2h5',
  // Warning triangle — shared recoverable-error-state component (FOR-60).
  alertTriangle: 'M12 4l9 16H3zM12 10v4M12 16.5v.01',
  // Padlock — shared permission/access-error-state component (FOR-60).
  lock: 'M6 11V8a6 6 0 0 1 12 0v3M5 11h14v9H5zM12 15v2',
  // Sun — switch to / indicate light theme (topbar theme toggle, FOR-62).
  sun: 'M12 4V2M12 22v-2M4 12H2M22 12h-2M5.6 5.6L4.2 4.2M19.8 19.8l-1.4-1.4M18.4 5.6l1.4-1.4M4.2 19.8l1.4-1.4M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
  // Crescent moon — switch to / indicate dark theme (topbar theme toggle, FOR-62).
  moon: 'M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z',
};

interface IconProps {
  readonly name: IconName;
  readonly size?: number;
  readonly className?: string;
}

export function Icon({ name, size = 20, className }: IconProps) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
