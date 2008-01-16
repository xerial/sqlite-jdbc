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
 * Set the system properties, org.sqlite.lib.path, org.sqlite.lib.name,
 * appropriately so that the SQLite JDBC driver can find *.dll, *.jnilib and
 * *.so files, according to the current OS (win, linux, mac).
 * 
 * The library files are automatically extracted from this project's package
 * (JAR).
 * 
 * usage: call {@link #initialize()} before using SQLite JDBC driver.
 * 
 * @author leo
 * 
 */
public class SQLiteJDBCLoader
{

    private static boolean extracted = false;

    public static void initialize()
    {
        setSQLiteNativeLibraryPath();
    }

    private static boolean extractLibraryFile(String libraryResourcePath, String libraryFolder, String libraryFileName)
    {
        File libFile = new File(libraryFolder, libraryFileName);

        try
        {
            if (!libFile.exists())
            {
                // extract file into the current directory
                InputStream reader = SQLiteJDBCLoader.class.getResourceAsStream(libraryResourcePath);
                FileOutputStream writer = new FileOutputStream(libFile);
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = reader.read(buffer)) != -1)
                {
                    writer.write(buffer, 0, bytesRead);
                }

                writer.close();
                reader.close();

                if (!System.getProperty("os.name").contains("Windows"))
                {
                    try
                    {
                        Runtime.getRuntime().exec(new String[] { "chmod", "755", libFile.getAbsolutePath() }).waitFor();
                    }
                    catch (Throwable e)
                    {}
                }
            }

            return setNativeLibraryPath(libraryFolder, libraryFileName);
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
            return false;
        }

    }

    private static boolean setNativeLibraryPath(String path, String name)
    {
        File libPath = new File(path, name);
        if (libPath.exists())
        {
            System.setProperty("org.sqlite.lib.path", path == null ? "./" : path);
            System.setProperty("org.sqlite.lib.name", name);
            return true;
        }
        else
            return false;
    }

    private static void setSQLiteNativeLibraryPath()
    {
        if (extracted)
            return;

        // Try loading library from org.sqlite.lib.path library path */
        String sqliteNativeLibraryPath = System.getProperty("org.sqlite.lib.path");
        String sqliteNativeLibraryName = System.getProperty("org.sqlite.lib.name");
        if (sqliteNativeLibraryName == null)
            sqliteNativeLibraryName = System.mapLibraryName("sqlitejdbc");

        if (sqliteNativeLibraryPath != null)
        {
            if (setNativeLibraryPath(sqliteNativeLibraryPath, sqliteNativeLibraryName))
            {
                extracted = true;
                return;
            }
        }

        // Load the os-dependent library from a jar file
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows"))
        {
            sqliteNativeLibraryPath = "/native/win";
        }
        else if (osName.contains("Mac"))
        {
            sqliteNativeLibraryPath = "/native/mac";
        }
        else if (osName.contains("Linux"))
        {
            sqliteNativeLibraryPath = "/native/linux";
        }
        else
            throw new UnsupportedOperationException("unsupported OS for SQLite-JDBC driver: " + osName);

        // temporary library folder
        String libraryFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        /* Try extracting thelibrary from jar */
        if (extractLibraryFile(sqliteNativeLibraryPath + "/" + sqliteNativeLibraryName, libraryFolder,
                sqliteNativeLibraryName))
        {
            extracted = true;
            return;
        }

        extracted = false;
        return;
    }

}
