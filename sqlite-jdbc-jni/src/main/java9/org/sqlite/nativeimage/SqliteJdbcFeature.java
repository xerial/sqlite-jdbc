package org.sqlite.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.sqlite.*;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDB;
import org.sqlite.jdbc3.JDBC3DatabaseMetaData;
import org.sqlite.util.LibraryLoaderUtil;
import org.sqlite.util.OSInfo;
import org.sqlite.util.ProcessRunner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SqliteJdbcFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess a) {
        RuntimeClassInitialization.initializeAtBuildTime(SQLiteJDBCLoader.VersionHolder.class);
        RuntimeClassInitialization.initializeAtBuildTime(JDBC3DatabaseMetaData.class);
        RuntimeClassInitialization.initializeAtBuildTime(OSInfo.class);
        RuntimeClassInitialization.initializeAtBuildTime(ProcessRunner.class);
        RuntimeClassInitialization.initializeAtBuildTime(LibraryLoaderUtil.class);
        a.registerReachabilityHandler(
                this::nativeDbReachable, method(SQLiteJDBCLoader.class, "initialize"));
    }

    private void nativeDbReachable(DuringAnalysisAccess a) {
        handleLibraryResources();
        registerJNICalls();
    }

    private void handleLibraryResources() {
        String libraryPath = LibraryLoaderUtil.getNativeLibResourcePath();
        String libraryName = LibraryLoaderUtil.getNativeLibName();
        // Sanity check
        if (!LibraryLoaderUtil.hasNativeLib(libraryPath, libraryName)) {
            throw new SqliteJdbcFeatureException(
                    "Unable to locate the required native resources for native-image. Please contact the maintainers of sqlite-jdbc.");
        }

        // libraryResource always has a leading '/'
        String libraryResource = libraryPath + "/" + libraryName;
        String exportLocation = System.getProperty("org.sqlite.lib.exportPath", "");
        if (exportLocation.isEmpty()) {
            // Do not export the library and include it in native-image instead
            RuntimeResourceAccess.addResource(
                    SQLiteJDBCLoader.class.getModule(), libraryResource.substring(1));
        } else {
            // export the required library to the specified path,
            // the user is responsible to make sure the created native-image can actually find it.
            Path targetPath = Paths.get(exportLocation, libraryName);
            try (InputStream in = SQLiteJDBCLoader.class.getResourceAsStream(libraryResource)) {
                Files.createDirectories(targetPath.getParent());
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new SqliteJdbcFeatureException(e);
            }
        }
    }

    private void registerJNICalls() {
        // NativeDB and DB JNI calls
        RuntimeJNIAccess.register(NativeDB.class);
        RuntimeJNIAccess.register(
                fields(
                        NativeDB.class,
                        "pointer",
                        "busyHandler",
                        "commitListener",
                        "updateListener",
                        "progressHandler"));
        RuntimeJNIAccess.register(
                method(DB.class, "onUpdate", int.class, String.class, String.class, long.class));
        RuntimeJNIAccess.register(method(DB.class, "onCommit", boolean.class));
        RuntimeJNIAccess.register(method(NativeDB.class, "stringToUtf8ByteArray", String.class));
        RuntimeJNIAccess.register(method(DB.class, "throwex"));
        RuntimeJNIAccess.register(method(DB.class, "throwex", int.class));
        RuntimeJNIAccess.register(method(NativeDB.class, "throwex", String.class));

        // Function JNI calls
        RuntimeJNIAccess.register(Function.class);
        RuntimeJNIAccess.register(fields(Function.class, "context", "value", "args"));
        RuntimeJNIAccess.register(method(Function.class, "xFunc"));

        // Collation JNI calls
        RuntimeJNIAccess.register(Collation.class);
        RuntimeJNIAccess.register(method(Collation.class, "xCompare", String.class, String.class));

        // Function$Aggregate JNI calls
        RuntimeJNIAccess.register(Function.Aggregate.class);
        RuntimeJNIAccess.register(method(Function.Aggregate.class, "xStep"));
        RuntimeJNIAccess.register(method(Function.Aggregate.class, "xFinal"));
        RuntimeJNIAccess.register(method(Function.Aggregate.class, "clone"));

        // Function&Window JNI calls
        RuntimeJNIAccess.register(Function.Window.class);
        RuntimeJNIAccess.register(method(Function.Window.class, "xInverse"));
        RuntimeJNIAccess.register(method(Function.Window.class, "xValue"));

        // DB&ProgressObserver JNI calls
        RuntimeJNIAccess.register(DB.ProgressObserver.class);
        RuntimeJNIAccess.register(
                method(DB.ProgressObserver.class, "progress", int.class, int.class));

        // ProgressHandler JNI calls
        RuntimeJNIAccess.register(ProgressHandler.class);
        RuntimeJNIAccess.register(method(ProgressHandler.class, "progress"));

        // BusyHandler JNI calls
        RuntimeJNIAccess.register(BusyHandler.class);
        RuntimeJNIAccess.register(method(BusyHandler.class, "callback", int.class));

        // Throwable JNI calls
        RuntimeJNIAccess.register(Throwable.class);
        RuntimeJNIAccess.register(method(Throwable.class, "toString"));

        // Other JNI calls
        RuntimeJNIAccess.register(boolean[].class);
    }

    private Method method(Class<?> clazz, String methodName, Class<?>... args) {
        try {
            return clazz.getDeclaredMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            throw new SqliteJdbcFeatureException(e);
        }
    }

    private Field[] fields(Class<?> clazz, String... fieldNames) {
        try {
            Field[] fields = new Field[fieldNames.length];
            for (int i = 0; i < fieldNames.length; i++) {
                fields[i] = clazz.getDeclaredField(fieldNames[i]);
            }
            return fields;
        } catch (NoSuchFieldException e) {
            throw new SqliteJdbcFeatureException(e);
        }
    }

    private static class SqliteJdbcFeatureException extends RuntimeException {
        private SqliteJdbcFeatureException(Throwable cause) {
            super(cause);
        }

        private SqliteJdbcFeatureException(String message) {
            super(message);
        }
    }
}
