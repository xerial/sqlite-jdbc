package org.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests are designed to stress Statements on memory databases.
 */
public class DBMetaDataVersionTest {
    private Connection conn;
    private DatabaseMetaData meta;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        meta = conn.getMetaData();
    }

    @AfterEach
    public void close() throws SQLException {
        meta = null;
        conn.close();
    }

    @Test
    public void version() throws Exception {
        Properties version;
        try (InputStream resourceAsStream = DBMetaDataVersionTest.class.getResourceAsStream("/META-INF/maven/org.xerial/sqlite-jdbc/VERSION")) {
            version = new Properties();
            Assumptions.assumeTrue(resourceAsStream != null);
            version.load(resourceAsStream);
        }
        String versionString = version.getProperty("version");
        int majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
        int minorVersion = Integer.parseInt(versionString.split("\\.")[1]);

        assertTrue(majorVersion > 0, "major version check");
        assertEquals("SQLite JDBC", meta.getDriverName(), "driver name");
        assertTrue(meta.getDriverVersion().startsWith(String.format("%d.%d", majorVersion, minorVersion)), "driver version");
        assertEquals(majorVersion, meta.getDriverMajorVersion(), "driver major version");
        assertEquals(minorVersion, meta.getDriverMinorVersion(), "driver minor version");
        assertEquals("SQLite", meta.getDatabaseProductName(), "db name");
        assertEquals(versionString, meta.getDatabaseProductVersion(), "db version");
        assertEquals(majorVersion, meta.getDatabaseMajorVersion(), "db major version");
        assertEquals(minorVersion, meta.getDatabaseMinorVersion(), "db minor version");
        assertNull(meta.getUserName(), "user name");
    }

}
