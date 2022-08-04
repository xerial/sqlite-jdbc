package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

/** A test to make sure that getMetadata is properly freed */
public class MetadataLeakTest {
    /**
     * Check that statement is properly closed when result set is closed. See
     * https://github.com/xerial/sqlite-jdbc/issues/423
     *
     * @throws Exception on failure
     */
    @Test
    void testMemoryIsFreed() throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DatabaseMetaData meta = con.getMetaData();

            Statement statement;
            try (ResultSet tables = meta.getTables(null, null, null, null); ) {
                statement = tables.getStatement();
                assertFalse(statement.isClosed());
            }
            assertTrue(statement.isClosed());
        }
    }
}
