/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
// --------------------------------------
// sqlite-jdbc Project
//
// SQLiteJDBCLoaderTest.java
// Since: Oct 15, 2007
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(
        named = "SKIP_TEST_MULTIARCH",
        matches = "true",
        disabledReason = "Those tests would fail when ran on a multi-arch image")
public class MultipleClassLoaderTest {

    private Connection connection = null;

    @BeforeEach
    public void setUp() throws Exception {
        connection = null;
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void multipleClassLoader() throws Throwable {
        // Get current classpath
        String[] stringUrls =
                System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        // Find the classes under test.
        String targetFolderName =
                Paths.get("").toAbsolutePath().resolve(Paths.get("target", "classes")).toString();
        File classesDir = null;
        String classesDirPrefix = null;
        for (String stringUrl : stringUrls) {
            int indexOf = stringUrl.indexOf(targetFolderName);
            if (indexOf != -1) {
                classesDir = new File(stringUrl);
                classesDirPrefix = stringUrl.substring(0, indexOf + targetFolderName.length());
                break;
            }
        }
        if (classesDir == null) {
            fail("Couldn't find classes under test.");
        }

        // find the slf4j-api jar
        String targetSlf4j = Paths.get("org", "slf4j", "slf4j-api").toString();
        Optional<String> slf4jApi =
                Arrays.stream(stringUrls).filter(s -> s.contains(targetSlf4j)).findFirst();
        if (!slf4jApi.isPresent()) fail("Couldn't find slf4j-api");

        // Create a JAR file out the classes and resources
        File jarFile = File.createTempFile("jar-for-test-", ".jar");
        createJar(classesDir, classesDirPrefix, jarFile);
        URL[] jarUrl =
                new URL[] {
                    jarFile.toPath().toUri().toURL(), Paths.get(slf4jApi.get()).toUri().toURL()
                };

        final AtomicInteger completedThreads = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            final int sleepMillis = i;
            pool.execute(
                    () -> {
                        try {
                            Thread.sleep(sleepMillis * 10);
                            // Create an isolated class loader, it should load *different* instances
                            // of SQLiteJDBCLoader.class
                            URLClassLoader classLoader =
                                    new URLClassLoader(
                                            jarUrl, ClassLoader.getSystemClassLoader().getParent());
                            Class<?> clazz = classLoader.loadClass("org.sqlite.SQLiteJDBCLoader");
                            Method initMethod = clazz.getDeclaredMethod("initialize");
                            initMethod.invoke(null);
                            classLoader.close();
                        } catch (Throwable e) {
                            e.printStackTrace();
                            fail(e.getLocalizedMessage());
                        }
                        completedThreads.incrementAndGet();
                    });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
        assertThat(completedThreads.get()).isEqualTo(4);
    }

    private static void createJar(File inputDir, String changeDir, File outputFile)
            throws IOException {
        JarOutputStream target = new JarOutputStream(Files.newOutputStream(outputFile.toPath()));
        addJarEntry(inputDir, changeDir, target);
        target.close();
    }

    private static void addJarEntry(File source, String changeDir, JarOutputStream target)
            throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    JarEntry entry = new JarEntry(name.substring(changeDir.length() + 1));
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles()) {
                    addJarEntry(nestedFile, changeDir, target);
                }
                return;
            }

            JarEntry entry =
                    new JarEntry(
                            source.getPath().replace("\\", "/").substring(changeDir.length() + 1));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(Files.newInputStream(source.toPath()));

            byte[] buffer = new byte[8192];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) {
                    break;
                }
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
