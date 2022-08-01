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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisabledIfEnvironmentVariable(
        named = "SKIP_TEST_OSINFO",
        matches = "true",
        disabledReason = "Those tests would fail when ran on a musl based Linux")
public class OSInfoTest {
    private static final Logger logger = Logger.getLogger(OSInfoTest.class.getName());

    @Test
    public void osName() {
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows XP"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 2000"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows Vista"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 98"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 95"));

        assertEquals("Mac", OSInfo.translateOSNameToFolderName("Mac OS"));
        assertEquals("Mac", OSInfo.translateOSNameToFolderName("Mac OS X"));

        assertEquals("AIX", OSInfo.translateOSNameToFolderName("AIX"));

        assertEquals("Linux", OSInfo.translateOSNameToFolderName("Linux"));
        assertEquals("OS2", OSInfo.translateOSNameToFolderName("OS2"));

        assertEquals("HPUX", OSInfo.translateOSNameToFolderName("HP UX"));
    }

    @Test
    public void archName() {
        assertEquals("i386", OSInfo.translateArchNameToFolderName("i386"));
        assertEquals("x86", OSInfo.translateArchNameToFolderName("x86"));
        assertEquals("ppc", OSInfo.translateArchNameToFolderName("ppc"));
        assertEquals("amd64", OSInfo.translateArchNameToFolderName("amd64"));
    }

    @Test
    public void folderPath() {
        String[] component = OSInfo.getNativeLibFolderPathForCurrentOS().split("/");
        assertEquals(2, component.length);
        assertEquals(OSInfo.getOSName(), component[0]);
        assertEquals(OSInfo.getArchName(), component[1]);
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
            assertEquals(OSInfo.getOSName(), buf.toString());
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
            assertEquals(OSInfo.getArchName(), buf.toString());
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
        assertFalse(OSInfo.isAndroid());
    }

    @Nested
    @SetSystemProperty(key = "java.runtime.name", value = "Java for Android")
    @SetSystemProperty(key = "os.name", value = "Linux for Android")
    class Android {
        @Test
        public void testIsAndroid() {
            assertTrue(OSInfo.isAndroid());
        }

        @Test
        @SetSystemProperty(key = "os.arch", value = "arm")
        public void testArmvNativePath() throws IOException, InterruptedException {
            try {
                ProcessRunner mockRunner = mock(ProcessRunner.class);
                OSInfo.processRunner = mockRunner;
                when(mockRunner.runAndWaitFor("uname -m")).thenReturn("armv7l");

                assertEquals("Linux-Android/arm", OSInfo.getNativeLibFolderPathForCurrentOS());
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

                assertEquals("Linux-Android/aarch64", OSInfo.getNativeLibFolderPathForCurrentOS());
            } finally {
                OSInfo.processRunner = new ProcessRunner();
            }
        }
    }
}
