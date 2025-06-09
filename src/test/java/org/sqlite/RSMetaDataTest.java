package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RSMetaDataTest {
    private Connection conn;
    private Statement stat;
    private ResultSetMetaData meta;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate(
                "create table People (pid integer primary key autoincrement, "
                        + " firstname string(255), surname string(25,5), dob date);");
        stat.executeUpdate(
                "create table Film (id integer primary key autoincrement, "
                        + " title string(255) not null, length integer not null, budget real);");
        stat.executeUpdate(
                "insert into people values (null, 'Mohandas', 'Gandhi', " + " '1869-10-02');");
        meta = stat.executeQuery("select pid, firstname, surname from people;").getMetaData();
    }

    @AfterEach
    public void close() throws SQLException {
        stat.executeUpdate("drop table people;");
        stat.close();
        conn.close();
    }

    @Test
    public void catalogName() throws SQLException {
        assertThat(meta.getCatalogName(1)).isEqualTo("");
    }

    @Test
    public void schemaName() throws SQLException {
        assertThat(meta.getSchemaName(1)).isEqualTo("");
    }

    @Test
    public void columns() throws SQLException {
        assertThat(meta.getColumnCount()).isEqualTo(3);
        assertThat(meta.getColumnName(1)).isEqualTo("pid");
        assertThat(meta.getColumnName(2)).isEqualTo("firstname");
        assertThat(meta.getColumnName(3)).isEqualTo("surname");
        assertThat(meta.getColumnType(1)).isEqualTo(Types.INTEGER);
        assertThat(meta.getColumnType(2)).isEqualTo(Types.VARCHAR);
        assertThat(meta.getColumnType(3)).isEqualTo(Types.VARCHAR);
        assertThat(meta.isAutoIncrement(1)).isTrue();
        assertThat(meta.isAutoIncrement(2)).isFalse();
        assertThat(meta.isAutoIncrement(3)).isFalse();
        assertThat(meta.isNullable(1)).isEqualTo(ResultSetMetaData.columnNullable);
        assertThat(meta.isNullable(2)).isEqualTo(ResultSetMetaData.columnNullable);
        assertThat(meta.isNullable(3)).isEqualTo(ResultSetMetaData.columnNullable);
    }

    @Test
    public void columnTypes() throws SQLException {
        stat.executeUpdate(
                "create table tbl (col1 INT, col2 INTEGER, col3 TINYINT, "
                        + "col4 SMALLINT, col5 MEDIUMINT, col6 BIGINT, col7 UNSIGNED BIG INT, "
                        + "col8 INT2, col9 INT8, col10 CHARACTER(20), col11 VARCHAR(255), "
                        + "col12 VARYING CHARACTER(255), col13 NCHAR(55), "
                        + "col14 NATIVE CHARACTER(70), col15 NVARCHAR(100), col16 TEXT, "
                        + "col17 CLOB, col18 BLOB, col19 REAL, col20 DOUBLE, "
                        + "col21 DOUBLE PRECISION, col22 FLOAT, col23 NUMERIC, "
                        + "col24 DECIMAL(10,5), col25 BOOLEAN, col26 DATE, col27 DATETIME, "
                        + "col28 TIMESTAMP, col29 CHAR(70), col30 TEXT, col31 TIMESTAMP)");
        // insert empty data into table otherwise getColumnType returns null
        stat.executeUpdate(
                "insert into tbl values (1, 2, 3, 4, 5, 6, 7, 8, 9,"
                        + "'c', 'varchar', 'varying', 'n', 'n','nvarchar', 'text', 'clob',"
                        + "null, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 0, 12345, 123456, 0, 'char', 'some text',"
                        + "'2022-08-26 10:20:00.123')");
        meta =
                stat.executeQuery(
                                "select col1, col2, col3, col4, col5, col6, col7, col8, col9, "
                                        + "col10, col11, col12, col13, col14, col15, col16, col17, col18, "
                                        + "col19, col20, col21, col22, col23, col24, col25, col26, col27, "
                                        + "col28, col29, col30, "
                                        + "cast(col1 as boolean), col31 from tbl")
                        .getMetaData();

        assertThat(meta.getColumnType(1)).isEqualTo(Types.INTEGER);
        assertThat(meta.getColumnType(2)).isEqualTo(Types.INTEGER);
        assertThat(meta.getColumnType(3)).isEqualTo(Types.TINYINT);
        assertThat(meta.getColumnType(4)).isEqualTo(Types.SMALLINT);
        assertThat(meta.getColumnType(5)).isEqualTo(Types.INTEGER);
        assertThat(meta.getColumnType(6)).isEqualTo(Types.BIGINT);
        assertThat(meta.getColumnType(7)).isEqualTo(Types.BIGINT);
        assertThat(meta.getColumnType(8)).isEqualTo(Types.SMALLINT);
        assertThat(meta.getColumnType(9)).isEqualTo(Types.BIGINT);

        assertThat(meta.getColumnType(10)).isEqualTo(Types.CHAR);
        assertThat(meta.getColumnType(11)).isEqualTo(Types.VARCHAR);
        assertThat(meta.getColumnType(12)).isEqualTo(Types.VARCHAR);
        assertThat(meta.getColumnType(13)).isEqualTo(Types.CHAR);
        assertThat(meta.getColumnType(14)).isEqualTo(Types.CHAR);
        assertThat(meta.getColumnType(15)).isEqualTo(Types.VARCHAR);
        assertThat(meta.getColumnType(16)).isEqualTo(Types.VARCHAR);
        assertThat(meta.getColumnType(17)).isEqualTo(Types.CLOB);

        assertThat(meta.getColumnType(18)).isEqualTo(Types.BLOB);

        assertThat(meta.getColumnType(19)).isEqualTo(Types.REAL);
        assertThat(meta.getColumnType(20)).isEqualTo(Types.DOUBLE);
        assertThat(meta.getColumnType(21)).isEqualTo(Types.DOUBLE);
        assertThat(meta.getColumnType(22)).isEqualTo(Types.FLOAT);
        assertThat(meta.getColumnType(23)).isEqualTo(Types.NUMERIC);
        assertThat(meta.getColumnType(24)).isEqualTo(Types.DECIMAL);
        assertThat(meta.getColumnType(25)).isEqualTo(Types.BOOLEAN);

        assertThat(meta.getColumnType(26)).isEqualTo(Types.DATE);
        assertThat(meta.getColumnType(27)).isEqualTo(Types.DATE);

        assertThat(meta.getColumnType(28)).isEqualTo(Types.TIMESTAMP);
        assertThat(meta.getColumnType(29)).isEqualTo(Types.CHAR);

        assertThat(meta.getColumnType(30)).isEqualTo(Types.VARCHAR);

        assertThat(meta.getColumnType(31)).isEqualTo(Types.BOOLEAN);

        assertThat(meta.getColumnType(32)).isEqualTo(Types.TIMESTAMP);

        assertThat(meta.getPrecision(24)).isEqualTo(10);
        assertThat(meta.getScale(24)).isEqualTo(5);
    }

    @Test
    public void columTypeWithoutTable() throws SQLException {
        ResultSet rs =
                stat.executeQuery(
                        "SELECT FALSE, 1, 3900000000, CAST(3900000000 AS BIGINT), CAST(3900000000 AS VARCHAR(50))");
        ResultSetMetaData meta = rs.getMetaData();

        assertThat(rs.next()).isTrue();

        assertThat(meta.getColumnType(1)).isEqualTo(Types.INTEGER);
        assertThat(meta.isSigned(1)).isTrue();

        assertThat(meta.getColumnType(2)).isEqualTo(Types.INTEGER);
        assertThat(meta.isSigned(2)).isTrue();

        assertThat(meta.getColumnType(3)).isEqualTo(Types.BIGINT);
        assertThat(meta.isSigned(3)).isTrue();

        assertThat(meta.getColumnType(4)).isEqualTo(Types.BIGINT);
        assertThat(meta.isSigned(4)).isTrue();

        assertThat(meta.getColumnType(5)).isEqualTo(Types.VARCHAR);
        assertThat(meta.isSigned(5)).isFalse();

        assertThat(rs.next()).isFalse();
    }

    @Test
    public void testGetColumnClassName() throws SQLException {
        stat.executeUpdate(
                "create table gh_541 (id int, DESCRIPTION varchar(40), price DOUBLE, data BLOB, bool BOOLEAN)");
        stat.executeUpdate("insert into gh_541 values (1, 'description', 28.4, null, True);");
        ResultSetMetaData meta = stat.executeQuery("select * from gh_541").getMetaData();

        assertThat(meta.getColumnClassName(1)).isEqualTo("java.lang.Integer");
        assertThat(meta.getColumnClassName(2)).isEqualTo("java.lang.String");
        assertThat(meta.getColumnClassName(3)).isEqualTo("java.lang.Double");
        assertThat(meta.getColumnClassName(4)).isEqualTo("java.lang.Object");
        assertThat(meta.getColumnClassName(5)).isEqualTo("java.lang.Integer");
    }

    @Test
    public void differentRS() throws SQLException {
        meta = stat.executeQuery("select * from people;").getMetaData();
        assertThat(meta.getColumnCount()).isEqualTo(4);
        assertThat(meta.getColumnName(1)).isEqualTo("pid");
        assertThat(meta.getColumnName(2)).isEqualTo("firstname");
        assertThat(meta.getColumnName(3)).isEqualTo("surname");
        assertThat(meta.getColumnName(4)).isEqualTo("dob");
    }

    @Test
    public void nullable() throws SQLException {
        meta = stat.executeQuery("select * from film;").getMetaData();
        assertThat(meta.isNullable(1)).isEqualTo(ResultSetMetaData.columnNullable);
        assertThat(meta.isNullable(2)).isEqualTo(ResultSetMetaData.columnNoNulls);
        assertThat(meta.isNullable(3)).isEqualTo(ResultSetMetaData.columnNoNulls);
        assertThat(meta.isNullable(4)).isEqualTo(ResultSetMetaData.columnNullable);
    }

    @Test
    public void badTableIndex() {
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> meta.getTableName(5));
    }

    @Test
    public void badColumnIndex() {
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> meta.getColumnName(4));
    }

    @Test
    public void scale() throws SQLException {
        assertThat(meta.getScale(2)).isEqualTo(0);
        assertThat(meta.getScale(3)).isEqualTo(5);
    }

    @Test
    public void tableName() throws SQLException {
        final ResultSet rs = stat.executeQuery("SELECT pid, time(dob) as some_time from people");
        assertThat(rs.getMetaData().getTableName(1)).isEqualTo("People");
        assertThat(rs.getMetaData().getTableName(2)).isEqualTo("");
        rs.close();
    }
}
