// --------------------------------------
// sqlite-jdbc Project
//
// ReadCommitedTest.java
// Since: Jan 19, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReadUncommittedTest {
    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("shared_cache", "true");
        conn = DriverManager.getConnection("jdbc:sqlite:", prop);
        stat = conn.createStatement();
        stat.executeUpdate("create table test (id integer primary key, fn, sn);");
        stat.executeUpdate("create view testView as select * from test;");
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void setReadUncommitted() throws SQLException {
        conn.setTransactionIsolation(SQLiteConnection.TRANSACTION_READ_UNCOMMITTED);
    }

    @Test
    public void setSerializable() throws SQLException {
        conn.setTransactionIsolation(SQLiteConnection.TRANSACTION_SERIALIZABLE);
    }

    @Test
    public void setIsolationPromotedToSerializable() throws SQLException {
        conn.setTransactionIsolation(SQLiteConnection.TRANSACTION_REPEATABLE_READ);
    }

    @Test
    public void setReadUncommittedWithConfig() throws SQLException {
        // Override original setup
        Properties prop = new Properties();
        prop.setProperty("shared_cache", "true");
        conn = DriverManager.getConnection("jdbc:sqlite:", prop);
        stat = conn.createStatement();
        assertThat(stat.executeQuery("PRAGMA read_uncommitted;").getString(1))
                .as("Fail to set pragma read_uncommitted")
                .isEqualTo("0");

        prop.setProperty("read_uncommitted", "true");
        conn = DriverManager.getConnection("jdbc:sqlite:", prop);
        stat = conn.createStatement();
        assertThat(stat.executeQuery("PRAGMA read_uncommitted;").getString(1))
                .as("Fail to set pragma read_uncommitted")
                .isEqualTo("1");

        prop.setProperty("read_uncommitted", "false");
        conn = DriverManager.getConnection("jdbc:sqlite:", prop);
        stat = conn.createStatement();
        assertThat(stat.executeQuery("PRAGMA read_uncommitted;").getString(1))
                .as("Fail to set pragma read_uncommitted")
                .isEqualTo("0");
    }
}
