// --------------------------------------
// sqlite-jdbc Project
//
// OSInfoTest.java
// Since: May 20, 2008
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisabledIfEnvironmentVariable(
        named = "SKIP_TEST_MULTIARCH",
        matches = "true",
        disabledReason = "Those tests would fail when ran on a musl based Linux")
@DisabledInNativeImage
public class OSInfoTest {
    private static final Logger logger = Logger.getLogger(OSInfoTest.class.getName());

    @Test
    public void osName() {
        assertThat(OSInfo.translateOSNameToFolderName("Windows XP")).isEqualTo("Windows");
        assertThat(OSInfo.translateOSNameToFolderName("Windows 2000")).isEqualTo("Windows");
        assertThat(OSInfo.translateOSNameToFolderName("Windows Vista")).isEqualTo("Windows");
        assertThat(OSInfo.translateOSNameToFolderName("Windows 98")).isEqualTo("Windows");
        assertThat(OSInfo.translateOSNameToFolderName("Windows 95")).isEqualTo("Windows");

        assertThat(OSInfo.translateOSNameToFolderName("Mac OS")).isEqualTo("Mac");
        assertThat(OSInfo.translateOSNameToFolderName("Mac OS X")).isEqualTo("Mac");

        assertThat(OSInfo.translateOSNameToFolderName("AIX")).isEqualTo("AIX");

        assertThat(OSInfo.translateOSNameToFolderName("Linux")).isEqualTo("Linux");
        assertThat(OSInfo.translateOSNameToFolderName("OS2")).isEqualTo("OS2");

        assertThat(OSInfo.translateOSNameToFolderName("HP UX")).isEqualTo("HPUX");
    }

    @Test
    public void archName() {
        assertThat(OSInfo.translateArchNameToFolderName("i386")).isEqualTo("i386");
        assertThat(OSInfo.translateArchNameToFolderName("x86")).isEqualTo("x86");
        assertThat(OSInfo.translateArchNameToFolderName("ppc")).isEqualTo("ppc");
        assertThat(OSInfo.translateArchNameToFolderName("amd64")).isEqualTo("amd64");
    }

    @Test
    public void folderPath() {
        String[] component = OSInfo.getNativeLibFolderPathForCurrentOS().split("/");
        assertThat(component.length).isEqualTo(2);
        assertThat(component[0]).isEqualTo(OSInfo.getOSName());
        assertThat(component[1]).isEqualTo(OSInfo.getArchName());
    }

    @Test
    public void testMainForOSName() {

        // preserve the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--os"});
            assertThat(buf.toString()).isEqualTo(OSInfo.getOSName());
        } finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    @Test
    public void testMainForArchName() {

        // preserver the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--arch"});
            assertThat(buf.toString()).isEqualTo(OSInfo.getArchName());
        } finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    @Test
    public void displayOSInfo() {
        logger.info("Hardware name: " + OSInfo.getHardwareName());
        logger.info("OS name: " + OSInfo.getOSName());
        logger.info("Architecture name: " + OSInfo.getArchName());
    }

    // it's unlikely we run tests on an Android device
    @Test
    void testIsNotAndroid() {
        assertThat(OSInfo.isAndroidRuntime()).isFalse();
        assertThat(OSInfo.isAndroidTermux()).isFalse();
        assertThat(OSInfo.isAndroid()).isFalse();
    }

    @Test
    public void testIsAndroidTermux() throws Exception {
        try {
            ProcessRunner mockRunner = mock(ProcessRunner.class);
            OSInfo.processRunner = mockRunner;
            when(mockRunner.runAndWaitFor("uname -o")).thenReturn("Android");

            assertThat(OSInfo.isAndroidTermux()).isTrue();
            assertThat(OSInfo.isAndroidRuntime()).isFalse();
            assertThat(OSInfo.isAndroid()).isTrue();
        } finally {
            OSInfo.processRunner = new ProcessRunner();
        }
    }

    @Nested
    @SetSystemProperty(key = "java.runtime.name", value = "Android Runtime")
    @SetSystemProperty(key = "os.name", value = "Linux")
    class AndroidRuntime {

        @Test
        public void testIsAndroidRuntime() {
            assertThat(OSInfo.isAndroidRuntime()).isTrue();
            assertThat(OSInfo.isAndroidTermux()).isFalse();
            assertThat(OSInfo.isAndroid()).isTrue();
        }

        @Test
        @SetSystemProperty(key = "os.arch", value = "arm")
        public void testArmvNativePath() throws IOException, InterruptedException {
            try {
                ProcessRunner mockRunner = mock(ProcessRunner.class);
                OSInfo.processRunner = mockRunner;
                when(mockRunner.runAndWaitFor("uname -m")).thenReturn("armv7l");

                assertThat(OSInfo.getNativeLibFolderPathForCurrentOS())
                        .isEqualTo("Linux-Android/arm");
            } finally {
                OSInfo.processRunner = new ProcessRunner();
            }
        }

        @Test
        @SetSystemProperty(key = "os.arch", value = "arm64")
        public void testArm64NativePath() throws IOException, InterruptedException {
            try {
                ProcessRunner mockRunner = mock(ProcessRunner.class);
                OSInfo.processRunner = mockRunner;
                when(mockRunner.runAndWaitFor("uname -m")).thenReturn("aarch64");

                assertThat(OSInfo.getNativeLibFolderPathForCurrentOS())
                        .isEqualTo("Linux-Android/aarch64");
            } finally {
                OSInfo.processRunner = new ProcessRunner();
            }
        }
    }

    @Test
    @SetSystemProperty(key = "org.sqlite.osinfo.architecture", value = "overridden")
    @SetSystemProperty(key = "os.name", value = "Windows")
    void testOverride() {
        assertThat(OSInfo.getArchName()).isEqualTo("overridden");
        assertThat(OSInfo.getNativeLibFolderPathForCurrentOS()).isEqualTo("Windows/overridden");
    }
}
