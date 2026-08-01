package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ShoppingProductRepository;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import dev.diegobarrioh.forma.domain.StoreProductValues;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter persisting {@link ShoppingProduct}s to {@code shopping_products} (FOR-36).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). Generates the UUID id on create
 * (the domain type has no identity). Prices round-trip as {@code NUMERIC}/{@link
 * java.math.BigDecimal}; {@code last_checked_at} as an absolute instant.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V32): every read/write is scoped by the
 * real {@code user_id UUID} column added to close this "gap table"'s zero owner-scoping.
 */
@Repository
public class JdbcShoppingProductRepository implements ShoppingProductRepository {

  /**
   * The account's own columns, then the catalog's (FOR-192, V37). The join is a LEFT one because a
   * standalone product has no catalog row, and the merge is {@link ShoppingProduct#resolveWith} —
   * the rule of which side wins lives in the domain, not in this SELECT.
   */
  private static final String SELECT_COLUMNS =
      "p.id, p.name, p.url, p.package_size, p.estimated_price_eur, p.price_per_unit_eur,"
          + " p.linked_food_item_id, p.last_checked_at, p.notes, p.category, p.store_product_id,"
          + " s.name AS catalog_name, s.url AS catalog_url, s.package_size AS catalog_package_size,"
          + " s.price_eur AS catalog_price_eur, s.food_id AS catalog_food_id,"
          + " s.category AS catalog_category, s.notes AS catalog_notes";

  private static final RowMapper<StoredShoppingProduct> ROW_MAPPER =
      (rs, rowNum) -> {
        OffsetDateTime lastChecked = rs.getObject("last_checked_at", OffsetDateTime.class);
        String category = rs.getString("category");
        String storeProductId = rs.getString("store_product_id");
        ShoppingProduct stored =
            new ShoppingProduct(
                rs.getString("name"),
                rs.getString("url"),
                rs.getString("package_size"),
                rs.getBigDecimal("estimated_price_eur"),
                rs.getBigDecimal("price_per_unit_eur"),
                rs.getString("linked_food_item_id"),
                lastChecked == null ? null : lastChecked.toInstant(),
                rs.getString("notes"),
                category == null ? null : ShoppingCategory.valueOf(category),
                storeProductId);
        String catalogCategory = rs.getString("catalog_category");
        StoreProductValues catalog =
            storeProductId == null || rs.getString("catalog_name") == null
                ? null
                : new StoreProductValues(
                    rs.getString("catalog_name"),
                    rs.getString("catalog_url"),
                    rs.getString("catalog_package_size"),
                    rs.getBigDecimal("catalog_price_eur"),
                    rs.getString("catalog_food_id"),
                    catalogCategory == null ? null : ShoppingCategory.valueOf(catalogCategory),
                    rs.getString("catalog_notes"));
        return new StoredShoppingProduct(rs.getString("id"), stored.resolveWith(catalog));
      };

  private final JdbcTemplate jdbcTemplate;

  public JdbcShoppingProductRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredShoppingProduct> findAllByOwner(UUID userId) {
    // Ordered by the effective name, so a referencing entry sorts by the catalog's
    // name rather than by a null it does not have.
    return jdbcTemplate.query(
        "SELECT "
            + SELECT_COLUMNS
            + " FROM shopping_products p"
            + " LEFT JOIN store_product s ON s.id = p.store_product_id"
            + " WHERE p.user_id = ? ORDER BY COALESCE(p.name, s.name)",
        ROW_MAPPER,
        userId);
  }

  @Override
  public StoredShoppingProduct create(UUID userId, ShoppingProduct product) {
    String id = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO shopping_products (id, user_id, name, url, package_size,"
            + " estimated_price_eur, price_per_unit_eur, linked_food_item_id, last_checked_at,"
            + " notes, category, store_product_id)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.fromString(id),
        userId,
        product.name(),
        product.url(),
        product.packageSize(),
        product.estimatedPriceEur(),
        product.pricePerUnitEur(),
        product.linkedFoodItemId(),
        toOffsetDateTime(product.lastCheckedAt()),
        product.notes(),
        product.category().name(),
        product.storeProductId());
    return new StoredShoppingProduct(id, product);
  }

  @Override
  public Optional<StoredShoppingProduct> update(UUID userId, String id, ShoppingProduct product) {
    int updated =
        jdbcTemplate.update(
            "UPDATE shopping_products SET name = ?, url = ?, package_size = ?,"
                + " estimated_price_eur = ?, price_per_unit_eur = ?, linked_food_item_id = ?,"
                + " last_checked_at = ?, notes = ?, category = ? WHERE id = ? AND user_id = ?",
            product.name(),
            product.url(),
            product.packageSize(),
            product.estimatedPriceEur(),
            product.pricePerUnitEur(),
            product.linkedFoodItemId(),
            toOffsetDateTime(product.lastCheckedAt()),
            product.notes(),
            product.category().name(),
            UUID.fromString(id),
            userId);
    // Re-read rather than echo the argument back: the caller's product carries no
    // store_product_id (the request body has no such field), so echoing it would
    // report a catalog reference as a standalone product for one round trip.
    return updated == 0 ? Optional.empty() : findByOwnerAndId(userId, id);
  }

  /**
   * One INSERT per missing id, each guarded by a NOT EXISTS on this account's rows.
   *
   * <p>The guard is what makes a repeated regenerate cheap and safe: an account that already has an
   * entry for a product keeps it, with whatever price it overrode. The unique constraint added in
   * V37 backs the same rule at the database level, so a concurrent regenerate cannot slip a second
   * entry past the check.
   *
   * <p>Every column but the id, the owner and the reference is left null on purpose — null is what
   * "read this from the catalog" looks like. `category` is the exception: it is NOT NULL since V7,
   * so it takes its DEFAULT of OTROS, which {@link
   * dev.diegobarrioh.forma.domain.ShoppingProduct#resolveWith} treats as "not set" and fills from
   * the catalog's aisle.
   */
  @Override
  public int addMissingCatalogReferences(UUID userId, List<String> storeProductIds) {
    int created = 0;
    for (String storeProductId : storeProductIds) {
      created +=
          jdbcTemplate.update(
              "INSERT INTO shopping_products (id, user_id, store_product_id, category)"
                  + " SELECT ?, ?, ?, 'OTROS' FROM store_product s WHERE s.id = ?"
                  + " AND NOT EXISTS (SELECT 1 FROM shopping_products p"
                  + " WHERE p.user_id = ? AND p.store_product_id = ?)",
              UUID.randomUUID(),
              userId,
              storeProductId,
              storeProductId,
              userId,
              storeProductId);
    }
    return created;
  }

  private Optional<StoredShoppingProduct> findByOwnerAndId(UUID userId, String id) {
    return jdbcTemplate
        .query(
            "SELECT "
                + SELECT_COLUMNS
                + " FROM shopping_products p"
                + " LEFT JOIN store_product s ON s.id = p.store_product_id"
                + " WHERE p.id = ? AND p.user_id = ?",
            ROW_MAPPER,
            UUID.fromString(id),
            userId)
        .stream()
        .findFirst();
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
