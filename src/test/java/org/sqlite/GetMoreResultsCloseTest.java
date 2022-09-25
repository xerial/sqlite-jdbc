package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GetMoreResultsCloseTest {

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @Test
    public void getMoreResultsDoesNotCloseStatement() throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement pStmt = conn.prepareStatement("SELECT ?")) {
                pStmt.setString(1, "Hello World!");
                try (ResultSet rSet = pStmt.executeQuery()) {
                    int rows = 0;
                    while (rSet.next()) {
                        Assertions.assertEquals("Hello World!", rSet.getString(1));
                        rows++;
                    }
                    Assertions.assertEquals(1, rows);
                }
                Assertions.assertFalse(pStmt.getMoreResults());
                // This must pass
                pStmt.clearParameters();
            }
        }
    }

}
