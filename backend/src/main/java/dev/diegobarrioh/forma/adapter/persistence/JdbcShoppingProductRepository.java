package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ShoppingProductRepository;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
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
 */
@Repository
public class JdbcShoppingProductRepository implements ShoppingProductRepository {

  private static final RowMapper<StoredShoppingProduct> ROW_MAPPER =
      (rs, rowNum) -> {
        OffsetDateTime lastChecked = rs.getObject("last_checked_at", OffsetDateTime.class);
        String category = rs.getString("category");
        ShoppingProduct product =
            new ShoppingProduct(
                rs.getString("name"),
                rs.getString("url"),
                rs.getString("package_size"),
                rs.getBigDecimal("estimated_price_eur"),
                rs.getBigDecimal("price_per_unit_eur"),
                rs.getString("linked_food_item_id"),
                lastChecked == null ? null : lastChecked.toInstant(),
                rs.getString("notes"),
                category == null ? null : ShoppingCategory.valueOf(category));
        return new StoredShoppingProduct(rs.getString("id"), product);
      };

  private final JdbcTemplate jdbcTemplate;

  public JdbcShoppingProductRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredShoppingProduct> findAll() {
    return jdbcTemplate.query(
        "SELECT id, name, url, package_size, estimated_price_eur, price_per_unit_eur,"
            + " linked_food_item_id, last_checked_at, notes, category FROM shopping_products"
            + " ORDER BY name",
        ROW_MAPPER);
  }

  @Override
  public StoredShoppingProduct create(ShoppingProduct product) {
    String id = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO shopping_products (id, name, url, package_size, estimated_price_eur,"
            + " price_per_unit_eur, linked_food_item_id, last_checked_at, notes, category)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.fromString(id),
        product.name(),
        product.url(),
        product.packageSize(),
        product.estimatedPriceEur(),
        product.pricePerUnitEur(),
        product.linkedFoodItemId(),
        toOffsetDateTime(product.lastCheckedAt()),
        product.notes(),
        product.category().name());
    return new StoredShoppingProduct(id, product);
  }

  @Override
  public Optional<StoredShoppingProduct> update(String id, ShoppingProduct product) {
    int updated =
        jdbcTemplate.update(
            "UPDATE shopping_products SET name = ?, url = ?, package_size = ?,"
                + " estimated_price_eur = ?, price_per_unit_eur = ?, linked_food_item_id = ?,"
                + " last_checked_at = ?, notes = ?, category = ? WHERE id = ?",
            product.name(),
            product.url(),
            product.packageSize(),
            product.estimatedPriceEur(),
            product.pricePerUnitEur(),
            product.linkedFoodItemId(),
            toOffsetDateTime(product.lastCheckedAt()),
            product.notes(),
            product.category().name(),
            UUID.fromString(id));
    return updated == 0 ? Optional.empty() : Optional.of(new StoredShoppingProduct(id, product));
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
