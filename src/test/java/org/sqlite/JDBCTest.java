// --------------------------------------
// sqlite-jdbc Project
//
// JDBCTest.java
// Since: Apr 8, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class JDBCTest {
    @Test
    public void enableLoadExtensionTest() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("enable_load_extension", "true");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:", prop)) {
            Statement stat = conn.createStatement();

            // How to build shared lib in Windows
            // # mingw32-gcc -fPIC -c extension-function.c
            // # mingw32-gcc -shared -Wl -o extension-function.dll extension-function.o

            //            stat.executeQuery("select load_extension('extension-function.dll')");
            //
            //            ResultSet rs = stat.executeQuery("select sqrt(4)");
            //            System.out.println(rs.getDouble(1));

        }
    }

    @Test
    public void majorVersion() throws Exception {
        int major = DriverManager.getDriver("jdbc:sqlite:").getMajorVersion();
        int minor = DriverManager.getDriver("jdbc:sqlite:").getMinorVersion();
    }

    @Test
    public void shouldReturnNullIfProtocolUnhandled() throws Exception {
        assertNull(JDBC.createConnection("jdbc:anotherpopulardatabaseprotocol:", null));
    }

    @Test
    public void canSetJdbcConnectionToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        Connection connection = dataSource.getConnection();
        try{
            connection.setAutoCommit(false);
            assertFalse(connection.isReadOnly());
            connection.setReadOnly(true);
            assertTrue(connection.isReadOnly());
            connection.setReadOnly(false);
            assertFalse(connection.isReadOnly());
            connection.setReadOnly(true);
            assertTrue(connection.isReadOnly());
        }finally{
            connection.close();
        }
    }

    @Test
    public void cannotSetJdbcConnectionToReadOnlyAfterFirstStatement() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        Connection connection = dataSource.getConnection();

        try{
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            // execute a statement
            try{
                boolean success = statement.execute("SELECT * FROM sqlite_master");
                assertTrue(success);
            }finally{
                statement.close();
            }
            // try to assign read-only
            try{
                connection.setReadOnly(true);
                fail("Managed to set readOnly = true on a dirty connection!");
            }catch(SQLException expected){
                // pass
            }
        }finally{
            connection.close();
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterCommit() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        Connection connection = dataSource.getConnection();
        try{
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            Statement statement = connection.createStatement();
            // execute a statement
            try{
                boolean success = statement.execute("SELECT * FROM sqlite_master");
                assertTrue(success);
            }finally{
                statement.close();
            }
            connection.commit();

            // try to assign a new read-only value
            connection.setReadOnly(false);
        }finally{
            connection.close();
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterRollback() throws Exception {
        System.out.println("Creating JDBC Datasource");
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        System.out.println("Creating JDBC Connection");
        Connection connection = dataSource.getConnection();
        System.out.println("JDBC Connection created");
        try{
            System.out.println("Disabling auto-commit");
            connection.setAutoCommit(false);
            System.out.println("Creating statement");
            Statement statement = connection.createStatement();
            // execute a statement
            try{
                System.out.println("Executing query");
                boolean success = statement.execute("SELECT * FROM sqlite_master");
                assertTrue(success);
            }finally{
                System.out.println("Closing statement");
                statement.close();
            }
            System.out.println("Performing rollback");
            connection.rollback();

            System.out.println("Setting connection to read-only");
            // try to assign read-only
            connection.setReadOnly(true);
            Statement statement2 = connection.createStatement();
            // execute a statement
            try{
                System.out.println("Executing query 2");
                boolean success = statement2.execute("SELECT * FROM sqlite_master");
                assertTrue(success);
            }finally{
                System.out.println("Closing statement 2");
                statement2.close();
            }
            System.out.println("Performing rollback 2");
            connection.rollback();
        }finally{
            connection.close();
        }
    }

    @Test
    public void cannotExecuteUpdatesWhenConnectionIsSetToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        Connection connection = dataSource.getConnection();
        try{
            connection.setAutoCommit(false);
            connection.setReadOnly(true);

            Statement statement = connection.createStatement();
            // execute a statement
            try {
                statement.execute("CREATE TABLE TestTable(ID VARCHAR(255), PRIMARY KEY(ID))");
                fail("Managed to modify DB contents on a read-only connection!");
            }catch(SQLException expected){
                // pass
            }finally{
                statement.close();
            }
            connection.rollback();

            // try to assign read-only
            connection.setReadOnly(true);
        }finally{
            connection.close();
        }
    }

    // helper methods -----------------------------------------------------------------

    private SQLiteDataSource createDatasourceWithExplicitReadonly() {
        DriverManager.setLogWriter(new PrintWriter(System.out));
        SQLiteConfig config = new SQLiteConfig();
        config.setExplicitReadOnly(true);

        return new SQLiteDataSource(config);
    }


}
