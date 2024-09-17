package org.sqlite.bin;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.NativeLibraryNotFoundException;

/**
 * A {@link SQLiteNativeLoader} implementation that loads from java.library.path system property.
 */
public class SQLiteJavaLibraryPathNativeLoader implements SQLiteNativeLoader {

    private static final Logger logger =
            LoggerFactory.getLogger(SQLiteJavaLibraryPathNativeLoader.class);

    @Override
    public void loadSQLiteNative() throws NativeLibraryNotFoundException {
        // try from java.library.path
        String javaLibraryPath = System.getProperty("java.library.path", "");
        String sqliteNativeLibraryName = System.getProperty("org.sqlite.lib.name");

        for (String ldPath : javaLibraryPath.split(File.pathSeparator)) {
            if (ldPath.isEmpty()) {
                continue;
            }
            if (loadNativeLibrary(ldPath, sqliteNativeLibraryName)) {
                return;
            } else {
                throw new NativeLibraryNotFoundException("Tried path: " + ldPath);
            }
        }
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
                logger.debug("Failed to load native library: {}", name, e);
                return false;
            }

        } else {
            return false;
        }
    }
}
