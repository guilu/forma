import styles from './Brand.module.css';

/**
 * FORMA brand lockup: the "F" mark plus the wordmark, matching docs/mockup.png.
 * The mark renders the shared brand asset (`public/logo.svg`, a copy of
 * `docs/logo.svg`) as an `<img>` rather than inline SVG: the source file bakes
 * in fixed gradient ids (`_Radial1/2/3`), and multiple `<Brand>` instances can
 * render on one page (Topbar + Sidebar), so inlining it would collide those
 * ids across instances. The image is decorative (empty `alt`, `aria-hidden`)
 * — the wordmark span remains the component's sole accessible name.
 */
export function Brand({ showWordmark = true }: { readonly showWordmark?: boolean }) {
  return (
    <span className={styles.brand}>
      <img className={styles.mark} src="/logo.svg" alt="" aria-hidden="true" />
      {showWordmark && <span className={styles.wordmark}>FORMA</span>}
    </span>
  );
}
