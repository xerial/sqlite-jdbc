package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

public class ExtensionTest {
    Connection conn;
    Statement stat;

    @BeforeEach
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (stat != null) {
            stat.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void loadExtensionEnabled() throws Exception {
        SQLiteConnection connection =
                (SQLiteConnection)
                        DriverManager.getConnection(
                                "jdbc:sqlite::memory:?enable_load_extension=true");
        assertThat(connection.getDatabase().getConfig().isEnabledLoadExtension()).isTrue();
    }

    @Test
    public void extFTS3() throws Exception {
        stat.execute("create virtual table recipe using fts3(name, ingredients)");
        stat.execute(
                "insert into recipe (name, ingredients) values('broccoli stew', 'broccoli peppers cheese tomatoes')");
        stat.execute(
                "insert into recipe (name, ingredients) values('pumpkin stew', 'pumpkin onions garlic celery')");

        ResultSet rs =
                stat.executeQuery(
                        "select rowid, name, ingredients from recipe where ingredients match 'onions'");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(2)).isEqualTo("pumpkin stew");
    }

    @Test
    public void extFTS5() throws Exception {
        stat.execute("create virtual table recipe using fts5(name, ingredients)");
        stat.execute(
                "insert into recipe (name, ingredients) values('broccoli stew', 'broccoli peppers cheese tomatoes')");
        stat.execute(
                "insert into recipe (name, ingredients) values('pumpkin stew', 'pumpkin onions garlic celery')");

        ResultSet rs =
                stat.executeQuery(
                        "select rowid, name, ingredients from recipe where recipe match 'onions'");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(2)).isEqualTo("pumpkin stew");
    }

    @Test
    @DisabledInNativeImage // assertj Assumptions do not work in native-image tests
    public void extFunctions() throws Exception {
        Utils.assumeJdbcExtensions(conn);

        {
            ResultSet rs = stat.executeQuery("select reverse(\"ACGT\")");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("TGCA");
            rs.close();
        }
    }

    @Test
    @DisabledInNativeImage // assertj Assumptions do not work in native-image tests
    public void extFunctionsTruncatedUtf8() throws Exception {
        Utils.assumeJdbcExtensions(conn);

        // A text value that ends in a truncated multibyte sequence: the lead byte
        // announces more trailing bytes than the value actually holds. The utf-8
        // reader behind these functions used to read those missing bytes past the
        // terminating nul, a heap over-read. Each lead byte below claims 1, 2 and 3
        // trailing bytes respectively, so all three trailing-byte cases are covered.
        String[] truncated = {"x'c3'", "x'e0'", "x'f0'", "x'f4'"};
        for (String value : truncated) {
            String text = "cast(" + value + " as text)";
            // every call site of the reader
            assertReturnsRow("select reverse(" + text + ")");
            assertReturnsRow("select proper(" + text + ")");
            assertReturnsRow("select leftstr(" + text + ", 1)");
            assertReturnsRow("select rightstr(" + text + ", 1)");
            assertReturnsRow("select charindex('a', " + text + ")");
            assertReturnsRow("select strfilter(" + text + ", 'a')");
            assertReturnsRow("select difference(" + text + ", 'abc')");
        }

        // well-formed multibyte input still decodes as before
        ResultSet rs = stat.executeQuery("select reverse('a£€')");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo("€£a");
        rs.close();
    }

    private void assertReturnsRow(String sql) throws Exception {
        try (ResultSet rs = stat.executeQuery(sql)) {
            assertThat(rs.next()).isTrue();
            rs.getString(1);
        }
    }

    @Test
    @DisabledInNativeImage // assertj Assumptions do not work in native-image tests
    public void dbstat() throws Exception {
        assumeThat(Utils.getCompileOptions(conn))
                .as("SQLite has to be compiled with ENABLE_DBSTAT_VTAB")
                .contains("ENABLE_DBSTAT_VTAB");

        {
            boolean result = stat.execute("SELECT * FROM dbstat");
            assertThat(result).isTrue();
        }
    }
}
