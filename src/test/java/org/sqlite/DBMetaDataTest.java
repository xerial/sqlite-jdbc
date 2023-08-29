package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** These tests are designed to stress Statements on memory databases. */
public class DBMetaDataTest {
    private Connection conn;
    private Statement stat;
    private DatabaseMetaData meta;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate(
                "create table test (id integer primary key, fn float default 0.0, sn not null, intvalue integer(5), realvalue real(8,3), charvalue varchar(21));");
        stat.executeUpdate("create view testView as select * from test;");
        meta = conn.getMetaData();
    }

    @AfterEach
    public void close() throws SQLException {
        meta = null;
        stat.close();
        conn.close();
    }

    @Test
    public void getTables() throws SQLException {
        ResultSet rs = meta.getTables(null, null, null, null);
        assertThat(rs).isNotNull();

        stat.close();

        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("sqlite_schema");
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("SYSTEM TABLE");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test"); // 3
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("TABLE"); // 4
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("VIEW");
        assertThat(rs.next()).isFalse();
        rs.close();

        rs = meta.getTables(null, null, "bob", null);
        assertThat(rs.next()).isFalse();
        rs.close();
        rs = meta.getTables(null, null, "test", null);
        assertThat(rs.next()).isTrue();
        assertThat(rs.next()).isFalse();
        rs.close();
        rs = meta.getTables(null, null, "test%", null);
        assertThat(rs.next()).isTrue();
        assertThat(rs.next()).isTrue();
        rs.close();

        rs = meta.getTables(null, null, null, new String[] {"table"});
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.next()).isFalse();
        rs.close();

        rs = meta.getTables(null, null, null, new String[] {"view"});
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.next()).isFalse();
        rs.close();

        rs = meta.getTables(null, null, null, new String[] {"system table"});
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("sqlite_schema");
        assertThat(rs.next()).isFalse();
        rs.close();
    }

    @Test
    public void getTablesWithEscape() throws SQLException {
        stat.executeUpdate("create table 'table%with%wildcards'(c1 integer)");
        stat.executeUpdate("create table 'table_with_wildcards'(c2 integer)");
        stat.executeUpdate("create table 'tableXwithXwildcards'(c3 integer)");

        String esc = meta.getSearchStringEscape();
        try (ResultSet rs =
                meta.getTables(null, null, "table_with_wildcards".replace("_", esc + "_"), null)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("TABLE_NAME")).isEqualTo("table_with_wildcards");
            assertThat(rs.next()).isFalse();
        }
        try (ResultSet rs =
                meta.getTables(null, null, "table%with%wildcards".replace("%", esc + "%"), null)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("TABLE_NAME")).isEqualTo("table%with%wildcards");
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    public void getTableTypes() throws SQLException {
        ResultSet rs = meta.getTableTypes();
        assertThat(rs).isNotNull();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("GLOBAL TEMPORARY");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("SYSTEM TABLE");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("TABLE");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_TYPE")).isEqualTo("VIEW");
        assertThat(rs.next()).isFalse();
    }

    @Test
    public void getTypeInfo() throws SQLException {
        ResultSet rs = meta.getTypeInfo();
        assertThat(rs).isNotNull();

        testTypeInfo(
                rs,
                "NULL",
                Types.NULL,
                0,
                null,
                null,
                null,
                DatabaseMetaData.typeNullable,
                false,
                DatabaseMetaData.typeSearchable,
                true,
                false,
                false,
                0,
                0,
                10);
        testTypeInfo(
                rs,
                "INTEGER",
                Types.INTEGER,
                0,
                null,
                null,
                null,
                DatabaseMetaData.typeNullable,
                false,
                DatabaseMetaData.typeSearchable,
                false,
                false,
                true,
                0,
                0,
                10);
        testTypeInfo(
                rs,
                "REAL",
                Types.REAL,
                0,
                null,
                null,
                null,
                DatabaseMetaData.typeNullable,
                false,
                DatabaseMetaData.typeSearchable,
                false,
                false,
                false,
                0,
                0,
                10);
        testTypeInfo(
                rs,
                "TEXT",
                Types.VARCHAR,
                0,
                null,
                null,
                null,
                DatabaseMetaData.typeNullable,
                true,
                DatabaseMetaData.typeSearchable,
                true,
                false,
                false,
                0,
                0,
                10);
        testTypeInfo(
                rs,
                "BLOB",
                Types.BLOB,
                0,
                null,
                null,
                null,
                DatabaseMetaData.typeNullable,
                false,
                DatabaseMetaData.typeSearchable,
                true,
                false,
                false,
                0,
                0,
                10);

        assertThat(rs.next()).isFalse();
    }

    private void testTypeInfo(
            ResultSet rs,
            String name,
            int type,
            int precision,
            String literalPrefix,
            String literalSuffix,
            String createParams,
            int nullable,
            boolean caseSensitive,
            int searchable,
            boolean unsigned,
            boolean fixedPrecScale,
            boolean autoIncrement,
            int minScale,
            int maxScale,
            int radix)
            throws SQLException {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo(name);
        assertThat(rs.getInt(2)).isEqualTo(type);
        assertThat(rs.getInt(3)).isEqualTo(precision);
        assertThat(rs.getString(4)).isEqualTo(literalPrefix);
        assertThat(rs.getString(5)).isEqualTo(literalSuffix);
        assertThat(rs.getString(6)).isEqualTo(createParams);
        assertThat(rs.getShort(7)).isEqualTo((short) nullable);
        assertThat(rs.getBoolean(8)).isEqualTo(caseSensitive);
        assertThat(rs.getShort(9)).isEqualTo((short) searchable);
        assertThat(rs.getBoolean(10)).isEqualTo(unsigned);
        assertThat(rs.getBoolean(11)).isEqualTo(fixedPrecScale);
        assertThat(rs.getBoolean(12)).isEqualTo(autoIncrement);
        assertThat(rs.getString(13)).isEqualTo(null);
        assertThat(rs.getShort(14)).isEqualTo((short) minScale);
        assertThat(rs.getShort(15)).isEqualTo((short) maxScale);
        assertThat(rs.getInt(16)).isEqualTo(0);
        assertThat(rs.getInt(17)).isEqualTo(0);
        assertThat(rs.getInt(18)).isEqualTo(radix);
    }

    @Test
    public void getColumns() throws SQLException {
        ResultSet rs = meta.getColumns(null, null, "test", "id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.INTEGER);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("INTEGER");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(2000000000);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(0);
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");

        // verify type of metadata columns
        assertThat(rs.getObject("COLUMN_NAME")).isInstanceOf(String.class);
        assertThat(rs.getObject("DATA_TYPE")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("TYPE_NAME")).isInstanceOf(String.class);
        assertThat(rs.getObject("COLUMN_SIZE")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("DECIMAL_DIGITS")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("NUM_PREC_RADIX")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("NULLABLE")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("CHAR_OCTET_LENGTH")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("ORDINAL_POSITION")).isInstanceOf(Integer.class);
        assertThat(rs.getObject("IS_NULLABLE")).isInstanceOf(String.class);
        assertThat(rs.getObject("IS_AUTOINCREMENT")).isInstanceOf(String.class);
        assertThat(rs.getObject("IS_GENERATEDCOLUMN")).isInstanceOf(String.class);

        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.FLOAT);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("FLOAT");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getString("COLUMN_DEF")).isEqualTo("0.0");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(2000000000);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(10);
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("NO");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(2000000000);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(10);
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.INTEGER);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("INTEGER");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(5);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(0);
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.FLOAT);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("REAL");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(11);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(3);
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "charvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.getString("TYPE_NAME")).isEqualTo("VARCHAR");
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(21);
        assertThat(rs.getInt("DECIMAL_DIGITS")).isEqualTo(0);
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test", "%n");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "test%", "%");
        // TABLE "test"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        // VIEW "testView"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "%", "%");
        // SYSTEM TABLE "sqlite_schema"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("sqlite_schema");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("type");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("name");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("tbl_name");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("rootpage");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sql");
        // TABLE "test"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("test");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        // VIEW "testView"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("testView");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("id");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("fn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sn");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("intvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("realvalue");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("charvalue");
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "doesnotexist", "%");
        assertThat(rs.next()).isFalse();
        assertThat(rs.getMetaData().getColumnCount()).isEqualTo(24);

        rs = meta.getColumns(null, null, "sqlite_schema", "%");
        // SYSTEM TABLE "sqlite_schema"
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("sqlite_schema");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("type");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("name");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("tbl_name");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("rootpage");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("sql");
    }

    @Test
    public void getColumnsIncludingGenerated() throws SQLException {
        stat.executeUpdate("create table gh_724 (i integer,j integer generated always as (i))");

        ResultSet rs = meta.getColumns(null, null, "gh_724", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(4)).as("first column is named 'i'").isEqualTo("i");
        assertThat(rs.getString(24)).as("first column is not generated").isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(4)).as("second column is named 'j'").isEqualTo("j");
        assertThat(rs.getString(24)).as("second column is generated").isEqualTo("YES");
        assertThat(rs.next()).isFalse();
    }

    @Test
    public void getColumnsWithEscape() throws SQLException {
        stat.executeUpdate("create table wildcard(col1 integer, co_1 integer, 'co%1' integer)");

        String esc = meta.getSearchStringEscape();
        try (ResultSet rs =
                meta.getColumns(null, null, "wildcard", "co_1".replace("_", esc + "_"))) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("COLUMN_NAME")).isEqualTo("co_1");
            assertThat(rs.next()).isFalse();
        }
        try (ResultSet rs =
                meta.getColumns(null, null, "wildcard", "co%1".replace("%", esc + "%"))) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("COLUMN_NAME")).isEqualTo("co%1");
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    public void numberOfgetImportedKeysCols() throws SQLException {

        stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
        stat.executeUpdate(
                "create table child1 (id1 integer, id2 integer, foreign key(id1) references parent(id1), foreign key(id2) references parent(id2))");
        stat.executeUpdate(
                "create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

        ResultSet importedKeys = meta.getImportedKeys(null, null, "child1");

        // child1: 1st fk (simple)
        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id2");
        assertThat(importedKeys.getString("PK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("child1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id2");

        // child1: 2nd fk (simple)
        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id1");
        assertThat(importedKeys.getString("PK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("child1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id1");

        assertThat(importedKeys.next()).isFalse();

        importedKeys = meta.getImportedKeys(null, null, "child2");

        // child2: 1st fk (composite)
        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id2");
        assertThat(importedKeys.getString("PK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("child2");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id2");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id1");
        assertThat(importedKeys.getString("PK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("child2");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id1");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();
    }

    @Test
    public void numberOfgetExportedKeysCols() throws SQLException {

        stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
        stat.executeUpdate(
                "create table child1 (id1 integer, id2 integer,\r\n foreign\tkey(id1) references parent(id1), foreign key(id2) references parent(id2))");
        stat.executeUpdate(
                "create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

        ResultSet exportedKeys = meta.getExportedKeys(null, null, "parent");

        // 1st fk (simple) - child1
        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id2");
        assertThat(exportedKeys.getString("PK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("child1");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id2");

        // 2nd fk (simple) - child1
        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id1");
        assertThat(exportedKeys.getString("PK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("child1");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id1");

        // 3rd fk (composite) - child2
        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id2");
        assertThat(exportedKeys.getString("PK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("child2");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id2");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id1");
        assertThat(exportedKeys.getString("PK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("child2");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id1");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();
    }

    @Test
    public void getExportedKeysColsForNamedKeys() throws SQLException {

        ResultSet exportedKeys;

        // 1. Check for named primary keys
        // SQL is deliberately in uppercase, to make sure case-sensitivity is maintained
        stat.executeUpdate(
                "CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate(
                "CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");

        exportedKeys = meta.getExportedKeys(null, null, "PARENT1");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT1");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID1");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID1");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();

        // 2. Check for named foreign keys
        // SQL is deliberately in mixed case, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE Parent2 (Id1 INTEGER, DATA1 INTEGER, PRIMARY KEY (Id1))");
        stat.executeUpdate(
                "CREATE TABLE Child2 (Id1 INTEGER, DATA2 INTEGER, CONSTRAINT FK_Child2 FOREIGN KEY(Id1) REFERENCES Parent2(Id1))");

        exportedKeys = meta.getExportedKeys(null, null, "Parent2");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("Parent2");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id1");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_Child2");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("Child2");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("Id1");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();
    }

    @Test
    public void getImportedKeysMultipleColumns() throws SQLException {
        ResultSet importedKeys;
        stat.executeUpdate(
                "CREATE TABLE PONE ( ID_FIRST_ONE INTEGER NOT NULL, ID_SECOND_ONE INTEGER NOT NULL, ADDITIONAL_ONE TEXT, CONSTRAINT PONE_PK PRIMARY KEY (ID_FIRST_ONE, ID_SECOND_ONE) )");
        stat.executeUpdate(
                "CREATE TABLE PTWO ( ID_FIRST_TWO INTEGER NOT NULL, ID_SECOND_TWO INTEGER NOT NULL, ADDITIONAL_TWO TEXT, CONSTRAINT PTWO_PK PRIMARY KEY (ID_FIRST_TWO, ID_SECOND_TWO) )");
        stat.executeUpdate(
                "CREATE TABLE CHILD ( ID_CHILD INTEGER NOT NULL, ID_CHILD_TO_ONE INTEGER NOT NULL, ID_CHILD_TO_TWO INTEGER NOT NULL, CONSTRAINT CHILD_PK PRIMARY KEY (ID_CHILD, ID_CHILD_TO_ONE), CONSTRAINT \"PONE_FK01\" FOREIGN KEY (ID_CHILD, ID_CHILD_TO_ONE) REFERENCES PONE (ID_FIRST_ONE, ID_SECOND_ONE) ON DELETE CASCADE, CONSTRAINT PTWO_FK02 FOREIGN KEY (ID_CHILD, ID_CHILD_TO_TWO) REFERENCES PTWO (ID_FIRST_TWO, ID_SECOND_TWO) )");

        importedKeys = meta.getImportedKeys(null, null, "CHILD");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PONE");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID_FIRST_ONE");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PONE_PK");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("PONE_FK01");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID_CHILD");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PONE");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID_SECOND_ONE");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PONE_PK");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("PONE_FK01");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID_CHILD_TO_ONE");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PTWO");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID_FIRST_TWO");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PTWO_PK");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("PTWO_FK02");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID_CHILD");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PTWO");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID_SECOND_TWO");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PTWO_PK");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("PTWO_FK02");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID_CHILD_TO_TWO");

        assertThat(importedKeys.next()).isFalse();
        importedKeys.close();
    }

    @Test
    public void getImportedKeysColsForNamedKeys() throws SQLException {

        ResultSet importedKeys;

        // 1. Check for named primary keys
        // SQL is deliberately in uppercase, to make sure case-sensitivity is maintained
        stat.executeUpdate(
                "CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate(
                "CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");

        importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT1");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID1");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID1");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();

        // 2. Check for named foreign keys
        // SQL is deliberately in mixed case, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE Parent2 (Id1 INTEGER, DATA1 INTEGER, PRIMARY KEY (Id1))");
        stat.executeUpdate(
                "CREATE TABLE Child2 (Id1 INTEGER, DATA2 INTEGER, "
                        + "CONSTRAINT FK_Child2 FOREIGN KEY(Id1) REFERENCES Parent2(Id1))");

        importedKeys = meta.getImportedKeys(null, null, "Child2");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("Parent2");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id1");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_Child2");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("Child2");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("Id1");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();
    }

    @Test
    public void getImportedKeysColsForMixedCaseDefinition() throws SQLException {

        ResultSet importedKeys;

        // SQL is deliberately in mixed-case, to make sure case-sensitivity is maintained
        stat.executeUpdate(
                "CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate(
                "CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, "
                        + "CONSTRAINT FK_Parent1 FOREIGN KEY(ID1) REFERENCES Parent1(Id1))");

        importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("Parent1");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id1");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_Parent1");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID1");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();
    }

    @Test
    public void getImportedKeysColsForMultipleImports() throws SQLException {

        ResultSet importedKeys;

        stat.executeUpdate(
                "CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT1 PRIMARY KEY (ID1))");
        stat.executeUpdate(
                "CREATE TABLE PARENT2 (ID2 INTEGER, DATA2 INTEGER, CONSTRAINT PK_PARENT2 PRIMARY KEY (ID2))");
        stat.executeUpdate(
                "CREATE TABLE CHILD1 (ID1 INTEGER, ID2 INTEGER, "
                        + "CONSTRAINT FK_PARENT1 FOREIGN KEY(ID1) REFERENCES PARENT1(ID1), "
                        + "CONSTRAINT FK_PARENT2 FOREIGN KEY(ID2) REFERENCES PARENT2(ID2))");

        importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT1");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID1");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT1");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_PARENT1");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID1");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT2");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID2");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT2");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_PARENT2");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID2");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();

        // Unnamed foreign keys and unnamed primary keys
        stat.executeUpdate("CREATE TABLE PARENT3 (ID3 INTEGER, DATA3 INTEGER, PRIMARY KEY (ID3))");
        stat.executeUpdate(
                "CREATE TABLE PARENT4 (ID4 INTEGER, DATA4 INTEGER, CONSTRAINT PK_PARENT4 PRIMARY KEY (ID4))");
        stat.executeUpdate(
                "CREATE TABLE CHILD2 (ID3 INTEGER, ID4 INTEGER, "
                        + "FOREIGN KEY(ID3) REFERENCES PARENT3(ID3), "
                        + "CONSTRAINT FK_PARENT4 FOREIGN KEY(ID4) REFERENCES PARENT4(ID4))");

        importedKeys = meta.getImportedKeys(null, null, "CHILD2");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT3");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID3");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD2");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID3");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT4");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID4");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT4");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_PARENT4");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD2");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID4");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();
    }

    @Test
    public void getImportedKeysCols2() throws SQLException {

        stat.executeUpdate(
                "CREATE TABLE Authors (Id INTEGER NOT NULL, Name VARCHAR(20) NOT NULL, "
                        + "CONSTRAINT PK_Authors PRIMARY KEY (Id),"
                        + "  CONSTRAINT CHECK_UPPERCASE_Name CHECK (Name=UPPER(Name)))");
        stat.executeUpdate(
                "CREATE TABLE Books (Id INTEGER NOT NULL, Title VARCHAR(255) NOT NULL, PreviousEditionId INTEGER,"
                        + "CONSTRAINT PK_Books PRIMARY KEY (Id), "
                        + "CONSTRAINT FK_PreviousEdition FOREIGN KEY(PreviousEditionId) REFERENCES Books (Id))");
        stat.executeUpdate(
                "CREATE TABLE BookAuthors (BookId INTEGER NOT NULL, AuthorId INTEGER NOT NULL, "
                        + "CONSTRAINT FK_Y_Book FOREIGN KEY (BookId) REFERENCES Books (Id), "
                        + "CONSTRAINT FK_Z_Author FOREIGN KEY (AuthorId) REFERENCES Authors (Id)) ");

        ResultSet importedKeys;

        importedKeys = meta.getImportedKeys(null, null, "BookAuthors");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("Authors");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_Authors");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_Z_Author");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("BookAuthors");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("AuthorId");

        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("Books");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id");
        assertThat(importedKeys.getString("PK_NAME")).isEqualTo("PK_Books");
        assertThat(importedKeys.getString("FK_NAME")).isEqualTo("FK_Y_Book");
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("BookAuthors");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("BookId");

        assertThat(importedKeys.next()).isFalse();

        importedKeys.close();

        ResultSet exportedKeys;

        exportedKeys = meta.getExportedKeys(null, null, "Authors");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("Authors");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("PK_Authors");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_Z_Author");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("BookAuthors");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("AuthorId");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();

        exportedKeys = meta.getExportedKeys(null, null, "Books");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("Books");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("PK_Books");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_Y_Book");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("BookAuthors");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("BookId");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("Books");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("Id");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("PK_Books");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_PreviousEdition"); // ???
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("Books");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("PreviousEditionId");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();
    }

    @Test
    public void getExportedKeysColsForMultipleImports() throws SQLException {

        ResultSet exportedKeys;

        stat.executeUpdate(
                "CREATE TABLE PARENT1 (ID1 INTEGER, ID2 INTEGER, CONSTRAINT PK_PARENT1 PRIMARY KEY (ID1))");
        stat.executeUpdate(
                "CREATE TABLE CHILD1 (ID1 INTEGER, CONSTRAINT FK_PARENT1 FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");
        stat.executeUpdate(
                "CREATE TABLE CHILD2 (ID2 INTEGER, CONSTRAINT FK_PARENT2 FOREIGN KEY(ID2) REFERENCES PARENT1(ID2))");

        exportedKeys = meta.getExportedKeys(null, null, "PARENT1");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT1");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID1");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("PK_PARENT1");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_PARENT1");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD1");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID1");

        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("PARENT1");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("ID2");
        assertThat(exportedKeys.getString("PK_NAME")).isEqualTo("");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("FK_PARENT2");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("CHILD2");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("ID2");

        assertThat(exportedKeys.next()).isFalse();

        exportedKeys.close();
    }

    @Test
    public void getImportedKeysWithIncorrectReference() throws SQLException {

        stat.executeUpdate(
                "create table child (id1 integer, id2 integer, foreign key(id1) references parent(id1))");

        try (ResultSet importedKeys = meta.getImportedKeys(null, null, "child")) {
            assertThat(importedKeys.next()).isTrue();

            assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("parent");
            assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id1");
            assertThat(importedKeys.getString("PK_NAME")).isNotNull();
            assertThat(importedKeys.getString("FK_NAME")).isNotNull();
            assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("child");
            assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("id1");

            assertThat(importedKeys.next()).isFalse();
        }
    }

    @Test
    public void columnOrderOfgetTables() throws SQLException {

        stat.executeUpdate(
                "CREATE TABLE TABLE1 (ID1 INTEGER PRIMARY KEY AUTOINCREMENT, ID2 INTEGER)");
        stat.executeUpdate("CREATE TABLE TABLE2 (ID2 INTEGER, DATA2 VARCHAR(20))");
        stat.executeUpdate("CREATE TEMP TABLE TABLE3 (ID3 INTEGER, DATA3 VARCHAR(20))");
        stat.executeUpdate("CREATE VIEW VIEW1 (V1, V2) AS SELECT ID1, ID2 FROM TABLE1");

        ResultSet rsTables =
                meta.getTables(
                        null,
                        null,
                        null,
                        new String[] {"TABLE", "VIEW", "GLOBAL TEMPORARY", "SYSTEM TABLE"});

        assertThat(rsTables.next()).isTrue();

        // Check order of columns
        ResultSetMetaData rsmeta = rsTables.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(10);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TABLE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("TABLE_TYPE");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("REMARKS");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("TYPE_CAT");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("TYPE_SCHEM");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(9)).isEqualTo("SELF_REFERENCING_COL_NAME");
        assertThat(rsmeta.getColumnName(10)).isEqualTo("REF_GENERATION");

        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("TABLE3");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("GLOBAL TEMPORARY");

        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("sqlite_schema");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("SYSTEM TABLE");

        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("sqlite_sequence");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("SYSTEM TABLE");

        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("TABLE1");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("TABLE");

        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("TABLE2");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("TABLE");

        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.next()).isTrue();
        assertThat(rsTables.getString("TABLE_NAME")).isEqualTo("VIEW1");
        assertThat(rsTables.getString("TABLE_TYPE")).isEqualTo("VIEW");

        rsTables.close();
    }

    @Test
    public void columnOrderOfgetTableTypes() throws SQLException {
        ResultSet rs = meta.getTableTypes();
        assertThat(rs.next()).isTrue();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(1);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_TYPE");
    }

    @Test
    public void columnOrderOfgetTypeInfo() throws SQLException {
        ResultSet rs = meta.getTypeInfo();
        assertThat(rs.next()).isTrue();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(18);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("PRECISION");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("LITERAL_PREFIX");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("LITERAL_SUFFIX");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("CREATE_PARAMS");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("NULLABLE");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("CASE_SENSITIVE");
        assertThat(rsmeta.getColumnName(9)).isEqualTo("SEARCHABLE");
        assertThat(rsmeta.getColumnName(10)).isEqualTo("UNSIGNED_ATTRIBUTE");
        assertThat(rsmeta.getColumnName(11)).isEqualTo("FIXED_PREC_SCALE");
        assertThat(rsmeta.getColumnName(12)).isEqualTo("AUTO_INCREMENT");
        assertThat(rsmeta.getColumnName(13)).isEqualTo("LOCAL_TYPE_NAME");
        assertThat(rsmeta.getColumnName(14)).isEqualTo("MINIMUM_SCALE");
        assertThat(rsmeta.getColumnName(15)).isEqualTo("MAXIMUM_SCALE");
        assertThat(rsmeta.getColumnName(16)).isEqualTo("SQL_DATA_TYPE");
        assertThat(rsmeta.getColumnName(17)).isEqualTo("SQL_DATETIME_SUB");
        assertThat(rsmeta.getColumnName(18)).isEqualTo("NUM_PREC_RADIX");
    }

    @Test
    public void columnOrderOfgetColumns() throws SQLException {
        ResultSet rs = meta.getColumns(null, null, "test", null);
        assertThat(rs.next()).isTrue();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(24);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TABLE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("COLUMN_SIZE");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("BUFFER_LENGTH");
        assertThat(rsmeta.getColumnName(9)).isEqualTo("DECIMAL_DIGITS");
        assertThat(rsmeta.getColumnName(10)).isEqualTo("NUM_PREC_RADIX");
        assertThat(rsmeta.getColumnName(11)).isEqualTo("NULLABLE");
        assertThat(rsmeta.getColumnName(12)).isEqualTo("REMARKS");
        assertThat(rsmeta.getColumnName(13)).isEqualTo("COLUMN_DEF");
        assertThat(rsmeta.getColumnName(14)).isEqualTo("SQL_DATA_TYPE");
        assertThat(rsmeta.getColumnName(15)).isEqualTo("SQL_DATETIME_SUB");
        assertThat(rsmeta.getColumnName(16)).isEqualTo("CHAR_OCTET_LENGTH");
        assertThat(rsmeta.getColumnName(17)).isEqualTo("ORDINAL_POSITION");
        assertThat(rsmeta.getColumnName(18)).isEqualTo("IS_NULLABLE");
        assertThat(rsmeta.getColumnName(19)).isEqualTo("SCOPE_CATALOG");
        assertThat(rsmeta.getColumnName(20)).isEqualTo("SCOPE_SCHEMA");
        assertThat(rsmeta.getColumnName(21)).isEqualTo("SCOPE_TABLE");
        assertThat(rsmeta.getColumnName(22)).isEqualTo("SOURCE_DATA_TYPE");
        assertThat(rsmeta.getColumnName(23)).isEqualTo("IS_AUTOINCREMENT");
        assertThat(rsmeta.getColumnName(24)).isEqualTo("IS_GENERATEDCOLUMN");
        assertThat(rs.getString("COLUMN_NAME").toUpperCase()).isEqualTo("ID");
        assertThat(rs.getInt("ORDINAL_POSITION")).isEqualTo(1);
    }

    // the following functions always return an empty resultset, so
    // do not bother testing their parameters, only the column types

    @Test
    public void columnOrderOfgetProcedures() throws SQLException {
        ResultSet rs = meta.getProcedures(null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(8);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("PROCEDURE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("PROCEDURE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("PROCEDURE_NAME");
        // currently (Java 1.5), cols 4,5,6 are undefined
        assertThat(rsmeta.getColumnName(7)).isEqualTo("REMARKS");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("PROCEDURE_TYPE");
    }

    @Test
    public void columnOrderOfgetProcedurColumns() throws SQLException {
        ResultSet rs = meta.getProcedureColumns(null, null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(13);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("PROCEDURE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("PROCEDURE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("PROCEDURE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("COLUMN_TYPE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("PRECISION");
        assertThat(rsmeta.getColumnName(9)).isEqualTo("LENGTH");
        assertThat(rsmeta.getColumnName(10)).isEqualTo("SCALE");
        assertThat(rsmeta.getColumnName(11)).isEqualTo("RADIX");
        assertThat(rsmeta.getColumnName(12)).isEqualTo("NULLABLE");
        assertThat(rsmeta.getColumnName(13)).isEqualTo("REMARKS");
    }

    @Test
    public void columnOrderOfgetSchemas() throws SQLException {
        ResultSet rs = meta.getSchemas();
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(2);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_CATALOG");
    }

    @Test
    public void columnOrderOfgetCatalogs() throws SQLException {
        ResultSet rs = meta.getCatalogs();
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(1);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
    }

    @Test
    public void columnOrderOfgetColumnPrivileges() throws SQLException {
        ResultSet rs = meta.getColumnPrivileges(null, null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(8);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TABLE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("GRANTOR");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("GRANTEE");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("PRIVILEGE");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("IS_GRANTABLE");
    }

    @Test
    public void columnOrderOfgetTablePrivileges() throws SQLException {
        ResultSet rs = meta.getTablePrivileges(null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(7);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TABLE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("GRANTOR");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("GRANTEE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("PRIVILEGE");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("IS_GRANTABLE");
    }

    @Test
    public void columnOrderOfgetBestRowIdentifier() throws SQLException {
        ResultSet rs = meta.getBestRowIdentifier(null, null, null, 0, false);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(8);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("SCOPE");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("COLUMN_SIZE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("BUFFER_LENGTH");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("DECIMAL_DIGITS");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("PSEUDO_COLUMN");
    }

    @Test
    public void columnOrderOfgetVersionColumns() throws SQLException {
        ResultSet rs = meta.getVersionColumns(null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(8);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("SCOPE");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("COLUMN_SIZE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("BUFFER_LENGTH");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("DECIMAL_DIGITS");
        assertThat(rsmeta.getColumnName(8)).isEqualTo("PSEUDO_COLUMN");
    }

    @Test
    public void viewIngetPrimaryKeys() throws SQLException {
        ResultSet rs;

        stat.executeUpdate("create table t1 (c1, c2, c3);");
        stat.executeUpdate("create view view_nopk (v1, v2) as select c1, c3 from t1;");

        rs = meta.getPrimaryKeys(null, null, "view_nopk");
        assertThat(rs.next()).isFalse();
    }

    @Test
    public void moreOfgetColumns() throws SQLException {
        ResultSet rs;

        stat.executeUpdate("create table tabcols1 (col1, col2);");
        // mixed-case table, column and primary key names
        stat.executeUpdate("CREATE TABLE TabCols2 (Col1, Col2);");
        // quoted table, column and primary key names
        stat.executeUpdate("CREATE TABLE `TabCols3` (`Col1`, `Col2`);");

        rs = meta.getColumns(null, null, "tabcols1", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("tabcols1");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("col1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("tabcols1");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("col2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "TabCols2", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TabCols2");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("Col1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TabCols2");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("Col2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "TabCols3", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TabCols3");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("Col1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("YES");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TabCols3");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("Col2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();
    }

    @Test
    public void autoincrement() throws SQLException {
        ResultSet rs;

        // no autoincrement no rowid
        stat.executeUpdate(
                "CREATE TABLE TAB1 (COL1 INTEGER NOT NULL PRIMARY KEY, COL2) WITHOUT ROWID;");
        // no autoincrement
        stat.executeUpdate("CREATE TABLE TAB2 (COL1 INTEGER NOT NULL PRIMARY KEY, COL2);");
        // autoincrement
        stat.executeUpdate(
                "CREATE TABLE TAB3 (COL1 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, COL2);");

        rs = meta.getColumns(null, null, "TAB1", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB1");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.INTEGER);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("NO");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB1");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "TAB2", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB2");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.INTEGER);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("NO");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("NO");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB2");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();

        rs = meta.getColumns(null, null, "TAB3", "%");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB3");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL1");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.INTEGER);
        assertThat(rs.getString("IS_NULLABLE")).isEqualTo("NO");
        assertThat(rs.getString("COLUMN_DEF")).isNull();
        assertThat(rs.getString("IS_AUTOINCREMENT")).isEqualTo("YES");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TABLE_NAME")).isEqualTo("TAB3");
        assertThat(rs.getString("COLUMN_NAME")).isEqualTo("COL2");
        assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
        assertThat(rs.next()).isFalse();
    }

    @Test
    public void columnOrderOfgetPrimaryKeys() throws Exception {
        ResultSet rs;
        ResultSetMetaData rsmeta;

        stat.executeUpdate("create table nopk (c1, c2, c3, c4);");
        stat.executeUpdate("create table pk1 (col1 primary key, col2, col3);");
        stat.executeUpdate("create table pk2 (col1, col2 primary key, col3);");
        stat.executeUpdate(
                "create table pk3 (col1, col2, col3, col4, primary key (col3, col2  ));");
        // extra spaces and mixed case are intentional, do not remove!
        stat.executeUpdate(
                "create table pk4 (col1, col2, col3, col4, "
                        + "\r\nCONSTraint\r\nnamed  primary\r\n\t\t key   (col3, col2  ));");
        // mixed-case table, column and primary key names - GitHub issue #219
        stat.executeUpdate(
                "CREATE TABLE Pk5 (Col1, Col2, Col3, Col4, CONSTRAINT NamedPk PRIMARY KEY (Col3, Col2));");
        // quoted table, column and primary key names - GitHub issue #219
        stat.executeUpdate(
                "CREATE TABLE `Pk6` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk` PRIMARY KEY (`Col3`, `Col2`));");
        // spaces before and after "primary key" - GitHub issue #236
        stat.executeUpdate(
                "CREATE TABLE pk7 (col1, col2, col3, col4 VARCHAR(10),PRIMARY KEY (col1, col2, col3));");
        stat.executeUpdate(
                "CREATE TABLE pk8 (col1, col2, col3, col4 VARCHAR(10), PRIMARY KEY(col1, col2, col3));");
        stat.executeUpdate(
                "CREATE TABLE pk9 (col1, col2, col3, col4 VARCHAR(10),PRIMARY KEY(col1, col2, col3));");
        stat.executeUpdate(
                "CREATE TABLE `Pk10` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk`PRIMARY KEY (`Col3`, `Col2`));");
        stat.executeUpdate(
                "CREATE TABLE `Pk11` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk` PRIMARY KEY(`Col3`, `Col2`));");
        stat.executeUpdate(
                "CREATE TABLE `Pk12` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT`NamedPk`PRIMARY KEY(`Col3`,`Col2`));");
        stat.executeUpdate(
                "CREATE TABLE \"Pk13\" (\"Col1\", \"Col2\", \"Col3\", \"Col4\", CONSTRAINT \"NamedPk\" PRIMARY KEY(\"Col3\",\"Col2\"));");
        stat.executeUpdate(
                "CREATE TABLE \"Pk14\" (\"Col1\", \"Col2\", \"Col3\", \"Col4\", PRIMARY KEY(\"Col3\"), FOREIGN KEY (\"Col1\") REFERENCES \"pk1\" (\"col1\"))");
        stat.executeUpdate(
                "CREATE TABLE \"Pk15\" (\"Col1\", \"Col2\", \"Col3\", \"Col4\", PRIMARY KEY(\"Col3\", \"Col2\"), FOREIGN KEY (\"Col1\") REFERENCES \"pk1\" (\"col1\"))");

        rs = meta.getPrimaryKeys(null, null, "nopk");
        assertThat(rs.next()).isFalse();
        rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(6);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TABLE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TABLE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TABLE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("COLUMN_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("KEY_SEQ");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("PK_NAME");
        rs.close();

        assertPrimaryKey(meta, "pk1", null, "col1");
        assertPrimaryKey(meta, "pk2", null, "col2");
        assertPrimaryKey(meta, "pk3", null, "col3", "col2");
        assertPrimaryKey(meta, "pk4", "named", "col3", "col2");
        assertPrimaryKey(meta, "Pk5", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk6", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "pk7", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "pk8", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "pk9", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "Pk10", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk11", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk12", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk13", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk14", null, "Col3");
        assertPrimaryKey(meta, "Pk15", null, "Col3", "Col2");
    }

    private void assertPrimaryKey(
            DatabaseMetaData meta, String tableName, String pkName, String... pkColumns)
            throws Exception {
        final Map<String, Integer> colSeq = new HashMap<>();
        for (int i = 0; i < pkColumns.length; i++) {
            colSeq.put(pkColumns[i], i + 1);
        }
        Arrays.sort(pkColumns);

        final ResultSet rs = meta.getPrimaryKeys(null, null, tableName);
        assertThat(rs.next()).isTrue();
        for (int i = 0; i < pkColumns.length; i++) {
            assertThat(rs.getString("TABLE_CAT"))
                    .as("DatabaseMetaData.getPrimaryKeys: TABLE_CAT")
                    .isNull();
            assertThat(rs.getString("TABLE_SCHEM"))
                    .as("DatabaseMetaData.getPrimaryKeys: TABLE_SCHEM")
                    .isNull();
            assertThat(rs.getString("TABLE_NAME"))
                    .as("DatabaseMetaData.getPrimaryKeys: TABLE_NAME")
                    .isEqualTo(tableName);
            assertThat(rs.getString("COLUMN_NAME"))
                    .as("DatabaseMetaData.getPrimaryKeys: COLUMN_NAME")
                    .isEqualTo(pkColumns[i]);
            assertThat(rs.getString("PK_NAME"))
                    .as("DatabaseMetaData.getPrimaryKeys: PK_NAME")
                    .isEqualTo(pkName);
            assertThat(rs.getInt("KEY_SEQ"))
                    .as("DatabaseMetaData.getPrimaryKeys: KEY_SEQ")
                    .isEqualTo(colSeq.get(pkColumns[i]).intValue());
            if (i < pkColumns.length - 1) assertThat(rs.next()).isTrue();
        }

        assertThat(rs.next()).isFalse();

        rs.close();
    }

    @Test
    public void columnOrderOfgetImportedKeys() throws SQLException {

        stat.executeUpdate("create table person (id integer)");
        stat.executeUpdate(
                "create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet importedKeys = meta.getImportedKeys("default", "global", "address");
        assertThat(importedKeys.next()).isTrue();
        assertThat(importedKeys.getString("PKTABLE_CAT")).isEqualTo("default");
        assertThat(importedKeys.getString("PKTABLE_SCHEM")).isEqualTo("global");
        assertThat(importedKeys.getString("FKTABLE_CAT")).isEqualTo("default");
        assertThat(importedKeys.getString("PKTABLE_NAME")).isEqualTo("person");
        assertThat(importedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id");
        assertThat(importedKeys.getString("PK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FK_NAME")).isNotNull();
        assertThat(importedKeys.getString("FKTABLE_NAME")).isEqualTo("address");
        assertThat(importedKeys.getString("FKCOLUMN_NAME")).isEqualTo("pid");
        importedKeys.close();

        importedKeys = meta.getImportedKeys(null, null, "person");
        assertThat(importedKeys.next()).isFalse();
        importedKeys.close();
    }

    @Test
    public void columnOrderOfgetExportedKeys() throws SQLException {

        stat.executeUpdate("create table person (id integer primary key)");
        stat.executeUpdate(
                "create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet exportedKeys = meta.getExportedKeys("default", "global", "person");
        assertThat(exportedKeys.next()).isTrue();
        assertThat(exportedKeys.getString("PKTABLE_CAT")).isEqualTo("default");
        assertThat(exportedKeys.getString("PKTABLE_SCHEM")).isEqualTo("global");
        assertThat(exportedKeys.getString("FKTABLE_CAT")).isEqualTo("default");
        assertThat(exportedKeys.getString("FKTABLE_SCHEM")).isEqualTo("global");
        assertThat(exportedKeys.getString("PK_NAME")).isNotNull();
        assertThat(exportedKeys.getString("FK_NAME")).isNotNull();

        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("person");
        assertThat(exportedKeys.getString("PKCOLUMN_NAME")).isEqualTo("id");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("address");
        assertThat(exportedKeys.getString("FKCOLUMN_NAME")).isEqualTo("pid");

        exportedKeys.close();

        exportedKeys = meta.getExportedKeys(null, null, "address");
        assertThat(exportedKeys.next()).isFalse();
        exportedKeys.close();

        // With explicit primary column defined.
        stat.executeUpdate("create table REFERRED (ID integer primary key not null)");
        stat.executeUpdate(
                "create table REFERRING (ID integer, RID integer, constraint fk\r\n foreign\tkey\r\n(RID) references REFERRED(id))");

        exportedKeys = meta.getExportedKeys(null, null, "referred");
        assertThat(exportedKeys.getString("PKTABLE_NAME")).isEqualTo("REFERRED");
        assertThat(exportedKeys.getString("FKTABLE_NAME")).isEqualTo("REFERRING");
        assertThat(exportedKeys.getString("FK_NAME")).isEqualTo("fk");
        exportedKeys.close();
    }

    @Test
    public void columnOrderOfgetCrossReference() throws SQLException {
        stat.executeUpdate("create table person (id integer)");
        stat.executeUpdate(
                "create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet cr = meta.getCrossReference(null, null, "person", null, null, "address");
        // assertTrue(cr.next());
        // TODO: unfinished business
    }

    /* TODO

    @Test public void columnOrderOfgetTypeInfo() throws SQLException {
    @Test public void columnOrderOfgetIndexInfo() throws SQLException {
    @Test public void columnOrderOfgetSuperTypes() throws SQLException {
    @Test public void columnOrderOfgetSuperTables() throws SQLException {
    @Test public void columnOrderOfgetAttributes() throws SQLException {*/

    @Test
    public void columnOrderOfgetUDTs() throws SQLException {
        ResultSet rs = meta.getUDTs(null, null, null, null);
        assertThat(rs.next()).isFalse();
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertThat(rsmeta.getColumnCount()).isEqualTo(7);
        assertThat(rsmeta.getColumnName(1)).isEqualTo("TYPE_CAT");
        assertThat(rsmeta.getColumnName(2)).isEqualTo("TYPE_SCHEM");
        assertThat(rsmeta.getColumnName(3)).isEqualTo("TYPE_NAME");
        assertThat(rsmeta.getColumnName(4)).isEqualTo("CLASS_NAME");
        assertThat(rsmeta.getColumnName(5)).isEqualTo("DATA_TYPE");
        assertThat(rsmeta.getColumnName(6)).isEqualTo("REMARKS");
        assertThat(rsmeta.getColumnName(7)).isEqualTo("BASE_TYPE");
    }

    @Test
    public void getIndexInfoOnTest() throws SQLException {
        ResultSet rs = meta.getIndexInfo(null, null, "test", false, false);

        assertThat(rs).isNotNull();
    }

    @Test
    public void getIndexInfoIndexedSingle() throws SQLException {
        stat.executeUpdate(
                "create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn);");

        ResultSet rs = meta.getIndexInfo(null, null, "testindex", false, false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertThat(rs).isNotNull();
        assertThat(rsmd).isNotNull();
    }

    @Test
    public void getIndexInfoIndexedSingleExpr() throws SQLException {
        stat.executeUpdate(
                "create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn, fn/2);");

        ResultSet rs = meta.getIndexInfo(null, null, "testindex", false, false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertThat(rs).isNotNull();
        assertThat(rsmd).isNotNull();
    }

    @Test
    public void getIndexInfoIndexedMulti() throws SQLException {
        stat.executeUpdate(
                "create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn);");
        stat.executeUpdate("create index testindex_pk_idx on testindex (id);");

        ResultSet rs = meta.getIndexInfo(null, null, "testindex", false, false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertThat(rs).isNotNull();
        assertThat(rsmd).isNotNull();
    }

    @Test
    @DisabledInNativeImage // assertj Assumptions do not work in native-image tests
    public void version() throws Exception {
        assumeThat(Utils.getCompileOptions(conn))
                .as("Can't check the version if not compiled by us")
                .contains("JDBC_EXTENSIONS");
        Properties version;
        try (InputStream resourceAsStream =
                DBMetaDataTest.class.getResourceAsStream(
                        "/META-INF/maven/org.xerial/sqlite-jdbc/VERSION")) {
            version = new Properties();
            assumeThat(resourceAsStream).isNotNull();
            version.load(resourceAsStream);
        }
        String versionString = version.getProperty("version");
        int majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
        int minorVersion = Integer.parseInt(versionString.split("\\.")[1]);

        assertThat(majorVersion > 0).as("major version check").isTrue();
        assertThat(meta.getDriverName()).as("driver name").isEqualTo("SQLite JDBC");
        assertThat(
                        meta.getDriverVersion()
                                .startsWith(String.format("%d.%d", majorVersion, minorVersion)))
                .as("driver version")
                .isTrue();
        assertThat(meta.getDriverMajorVersion()).as("driver major version").isEqualTo(majorVersion);
        assertThat(meta.getDriverMinorVersion()).as("driver minor version").isEqualTo(minorVersion);
        assertThat(meta.getDatabaseProductName()).as("db name").isEqualTo("SQLite");
        assertThat(meta.getDatabaseProductVersion()).as("db version").isEqualTo(versionString);
        assertThat(meta.getDatabaseMajorVersion()).as("db major version").isEqualTo(majorVersion);
        assertThat(meta.getDatabaseMinorVersion()).as("db minor version").isEqualTo(minorVersion);
        assertThat(meta.getUserName()).as("user name").isNull();
    }

    @Nested
    class SqliteSchema {
        @ParameterizedTest
        @ValueSource(strings = {"sqlite_schema", "sqlite_master"})
        public void getImportedKeys(String table) throws SQLException {
            ResultSet importedKeys = meta.getImportedKeys(null, null, table);

            assertThat(importedKeys.next()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"sqlite_schema", "sqlite_master"})
        public void getExportedKeys(String table) throws SQLException {
            ResultSet exportedKeys = meta.getExportedKeys(null, null, table);

            assertThat(exportedKeys.next()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"sqlite_schema", "sqlite_master"})
        public void getPrimaryKeys(String table) throws SQLException {
            ResultSet primaryKeys = meta.getPrimaryKeys(null, null, table);

            assertThat(primaryKeys.next()).isFalse();
        }
    }
}
