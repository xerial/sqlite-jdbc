package org.sqlite.bin;

import org.sqlite.NativeLibraryNotFoundException;

/** Loader which loads the SQLite Native binary file. */
public interface SQLiteNativeLoader {

    /**
     * Load the SQLite Native binary file.
     *
     * @throws NativeLibraryNotFoundException Unable to load the native binary
     */
    void loadSQLiteNative() throws NativeLibraryNotFoundException;
}
