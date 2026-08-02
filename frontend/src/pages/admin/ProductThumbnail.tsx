import { thumbnailUrl } from './thumbnail';
import styles from './ProductThumbnail.module.css';

/**
 * The shop's photo of a product, at list size (FOR-195).
 *
 * <p>Decorative: every row states its own name beside it, so an alt text would
 * only repeat what a screen reader just read. Renders nothing at all when the
 * product has no photo — a placeholder box would add a column of grey squares to
 * say "we do not know what this looks like".
 */
interface ProductThumbnailProps {
  readonly url?: string;
  /** Rendered size in px; also what the shop's CDN is asked to crop to. */
  readonly size?: number;
}

export function ProductThumbnail({ url, size = 24 }: ProductThumbnailProps) {
  const src = thumbnailUrl(url, size);
  if (!src) {
    return null;
  }
  return (
    <img
      className={styles.thumbnail}
      src={src}
      alt=""
      width={size}
      height={size}
      loading="lazy"
      decoding="async"
    />
  );
}
