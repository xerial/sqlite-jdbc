package org.sqlite.bin;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.NativeLibraryNotFoundException;
import org.sqlite.util.LibraryLoaderUtil;

/**
 * A {@link SQLiteNativeLoader} implementation that loads through org.sqlite.lib.path system
 * property.
 */
public class SQLiteSystemPropertyNativeLoader implements SQLiteNativeLoader {

    private static final Logger logger =
            LoggerFactory.getLogger(SQLiteSystemPropertyNativeLoader.class);

    @Override
    public void loadSQLiteNative() throws NativeLibraryNotFoundException {
        // Try loading library from org.sqlite.lib.path library path */
        String sqliteNativeLibraryPath = System.getProperty("org.sqlite.lib.path");
        String sqliteNativeLibraryName = System.getProperty("org.sqlite.lib.name");
        if (sqliteNativeLibraryName == null) {
            sqliteNativeLibraryName = LibraryLoaderUtil.getNativeLibName();
        }

        if (sqliteNativeLibraryPath != null) {
            if (loadNativeLibrary(sqliteNativeLibraryPath, sqliteNativeLibraryName)) {
                return;
            }
        }
        throw new NativeLibraryNotFoundException("");
    }

    /**
     * Loads native library using the given path and name of the library.
     *
     * @param path Path of the native library.
     * @param name Name of the native library.
     * @return True for successfully loading; false otherwise.
     */
    private static boolean loadNativeLibrary(String path, String name) {
        File libPath = new File(path, name);
        if (libPath.exists()) {

            try {
                System.load(new File(path, name).getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e) {
                logger.debug("Failed to load native library: {}.", name, e);
                return false;
            }

        } else {
            return false;
        }
    }
}
