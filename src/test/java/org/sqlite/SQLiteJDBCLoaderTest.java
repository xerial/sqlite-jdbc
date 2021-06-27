/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteJDBCLoaderTest.java
// Since: Oct 15, 2007
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SQLiteJDBCLoaderTest
{

    private Connection connection = null;

    @Before
    public void setUp() throws Exception
    {
        connection = null;
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @After
    public void tearDown() throws Exception
    {
        if (connection != null)
            connection.close();
    }

    @Test
    public void query() throws ClassNotFoundException
    {
        try
        {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate("create table person ( id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");

            ResultSet rs = statement.executeQuery("select * from person order by id");
            while (rs.next())
            {
                // read the result set
                rs.getInt(1);
                rs.getString(2);
            }
        }
        catch (SQLException e)
        {
            // if e.getMessage() is "out of memory", it probably means no
            // database file is found
            fail(e.getMessage());
        }
    }

    @Test
    public void function() throws SQLException
    {
        Function.create(connection, "total", new Function() {
            @Override
            protected void xFunc() throws SQLException
            {
                int sum = 0;
                for (int i = 0; i < args(); i++)
                    sum += value_int(i);
                result(sum);
            }
        });

        ResultSet rs = connection.createStatement().executeQuery("select total(1, 2, 3, 4, 5)");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1 + 2 + 3 + 4 + 5);
    }

    @Test
    public void version()
    {
    // System.out.println(SQLiteJDBCLoader.getVersion());
    }

    @Test
    public void test() throws Throwable {
        final AtomicInteger completedThreads = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        for (int i = 0; i < 32; i++) {
            final String connStr = "jdbc:sqlite:target/sample-" + i + ".db";
            final int sleepMillis = i;
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(sleepMillis * 10);
                    } catch (InterruptedException e) {
                    }
                    try {
                        // Uncomment the synchronized block and everything works.
                        //synchronized (TestSqlite.class) {
                        Connection conn = DriverManager.getConnection(connStr);
                        //}
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Assert.fail(e.getLocalizedMessage());
                    }
                    completedThreads.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
        assertEquals(32, completedThreads.get());
    }

    @Test
    public void multipleClassLoader() throws Throwable {
        // Get current classpath
        String[] stringUrls = System.getProperty("java.class.path")
                .split(System.getProperty("path.separator"));
        // Find the classes under test.
        String targetFolderName = "sqlite-jdbc/target/classes";
        File classesDir = null;
        String classesDirPrefix = null;
        for (String stringUrl : stringUrls) {
            int indexOf = stringUrl.indexOf(targetFolderName);
            if (indexOf != -1) {
                classesDir = new File(stringUrl);
                classesDirPrefix = stringUrl.substring(0, indexOf + targetFolderName.length());
                break;
            }
        }
        if (classesDir == null) {
            Assert.fail("Couldn't find classes under test.");
        }
        // Create a JAR file out the classes and resources
        File jarFile = File.createTempFile("jar-for-test-", ".jar");
        createJar(classesDir, classesDirPrefix, jarFile);
        URL[] jarUrl = new URL[] { new URL("file://" + jarFile.getAbsolutePath()) };

        final AtomicInteger completedThreads = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            final int sleepMillis = i;
            pool.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(sleepMillis * 10);
                        // Create an isolated class loader, it should load *different* instances
                        // of SQLiteJDBCLoader.class
                        URLClassLoader classLoader = new URLClassLoader(
                                jarUrl, ClassLoader.getSystemClassLoader().getParent());
                        Class<?> clazz =
                                classLoader.loadClass("org.sqlite.SQLiteJDBCLoader");
                        Method initMethod = clazz.getDeclaredMethod("initialize");
                        initMethod.invoke(null);
                        classLoader.close();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Assert.fail(e.getLocalizedMessage());
                    }
                    completedThreads.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
        assertEquals(4, completedThreads.get());
    }

    private static void createJar(File inputDir, String changeDir, File outputFile) throws IOException {
        JarOutputStream target = new JarOutputStream(new FileOutputStream(outputFile));
        addJarEntry(inputDir, changeDir, target);
        target.close();
    }

    private static void addJarEntry(File source, String changeDir, JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    JarEntry entry = new JarEntry(name.substring(changeDir.length() + 1));
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles()) {
                    addJarEntry(nestedFile, changeDir, target);
                }
                return;
            }

            JarEntry entry = new JarEntry(
                    source.getPath().replace("\\", "/").substring(changeDir.length() + 1));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[8192];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) {
                    break;
                }
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
