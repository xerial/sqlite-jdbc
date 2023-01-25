package org.sqlite.nativeimage;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.sqlite.*;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDB;
import org.sqlite.jdbc3.JDBC3DatabaseMetaData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SqliteJdbcFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess a) {
        a.registerReachabilityHandler(
                this::nativeDbReachable, method(SQLiteJDBCLoader.class, "initialize"));
    }

    private void nativeDbReachable(DuringAnalysisAccess a) {
        registerResources(a);
        registerJNICalls(a);
    }

    private void registerResources(DuringAnalysisAccess a) {
        RuntimeResourceAccess.addResource(
                SQLiteJDBCLoader.class.getModule(),
                "META-INF/maven/org.xerial/sqlite-jdbc/pom.properties");
        RuntimeResourceAccess.addResource(
                SQLiteJDBCLoader.class.getModule(),
                "META-INF/maven/org.xerial/sqlite-jdbc/VERSION");
        RuntimeResourceAccess.addResource(
                JDBC3DatabaseMetaData.class.getModule(), "sqlite-jdbc.properties");

        // TODO need a smarter way to get this resource location
        String libraryResource;
        if (Platform.includedIn(Platform.WINDOWS_AMD64.class)) {
            libraryResource = "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll";
        } else if (Platform.includedIn(Platform.WINDOWS_AARCH64.class)) {
            libraryResource = "org/sqlite/native/Windows/aarch64/sqlitejdbc.dll";
        } else if (Platform.includedIn(Platform.MACOS_AMD64.class)) {
            libraryResource = "org/sqlite/native/Mac/x86_64/libsqlitejdbc.jnilib";
        } else if (Platform.includedIn(Platform.MACOS_AARCH64.class)) {
            libraryResource = "org/sqlite/native/Mac/aarch64/libsqlitejdbc.jnilib";
        } else if (Platform.includedIn(Platform.LINUX_AMD64.class)) {
            libraryResource = "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so";
        } else if (Platform.includedIn(Platform.LINUX_AARCH64.class)) {
            libraryResource = "org/sqlite/native/Linux/aarch64/libsqlitejdbc.so";
        } else if (Platform.includedIn(Platform.ANDROID_AARCH64.class)) {
            libraryResource = "org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so";
        } else {
            throw new SqliteJdbcFeatureException("Unknown architecture");
        }
        RuntimeResourceAccess.addResource(SQLiteJDBCLoader.class.getModule(), libraryResource);
    }

    private void registerJNICalls(DuringAnalysisAccess a) {
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
        RuntimeJNIAccess.register(method(BusyHandler.class, "callback"));

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
