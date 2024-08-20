package org.sqlite.bin;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.NativeLibraryNotFoundException;

/**
 * A {@link SQLiteNativeLoader} implementation that chains together multiple SQLite native loaders.
 * When loading the SQLite native file from this loader, it calls all the loaders in the chain, in
 * the original order specified, until a loader can successfully load the native library. If all of
 * the loaders in the chain have been called, and none can load the library, then this class will
 * throw an exception.
 */
public abstract class AbstractSQLiteNativeLoaderChain implements SQLiteNativeLoader {

    private static final Logger logger =
            LoggerFactory.getLogger(AbstractSQLiteNativeLoaderChain.class);

    private final Collection<SQLiteNativeLoader> chain;

    AbstractSQLiteNativeLoaderChain(Collection<SQLiteNativeLoader> chain) {
        this.chain = Objects.requireNonNull(chain);
    }

    AbstractSQLiteNativeLoaderChain(SQLiteNativeLoader... chain) {
        this(Arrays.asList(chain));
    }

    @Override
    public void loadSQLiteNative() throws NativeLibraryNotFoundException {
        List<String> exceptionMessages = new LinkedList<>();
        for (SQLiteNativeLoader loader : chain) {
            try {
                loader.loadSQLiteNative();
                return;
            } catch (Exception e) {
                logger.debug("Unable to load SQLite native", e);
                exceptionMessages.add(e.getMessage());
            }
        }
        throw new NativeLibraryNotFoundException(
                "Unable to load SQLite native from chain: " + exceptionMessages);
    }
}
