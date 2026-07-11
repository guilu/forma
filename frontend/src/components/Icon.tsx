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
  | 'heart';

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
