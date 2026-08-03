package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * V46: each shop's own aisles, as a tree, alongside our six.
 *
 * <p>The table starts empty on purpose — a shop's aisles are the shop's to state — so what there is
 * to check is the shape it will be filled into: that a tree can be built, that the two vocabularies
 * stay independent, and that the constraints refuse the states the writer must not produce.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class StoreCategoryMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:store_category_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static Connection connection;

  @BeforeAll
  static void migrate() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();
    connection = DriverManager.getConnection(JDBC_URL, "sa", "");
  }

  @AfterAll
  static void closeConnection() throws Exception {
    connection.close();
  }

  @AfterEach
  void clear() throws Exception {
    execute("UPDATE store_product SET store_category_id = NULL");
    execute("DELETE FROM store_product WHERE id LIKE 'test-%'");
    execute("DELETE FROM store_category");
  }

  /** Nothing is invented: Mercadona's aisles arrive when Mercadona is asked. */
  @Test
  void startsEmpty() throws Exception {
    assertThat(column("SELECT id FROM store_category")).isEmpty();
  }

  @Test
  void holdsAThreeLevelTree() throws Exception {
    seedTree();

    assertThat(
            column(
                "SELECT c.name FROM store_category c"
                    + " JOIN store_category p ON c.parent_id = p.id"
                    + " WHERE p.external_id = '112' ORDER BY c.sort_order"))
        .containsExactly("Avena y cereales");
    assertThat(column("SELECT name FROM store_category WHERE level = 0"))
        .containsExactly("Cereales y galletas");
  }

  /** A root has no parent and nothing else does; the column and the level cannot disagree. */
  @Test
  void refusesARootWithAParentOrAChildWithout() {
    assertThatThrownBy(() -> insertCategory("x", null, "9", "Suelta", 1))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(
            () -> {
              insertCategory("r", null, "8", "Raíz", 0);
              insertCategory("y", "r", "7", "Hija", 0);
            })
        .isInstanceOf(SQLException.class);
  }

  /** The shop's own id is the identity, so a second crawl updates rather than duplicating. */
  @Test
  void refusesTwoAislesWithTheSameIdInOneShop() throws Exception {
    insertCategory("a", null, "112", "Cereales y galletas", 0);

    assertThatThrownBy(() -> insertCategory("b", null, "112", "Otro nombre", 0))
        .isInstanceOf(SQLException.class);
  }

  /** Two shops number their aisles independently; the same id in each is two different aisles. */
  @Test
  void letsTwoShopsUseTheSameAisleId() throws Exception {
    insertCategory("m-112", null, "112", "Cereales", 0);

    assertThatCode(
            () ->
                execute(
                    "INSERT INTO store_category (id, store_id, external_id, name, slug, level)"
                        + " VALUES ('c-112', 'CARREFOUR', '112', 'Desayunos', 'desayunos', 0)"))
        .doesNotThrowAnyException();
  }

  /** Names repeat across the tree — every shop has three shelves called "Otros". */
  @Test
  void allowsTheSameNameUnderDifferentParents() throws Exception {
    seedTree();

    assertThatCode(
            () -> {
              insertCategory("o1", "root", "900", "Otros", 1);
              insertCategory("o2", "sub", "901", "Otros", 2);
            })
        .doesNotThrowAnyException();
  }

  /** An aisle with products in it cannot vanish under them. */
  @Test
  void refusesToDeleteAnAisleAProductSitsIn() throws Exception {
    seedTree();
    execute(
        "INSERT INTO store_product (id, store, name, category, store_category_id)"
            + " VALUES ('test-p', 'MERCADONA', 'Avena', 'OTROS', 'sub')");

    assertThatThrownBy(() -> execute("DELETE FROM store_category WHERE id = 'sub'"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * The two vocabularies are independent. A product carries one of our six aisles whether or not
   * the shop's tree knows where it sits — every product that predates V46 is in exactly that state.
   */
  @Test
  void leavesOurOwnAisleUntouchedAndIndependent() throws Exception {
    execute(
        "INSERT INTO store_product (id, store, name, category)"
            + " VALUES ('test-q', 'MERCADONA', 'Avena', 'CEREALES_Y_LEGUMBRES')");

    assertThat(column("SELECT category FROM store_product WHERE id = 'test-q'"))
        .containsExactly("CEREALES_Y_LEGUMBRES");
    assertThat(column("SELECT store_category_id FROM store_product WHERE id = 'test-q'"))
        .containsOnlyNulls();
  }

  private static void seedTree() throws SQLException {
    insertCategory("root", null, "112", "Cereales y galletas", 0);
    insertCategory("sub", "root", "113", "Avena y cereales", 1);
  }

  private static void insertCategory(
      String id, String parentId, String externalId, String name, int level) throws SQLException {
    String parent = parentId == null ? "NULL" : "'" + parentId + "'";
    execute(
        "INSERT INTO store_category (id, store_id, parent_id, external_id, name, slug, level)"
            + " VALUES ('"
            + id
            + "', 'MERCADONA', "
            + parent
            + ", '"
            + externalId
            + "', '"
            + name
            + "', 'slug-"
            + externalId
            + "', "
            + level
            + ")");
  }

  private static List<String> column(String sql) throws SQLException {
    List<String> values = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        values.add(rs.getString(1));
      }
    }
    return values;
  }

  private static void execute(String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
