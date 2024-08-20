package org.sqlite.bin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.NativeLibraryNotFoundException;
import org.sqlite.util.LibraryLoaderUtil;

/**
 * A {@link SQLiteNativeLoader} implementation that loads through System.loadLibrary.
 *
 * @see System#loadLibrary(String)
 */
public class SQLiteLibraryJDKNativeLoader implements SQLiteNativeLoader {

    private static final Logger logger =
            LoggerFactory.getLogger(SQLiteLibraryJDKNativeLoader.class);

    @Override
    public void loadSQLiteNative() throws NativeLibraryNotFoundException {
        try {
            System.loadLibrary(LibraryLoaderUtil.NATIVE_LIB_BASE_NAME);
        } catch (UnsatisfiedLinkError e) {
            logger.debug("Failed to load native library through System.loadLibrary", e);
            throw new NativeLibraryNotFoundException(e);
        }
    }
}
