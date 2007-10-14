//--------------------------------------
// SQLite JDBC Project
//
// SQLite.java
// Since: 2007/05/10
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.db.sql.sqlite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Set the system properties, org.sqlite.lib.path, org.sqlite.lib.name, appropriately 
 * so that the SQLite JDBC driver can find *.dll, *.jnilib and *.so files, according   
 * to the current OS (win, linux, mac).
 * 
 * The library files are automatically extracted from this project's package (JAR).
 *
 * usage: call {@link #setSQLiteNativeLibraryPath()} before using SQLite JDBC driver.
 * 
 * @author leo
 *
 */
public class SQLiteJDBCLoader {

    private static boolean extracted = false;
    
    private static boolean extractLibraryFile(String libraryName, String outputFileName)
    {
        File libFile = new File(outputFileName);

        try
        {
            // extract file into the current directory
            InputStream reader = SQLiteJDBCLoader.class.getResourceAsStream(libraryName);
            FileOutputStream writer = new FileOutputStream(libFile);
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1)
            {
                writer.write(buffer, 0, bytesRead);
            }
            
            writer.close();
            reader.close();
            
            if(!System.getProperty("os.name").contains("Windows"))
            {
                try {
                    Runtime.getRuntime ().exec (new String []{"chmod", "755", outputFileName}).waitFor(); 
                } catch (Throwable e) {}
            }
            
            return setNativeLibraryPath(null, outputFileName);
        }
        catch (IOException e)
        {
            return false;
        }

    }

    private static boolean setNativeLibraryPath(String path, String name)
    {
        File libPath = new File(path, name);
        if(libPath.exists())
        {
            System.setProperty("org.sqlite.lib.path", path == null ? "./" : path);
            System.setProperty("org.sqlite.lib.name", name);
            return true;
        }
        else
            return false;
    }

    public static void setSQLiteNativeLibraryPath()
    {
        if (extracted)
            return;

        // Try loading library from org.sqlite.lib.path library path */
        String sqliteNativeLibraryPath = System.getProperty("org.sqlite.lib.path");
        String sqliteNativeLibraryName = System.getProperty("org.sqlite.lib.name");
        if (sqliteNativeLibraryName == null)
            sqliteNativeLibraryName = System.mapLibraryName("sqlitejdbc");

        if (setNativeLibraryPath(sqliteNativeLibraryPath, sqliteNativeLibraryName))
        {
            extracted = true;
            return;
        }

        // Load the os-dependent library from a jar file
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows"))
        {
            sqliteNativeLibraryPath = "/sqlite/win32";
        }
        else if (osName.contains("Mac"))
        {
            sqliteNativeLibraryPath = "/sqlite/mac";
        }
        /*
         * else if (osName.contains("Linux")) { sqliteNativeLibraryPath =
         * "/sqlite/linux"; }
         */
        else
            throw new UnsupportedOperationException("unsupported OS for SQLite-JDBC driver: " + osName);

        /* Try extracting and loading library from jar */
        if (extractLibraryFile(sqliteNativeLibraryPath + "/" + sqliteNativeLibraryName,
                sqliteNativeLibraryName))
        {
            extracted = true;
            return;
        }

        extracted = false;
        return;
    }

}
