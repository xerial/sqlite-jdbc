package org.sqlite.util;

import org.sqlite.SQLiteJDBCLoader;

public class LibraryLoaderUtil {

    public static final String NATIVE_LIB_BASE_NAME = "sqlitejdbc";

    /**
     * Get the OS-specific resource directory within the jar, where the relevant sqlitejdbc native
     * library is located.
     */
    public static String getNativeLibResourcePath() {
        String packagePath = SQLiteJDBCLoader.class.getPackage().getName().replace(".", "/");
        return String.format(
                "/%s/native/%s", packagePath, OSInfo.getNativeLibFolderPathForCurrentOS());
    }

    /** Get the OS-specific name of the sqlitejdbc native library. */
    public static String getNativeLibName() {
        return System.mapLibraryName(NATIVE_LIB_BASE_NAME);
    }

    public static boolean hasNativeLib(String path, String libraryName) {
        return SQLiteJDBCLoader.class.getResource(path + "/" + libraryName) != null;
    }
}
