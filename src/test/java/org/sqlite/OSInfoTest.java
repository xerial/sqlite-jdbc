//--------------------------------------
// sqlite-jdbc Project
//
// OSInfoTest.java
// Since: May 20, 2008
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class OSInfoTest
{
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
    public void testMainForOSName() throws Exception {

        // preserve the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] { "--os" });
            assertEquals(OSInfo.getOSName(), buf.toString());
        }
        finally {
            // reset STDOUT
            System.setOut(out);
        }

    }

    @Test
    public void testMainForArchName() throws Exception {

        // preserver the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] { "--arch" });
            assertEquals(OSInfo.getArchName(), buf.toString());
        }
        finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

}
