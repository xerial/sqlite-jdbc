// --------------------------------------
// sqlite-jdbc Project
//
// SQLiteDataSourceTest.java
// Since: Mar 11, 2010
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SQLiteDataSourceTest {

    @BeforeEach
    public void setUp() {}

    @AfterEach
    public void tearDown() {}

    @Test
    public void enumParam() throws Exception {

        SQLiteDataSource ds = new SQLiteDataSource();
        try (Connection conn = ds.getConnection();
                Statement stat = conn.createStatement()) {

            stat.executeUpdate("create table A (id integer, name)");
            stat.executeUpdate("insert into A values(1, 'leo')");
            ResultSet rs = stat.executeQuery("select * from A");
            int count = 0;
            while (rs.next()) {
                count++;
                int id = rs.getInt(1);
                String name = rs.getString(2);
                assertThat(id).isEqualTo(1);
                assertThat(name).isEqualTo("leo");
            }
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    public void encoding() throws Exception {

        String[] configArray =
                new String[] {
                    "UTF8", "UTF-8", "UTF_8",
                    "UTF16", "UTF-16", "UTF_16",
                    "UTF_16LE", "UTF-16LE", "UTF16_LITTLE_ENDIAN",
                    "UTF_16BE", "UTF-16BE", "UTF16_BIG_ENDIAN"
                };

        String nativeOrder;
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            nativeOrder = "le";
        } else {
            nativeOrder = "be";
        }
        String[] encodingArray =
                new String[] {"UTF-8", "UTF-16" + nativeOrder, "UTF-16le", "UTF-16be"};

        for (int i = 0; i < configArray.length; i++) {
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setEncoding(configArray[i]);

            try (Connection conn = ds.getConnection();
                    Statement stat = conn.createStatement()) {
                ResultSet rs = stat.executeQuery("pragma encoding");
                assertThat(rs.getString(1)).isEqualTo(encodingArray[i / 3]);
            }
        }
    }

    @Test
    public void setBusyTimeout() {
        final SQLiteDataSource ds = new SQLiteDataSource();
        ds.setBusyTimeout(1234);
        assertThat(
                        ds.getConfig()
                                .toProperties()
                                .getProperty(SQLiteConfig.Pragma.BUSY_TIMEOUT.pragmaName))
                .isEqualTo("1234");
        assertThat(ds.getConfig().getBusyTimeout()).isEqualTo(1234);
    }

    @Test
    public void setGetGeneratedKeys() throws SQLException {
        final SQLiteDataSource ds = new SQLiteDataSource();
        ds.setGetGeneratedKeys(false);
        assertThat(
                        ds.getConfig()
                                .toProperties()
                                .getProperty(
                                        SQLiteConfig.Pragma.JDBC_GET_GENERATED_KEYS.pragmaName))
                .isEqualTo("false");
        assertThat(ds.getConfig().isGetGeneratedKeys()).isEqualTo(false);
        assertThat(
                        ((SQLiteConnection) ds.getConnection())
                                .getConnectionConfig()
                                .isGetGeneratedKeys())
                .isFalse();

        ds.setGetGeneratedKeys(true);
        assertThat(
                        ds.getConfig()
                                .toProperties()
                                .getProperty(
                                        SQLiteConfig.Pragma.JDBC_GET_GENERATED_KEYS.pragmaName))
                .isEqualTo("true");
        assertThat(ds.getConfig().isGetGeneratedKeys()).isEqualTo(true);
        assertThat(
                        ((SQLiteConnection) ds.getConnection())
                                .getConnectionConfig()
                                .isGetGeneratedKeys())
                .isTrue();
    }
}
