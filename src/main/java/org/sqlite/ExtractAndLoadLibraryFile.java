package org.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.sqlite.SQLiteJDBCLoader.LOCK_EXT;


public class ExtractAndLoadLibraryFile {

    /**
     * Extracts and loads the specified library file to the target folder
     *
     * @param libFolderForCurrentOS Library path.
     * @param libraryFileName Library name.
     * @param targetFolder Target folder.
     * @return
     */
    static Boolean extractLoadLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        // Include architecture name in temporary filename in order to avoid conflicts
        // when multiple JVMs with different architectures running at the same time
        String uuid = UUID.randomUUID().toString();
        String extractedLibFileName =
                String.format("sqlite-%s-%s-%s", SQLiteJDBCLoader.getVersion(), uuid, libraryFileName);
        String extractedLckFileName = extractedLibFileName + LOCK_EXT;

        Path extractedLibFile = Paths.get(targetFolder, extractedLibFileName);
        Path extractedLckFile = Paths.get(targetFolder, extractedLckFileName);

        try {
            // Extract a native library file into the target directory
            try (InputStream reader = SQLiteJDBCLoader.getResourceAsStream(nativeLibraryFilePath)) {
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
            {
                try (InputStream nativeIn = SQLiteJDBCLoader.getResourceAsStream(nativeLibraryFilePath);
                     InputStream extractedLibIn = Files.newInputStream(extractedLibFile)) {
                    if (!SQLiteJDBCLoader.contentsEquals(nativeIn, extractedLibIn)) {
                        throw new RuntimeException(
                                String.format(
                                        "Failed to write a native library file at %s",
                                        extractedLibFile));
                    }
                }
            }
            return SQLiteJDBCLoader.loadNativeLibrary(targetFolder, extractedLibFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}