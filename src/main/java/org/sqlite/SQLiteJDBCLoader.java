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
// --------------------------------------
// SQLite JDBC Project
//
// SQLite.java
// Since: 2007/05/10
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.bin.DefaultSQLiteNativeLoaderChain;
import org.sqlite.util.OSInfo;

/**
 * Set the system properties, org.sqlite.lib.path, org.sqlite.lib.name, appropriately so that the
 * SQLite JDBC driver can find *.dll, *.dylib and *.so files, according to the current OS (win,
 * linux, mac).
 *
 * <p>The library files are automatically extracted from this project's package (JAR).
 *
 * <p>usage: call {@link #initialize()} before using SQLite JDBC driver.
 *
 * @author leo
 */
public class SQLiteJDBCLoader {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteJDBCLoader.class);

    private static final String LOCK_EXT = ".lck";
    private static boolean extracted = false;

    /**
     * Loads SQLite native JDBC library.
     *
     * @return True if SQLite native library is successfully loaded; false otherwise.
     */
    public static synchronized boolean initialize() throws Exception {
        // only cleanup before the first extract
        if (!extracted) {
            cleanup();
        }
        loadSQLiteNativeLibrary();
        return extracted;
    }

    private static File getTempDir() {
        return new File(
                System.getProperty("org.sqlite.tmpdir", System.getProperty("java.io.tmpdir")));
    }

    /**
     * Deleted old native libraries e.g. on Windows the DLL file is not removed on VM-Exit (bug #80)
     */
    static void cleanup() {
        String searchPattern = "sqlite-" + getVersion();

        try (Stream<Path> dirList = Files.list(getTempDir().toPath())) {
            dirList.filter(
                            path ->
                                    !path.getFileName().toString().endsWith(LOCK_EXT)
                                            && path.getFileName()
                                                    .toString()
                                                    .startsWith(searchPattern))
                    .forEach(
                            nativeLib -> {
                                Path lckFile = Paths.get(nativeLib + LOCK_EXT);
                                if (Files.notExists(lckFile)) {
                                    try {
                                        Files.delete(nativeLib);
                                    } catch (Exception e) {
                                        logger.error("Failed to delete old native lib", e);
                                    }
                                }
                            });
        } catch (IOException e) {
            logger.error("Failed to open directory", e);
        }
    }

    /**
     * Checks if the SQLite JDBC driver is set to native mode.
     *
     * @return True if the SQLite JDBC driver is set to native Java mode; false otherwise.
     */
    public static boolean isNativeMode() throws Exception {
        // load the driver
        initialize();
        return extracted;
    }

    /**
     * Computes the MD5 value of the input stream.
     *
     * @param input InputStream.
     * @return Encrypted string for the InputStream.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    static String md5sum(InputStream input) throws IOException {
        BufferedInputStream in = new BufferedInputStream(input);

        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            DigestInputStream digestInputStream = new DigestInputStream(in, digest);
            for (; digestInputStream.read() >= 0; ) {}

            ByteArrayOutputStream md5out = new ByteArrayOutputStream();
            md5out.write(digest.digest());
            return md5out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available: " + e);
        } finally {
            in.close();
        }
    }

    /**
     * Loads SQLite native library using given path and name of the library.
     *
     * @throws
     */
    private static void loadSQLiteNativeLibrary() throws Exception {
        if (extracted) {
            return;
        }

        try {
            DefaultSQLiteNativeLoaderChain.getInstance().loadSQLiteNative();
            extracted = true;
        } catch (NativeLibraryNotFoundException e) {
            throw new NativeLibraryNotFoundException(
                    String.format(
                            "No native library found for os.name=%s, os.arch=%s",
                            OSInfo.getOSName(), OSInfo.getArchName()),
                    e);
        }
    }

    @SuppressWarnings("unused")
    private static void getNativeLibraryFolderForTheCurrentOS() {
        String osName = OSInfo.getOSName();
        String archName = OSInfo.getArchName();
    }

    /** @return The major version of the SQLite JDBC driver. */
    public static int getMajorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 0) ? Integer.parseInt(c[0]) : 1;
    }

    /** @return The minor version of the SQLite JDBC driver. */
    public static int getMinorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 1) ? Integer.parseInt(c[1]) : 0;
    }

    /** @return The version of the SQLite JDBC driver. */
    public static String getVersion() {
        return VersionHolder.VERSION;
    }

    /**
     * This class will load the version from resources during <clinit>. By initializing this at
     * build-time in native-image, the resources do not need to be included in the native
     * executable, and we're eliminating the IO operations as well.
     */
    public static final class VersionHolder {
        private static final String VERSION;

        static {
            URL versionFile =
                    VersionHolder.class.getResource(
                            "/META-INF/maven/org.xerial/sqlite-jdbc/pom.properties");
            if (versionFile == null) {
                versionFile =
                        VersionHolder.class.getResource(
                                "/META-INF/maven/org.xerial/sqlite-jdbc/VERSION");
            }

            String version = "unknown";
            try {
                if (versionFile != null) {
                    Properties versionData = new Properties();
                    versionData.load(versionFile.openStream());
                    version = versionData.getProperty("version", version);
                    version = version.trim().replaceAll("[^0-9\\.]", "");
                }
            } catch (IOException e) {
                // inline creation of logger to avoid build-time initialization of the logging
                // framework in native-image
                LoggerFactory.getLogger(VersionHolder.class)
                        .error("Could not read version from file: {}", versionFile, e);
            }
            VERSION = version;
        }
    }
}
