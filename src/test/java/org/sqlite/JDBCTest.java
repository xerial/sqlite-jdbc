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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

public class JDBCTest
{
    @BeforeClass
    public static void forName() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

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

}
