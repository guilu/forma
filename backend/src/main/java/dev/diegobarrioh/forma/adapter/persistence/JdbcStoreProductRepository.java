package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.CatalogStoreProduct;
import dev.diegobarrioh.forma.application.StoreProductRepository;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over the {@code store_product} table (FOR-191, V36). Plain JDBC via {@link
 * JdbcTemplate} (no ORM, like FOR-16). Single-table, no joins — {@code food_id} is returned raw and
 * resolved by whoever needs the food, so listing the catalog costs one query.
 */
@Repository
public class JdbcStoreProductRepository implements StoreProductRepository {

  private static final String COLUMNS =
      "id, store, name, food_id, package_size, price_eur, url, category, notes, external_id,"
          + " image_url, store_category_id";

  private static final RowMapper<CatalogStoreProduct> ROW_MAPPER =
      (rs, rowNum) ->
          new CatalogStoreProduct(
              rs.getString("id"),
              rs.getString("store"),
              rs.getString("name"),
              rs.getString("food_id"),
              rs.getString("package_size"),
              rs.getBigDecimal("price_eur"),
              rs.getString("url"),
              ShoppingCategory.valueOf(rs.getString("category")),
              rs.getString("notes"),
              rs.getString("external_id"),
              rs.getString("image_url"),
              rs.getString("store_category_id"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcStoreProductRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<CatalogStoreProduct> findAll(String store) {
    // Ordered by name rather than id: the id is a slug nobody scans by, and the
    // screen lists one store at a time.
    if (store == null) {
      return jdbcTemplate.query(
          "SELECT " + COLUMNS + " FROM store_product ORDER BY store, name", ROW_MAPPER);
    }
    return jdbcTemplate.query(
        "SELECT " + COLUMNS + " FROM store_product WHERE store = ? ORDER BY name",
        ROW_MAPPER,
        store);
  }

  @Override
  public Optional<CatalogStoreProduct> findById(String id) {
    List<CatalogStoreProduct> rows =
        jdbcTemplate.query(
            "SELECT " + COLUMNS + " FROM store_product WHERE id = ?", ROW_MAPPER, id);
    return rows.stream().findFirst();
  }

  @Override
  public void insert(CatalogStoreProduct product) {
    jdbcTemplate.update(
        "INSERT INTO store_product (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        product.id(),
        product.store(),
        product.name(),
        product.foodId(),
        product.packageSize(),
        product.priceEur(),
        product.url(),
        product.category().name(),
        product.notes(),
        product.externalId(),
        product.imageUrl(),
        product.storeCategoryId());
  }

  @Override
  public void update(CatalogStoreProduct product) {
    jdbcTemplate.update(
        "UPDATE store_product SET store = ?, name = ?, food_id = ?, package_size = ?,"
            + " price_eur = ?, url = ?, category = ?, notes = ?, external_id = ?, image_url = ?,"
            + " store_category_id = ?"
            + " WHERE id = ?",
        product.store(),
        product.name(),
        product.foodId(),
        product.packageSize(),
        product.priceEur(),
        product.url(),
        product.category().name(),
        product.notes(),
        product.externalId(),
        product.imageUrl(),
        product.storeCategoryId(),
        product.id());
  }

  @Override
  public boolean delete(String id) {
    return jdbcTemplate.update("DELETE FROM store_product WHERE id = ?", id) > 0;
  }
}
