import styles from './Brand.module.css';

/**
 * FORMA brand lockup: the "F" mark plus the wordmark, matching docs/mockup.png.
 * The mark is inline SVG so it inherits the accent token and needs no asset.
 */
export function Brand({ showWordmark = true }: { readonly showWordmark?: boolean }) {
  return (
    <span className={styles.brand}>
      <svg
        className={styles.mark}
        width="28"
        height="28"
        viewBox="0 0 32 32"
        aria-hidden="true"
        focusable="false"
      >
        <path d="M8 4h18l-3 6H14v5h9l-3 6h-6v7l-6 3z" fill="currentColor" />
      </svg>
      {showWordmark && <span className={styles.wordmark}>FORMA</span>}
    </span>
  );
}
