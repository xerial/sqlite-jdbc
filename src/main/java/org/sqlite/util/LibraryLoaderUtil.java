package org.sqlite.util;

import org.sqlite.SQLiteJDBCLoader;

public class LibraryLoaderUtil {
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
        return System.mapLibraryName("sqlitejdbc");
    }

    public static boolean hasNativeLib(String path, String libraryName) {
        return SQLiteJDBCLoader.class.getResource(path + "/" + libraryName) != null;
    }
}
