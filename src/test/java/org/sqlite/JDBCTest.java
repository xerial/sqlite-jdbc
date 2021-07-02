//--------------------------------------
// sqlite-jdbc Project
//
// JDBCTest.java
// Since: Apr 8, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class JDBCTest
{
    @Test
    public void enableLoadExtensionTest() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("enable_load_extension", "true");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:", prop);
            Statement stat = conn.createStatement();

            // How to build shared lib in Windows
            // # mingw32-gcc -fPIC -c extension-function.c
            // # mingw32-gcc -shared -Wl -o extension-function.dll extension-function.o

            //            stat.executeQuery("select load_extension('extension-function.dll')");
            //
            //            ResultSet rs = stat.executeQuery("select sqrt(4)");
            //            System.out.println(rs.getDouble(1));

        }
        finally {
            if (conn != null)
                conn.close();
        }
    }

    @Test
    public void majorVersion() throws Exception {
        int major = DriverManager.getDriver("jdbc:sqlite:").getMajorVersion();
        int minor = DriverManager.getDriver("jdbc:sqlite:").getMinorVersion();
    }

    @Test
    public void shouldReturnNullIfProtocolUnhandled() throws Exception {
        Assert.assertNull(JDBC.createConnection("jdbc:anotherpopulardatabaseprotocol:", null));
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

    @Test
    public void jdbcHammer() throws Exception{
        final SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        File tempFile = File.createTempFile("myTestDB", ".db");
        dataSource.setUrl("jdbc:sqlite:" + tempFile.getAbsolutePath());
        Connection connection = dataSource.getConnection();
        try{
            connection.setAutoCommit(false);
            Statement stmt = connection.createStatement();
            try{
                stmt.executeUpdate("CREATE TABLE TestTable(ID INT, testval INT, PRIMARY KEY(ID));");
                stmt.executeUpdate("INSERT INTO TestTable (ID, testval) VALUES(1, 0);");
            }finally{
                stmt.close();
            }
            connection.commit();
        }finally{
            connection.close();
        }

        final AtomicInteger count = new AtomicInteger();
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < 10; i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i < 100; i++){
                        try{
                            Connection connection = dataSource.getConnection();
                            try{
                                connection.setAutoCommit(false);
                                boolean read = Math.random() < 0.5;
                                if(read){
                                    connection.setReadOnly(true);
                                    Statement statement = connection.createStatement();
                                    try{
                                        ResultSet rs = statement.executeQuery("SELECT * FROM TestTable");
                                        rs.close();
                                    }finally{
                                        statement.close();
                                    }
                                }else{
                                    Statement statement = connection.createStatement();
                                    try{
                                        ResultSet rs = statement.executeQuery("SELECT * FROM TestTable");
                                        try {
                                            while(rs.next()){
                                                int id = rs.getInt("ID");
                                                int value = rs.getInt("testval");
                                                count.incrementAndGet();
                                                statement.executeUpdate("UPDATE TestTable SET testval = " + (value+1) + " WHERE ID = " + id);
                                            }
                                        }finally{
                                            rs.close();
                                        }
                                    }finally{
                                        statement.close();
                                    }
                                    connection.commit();
                                }
                            }finally{
                                connection.close();
                            }
                        }catch(SQLException e){
                            throw new RuntimeException("Worker failed", e);
                        }
                    }
                }
            });
            thread.setName("Worker #" + (i + 1));
            threads.add(thread );
        }
        for(Thread thread : threads){
            thread.start();
        }
        for(Thread thread : threads){
            thread.join();
        }
        Connection connection2 = dataSource.getConnection();
        try{
            connection2.setAutoCommit(false);
            connection2.setReadOnly(true);
            Statement stmt = connection2.createStatement();
            try{
                ResultSet rs = stmt.executeQuery("SELECT * FROM TestTable");
                try{
                    assertTrue(rs.next());
                    int id = rs.getInt("ID");
                    int val = rs.getInt("testval");
                    assertEquals(1, id);
                    assertEquals(count.get(), val);
                    assertFalse(rs.next());
                }finally{
                    rs.close();
                }
            }finally{
                stmt.close();
            }
            connection2.commit();
        }finally{
            connection2.close();
        }

    }

    // helper methods -----------------------------------------------------------------

    private SQLiteDataSource createDatasourceWithExplicitReadonly() {
        DriverManager.setLogWriter(new PrintWriter(System.out));
        SQLiteConfig config = new SQLiteConfig();
        config.setExplicitReadOnly(true);
        config.setBusyTimeout(10000);

        return new SQLiteDataSource(config);
    }


}
