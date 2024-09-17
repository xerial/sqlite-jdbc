package org.sqlite.bin;

/**
 * A {@link SQLiteNativeLoader} implementation that looks in the following locations for the SQLite
 * native library, in order.
 *
 * <ol>
 *   <li>Load library from org.sqlite.lib.path library path
 *   <li>Load the os-dependent library from the jar file
 *   <li>Load from java.library.path
 *   <li>Load through System.loadLibrary
 * </ol>
 */
public class DefaultSQLiteNativeLoaderChain extends AbstractSQLiteNativeLoaderChain {

    private static final DefaultSQLiteNativeLoaderChain INSTANCE =
            new DefaultSQLiteNativeLoaderChain();

    DefaultSQLiteNativeLoaderChain() {
        super(
                new SQLiteSystemPropertyNativeLoader(),
                new SQLiteResourceNativeLoader(),
                new SQLiteJavaLibraryPathNativeLoader(),
                new SQLiteLibraryJDKNativeLoader());
    }

    public static DefaultSQLiteNativeLoaderChain getInstance() {
        return INSTANCE;
    }
}
