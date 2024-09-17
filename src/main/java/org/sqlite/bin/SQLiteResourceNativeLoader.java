package org.sqlite.bin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.FileException;
import org.sqlite.NativeLibraryNotFoundException;
import org.sqlite.SQLiteJDBCLoader;
import org.sqlite.util.LibraryLoaderUtil;

/**
 * A {@link SQLiteNativeLoader} implementation that loads the os-dependent library from the jar
 * file.
 */
public class SQLiteResourceNativeLoader implements SQLiteNativeLoader {

    private static final String LOCK_EXT = ".lck";

    private static final Logger logger = LoggerFactory.getLogger(SQLiteResourceNativeLoader.class);

    @Override
    public void loadSQLiteNative() throws NativeLibraryNotFoundException {
        String sqliteNativeLibraryPath = LibraryLoaderUtil.getNativeLibResourcePath();
        String sqliteNativeLibraryName = LibraryLoaderUtil.getNativeLibName();

        boolean hasNativeLib =
                LibraryLoaderUtil.hasNativeLib(sqliteNativeLibraryPath, sqliteNativeLibraryName);

        if (hasNativeLib) {
            // temporary library folder
            String tempFolder = getTempDir().getAbsolutePath();
            // Try extracting the library from jar
            try {
                if (extractAndLoadLibraryFile(
                        sqliteNativeLibraryPath, sqliteNativeLibraryName, tempFolder)) {
                    return;
                } else {
                    throw new NativeLibraryNotFoundException(
                            "Could not find library: " + sqliteNativeLibraryPath);
                }
            } catch (FileException e) {
                throw new NativeLibraryNotFoundException(e);
            }
        }
        throw new NativeLibraryNotFoundException("No native library present");
    }

    private static File getTempDir() {
        return new File(
                System.getProperty("org.sqlite.tmpdir", System.getProperty("java.io.tmpdir")));
    }

    /**
     * Extracts and loads the specified library file to the target folder
     *
     * @param libFolderForCurrentOS Library path.
     * @param libraryFileName Library name.
     * @param targetFolder Target folder.
     * @return
     */
    private static boolean extractAndLoadLibraryFile(
            String libFolderForCurrentOS, String libraryFileName, String targetFolder)
            throws FileException {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        // Include architecture name in temporary filename in order to avoid conflicts
        // when multiple JVMs with different architectures running at the same time
        String uuid = UUID.randomUUID().toString();
        String extractedLibFileName =
                String.format(
                        "sqlite-%s-%s-%s", SQLiteJDBCLoader.getVersion(), uuid, libraryFileName);
        String extractedLckFileName = extractedLibFileName + LOCK_EXT;

        Path extractedLibFile = Paths.get(targetFolder, extractedLibFileName);
        Path extractedLckFile = Paths.get(targetFolder, extractedLckFileName);

        try {
            // Extract a native library file into the target directory
            try (InputStream reader = getResourceAsStream(nativeLibraryFilePath)) {
                if (Files.notExists(extractedLckFile)) {
                    Files.createFile(extractedLckFile);
                }

                Files.copy(reader, extractedLibFile, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                // Delete the extracted lib file on JVM exit.
                extractedLibFile.toFile().deleteOnExit();
                extractedLckFile.toFile().deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            extractedLibFile.toFile().setReadable(true);
            extractedLibFile.toFile().setWritable(true, true);
            extractedLibFile.toFile().setExecutable(true);

            // Check whether the contents are properly copied from the resource folder
            try (InputStream nativeIn = getResourceAsStream(nativeLibraryFilePath);
                    InputStream extractedLibIn = Files.newInputStream(extractedLibFile)) {
                if (!contentsEquals(nativeIn, extractedLibIn)) {
                    throw new FileException(
                            String.format(
                                    "Failed to write a native library file at %s",
                                    extractedLibFile));
                }
            }
            return loadNativeLibrary(targetFolder, extractedLibFileName);
        } catch (IOException e) {
            logger.info("Unexpected IOException", e);
            return false;
        }
    }

    // Replacement of java.lang.Class#getResourceAsStream(String) to disable sharing the resource
    // stream
    // in multiple class loaders and specifically to avoid
    // https://bugs.openjdk.java.net/browse/JDK-8205976
    private static InputStream getResourceAsStream(String name) {
        // Remove leading '/' since all our resource paths include a leading directory
        // See:
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/Class.java#L3054
        String resolvedName = name.substring(1);
        ClassLoader cl = SQLiteJDBCLoader.class.getClassLoader();
        URL url = cl.getResource(resolvedName);
        if (url == null) {
            return null;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            logger.info("Could not connect", e);
            return null;
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

    private static boolean contentsEquals(InputStream in1, InputStream in2) throws IOException {
        if (!(in1 instanceof BufferedInputStream)) {
            in1 = new BufferedInputStream(in1);
        }
        if (!(in2 instanceof BufferedInputStream)) {
            in2 = new BufferedInputStream(in2);
        }

        int ch = in1.read();
        while (ch != -1) {
            int ch2 = in2.read();
            if (ch != ch2) {
                return false;
            }
            ch = in1.read();
        }
        int ch2 = in2.read();
        return ch2 == -1;
    }
}
