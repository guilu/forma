/**
 * The compact date the page headers show (FOR-193).
 *
 * <p>"2 ago 2026", not "domingo, 2 de agosto". The long form is what pushed the training header's
 * date onto its own row on a phone, taking everything below it down with it; the compact one fits
 * beside the title at 390 px, which is where the dashboard already puts its own.
 *
 * <p>Capitalised in JavaScript rather than with `text-transform: capitalize`, which capitalises
 * EVERY word and turned "domingo, 2 de agosto" into "Domingo, 2 De Agosto".
 */
const SHORT_DATE = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

export function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatShortDate(date: Date): string {
  return capitalize(SHORT_DATE.format(date));
}
