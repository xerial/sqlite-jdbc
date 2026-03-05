/*--------------------------------------------------------------------------
 *  Copyright 2008 Taro L. Saito
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
// OSInfo.java
// Since: May 20, 2008
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Provides OS name and architecture name.
 *
 * @author leo
 */
public class OSInfo {
    protected static ProcessRunner processRunner = new ProcessRunner();
    private static final HashMap<String, String> archMapping = new HashMap<>();

    public static final String X86 = "x86";
    public static final String X86_64 = "x86_64";
    public static final String IA64_32 = "ia64_32";
    public static final String IA64 = "ia64";
    public static final String PPC = "ppc";
    public static final String PPC64 = "ppc64";
    public static final String RISCV64 = "riscv64";

    static {
        // x86 mappings
        archMapping.put(X86, X86);
        archMapping.put("i386", X86);
        archMapping.put("i486", X86);
        archMapping.put("i586", X86);
        archMapping.put("i686", X86);
        archMapping.put("pentium", X86);

        // x86_64 mappings
        archMapping.put(X86_64, X86_64);
        archMapping.put("amd64", X86_64);
        archMapping.put("em64t", X86_64);
        archMapping.put("universal", X86_64); // Needed for openjdk7 in Mac

        // Itanium 64-bit mappings
        archMapping.put(IA64, IA64);
        archMapping.put("ia64w", IA64);

        // Itanium 32-bit mappings, usually an HP-UX construct
        archMapping.put(IA64_32, IA64_32);
        archMapping.put("ia64n", IA64_32);

        // PowerPC mappings
        archMapping.put(PPC, PPC);
        archMapping.put("power", PPC);
        archMapping.put("powerpc", PPC);
        archMapping.put("power_pc", PPC);
        archMapping.put("power_rs", PPC);

        // TODO: PowerPC 64bit mappings
        archMapping.put(PPC64, PPC64);
        archMapping.put("power64", PPC64);
        archMapping.put("powerpc64", PPC64);
        archMapping.put("power_pc64", PPC64);
        archMapping.put("power_rs64", PPC64);
        archMapping.put("ppc64el", PPC64);
        archMapping.put("ppc64le", PPC64);

        archMapping.put(RISCV64, RISCV64);
    }

    public static void main(String[] args) {
        if (args.length >= 1) {
            if ("--os".equals(args[0])) {
                System.out.print(getOSName());
                return;
            } else if ("--arch".equals(args[0])) {
                System.out.print(getArchName());
                return;
            }
        }

        System.out.print(getNativeLibFolderPathForCurrentOS());
    }

    public static String getNativeLibFolderPathForCurrentOS() {
        return getOSName() + "/" + getArchName();
    }

    public static String getOSName() {
        return translateOSNameToFolderName(System.getProperty("os.name"));
    }

    public static boolean isAndroid() {
        return isAndroidRuntime() || isAndroidTermux() || isRunningAndroid();
    }

    private static boolean isRunningAndroid() {
        // This file is guaranteed to be present on every android version since 1.6 (Donut, API 4),
        // see https://developer.android.com/ndk/guides/stable_apis#graphics
        // We don't use libc/libm/libdl because that has changed what directory its pointing to and
        // OEMs implement the symlink that allows backwards compatibility
        // for apps that use the old path differently, which may cause this check to fail because
        // of common undocumented behaviour. See
        // https://developer.android.com/about/versions/10/behavior-changes-all#bionic
        File androidGLES = new File("/system/lib/libGLESv1_CM.so");
        File android64GLES = new File("/system/lib64/libGLESv1_CM.so");

        return android64GLES.exists() || androidGLES.exists();
    }

    public static boolean isAndroidRuntime() {
        return System.getProperty("java.runtime.name", "").toLowerCase().contains("android");
    }

    public static boolean isAndroidTermux() {
        try {
            return processRunner.runAndWaitFor("uname -o").toLowerCase().contains("android");
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isMusl() {
        Path mapFilesDir = Paths.get("/proc/self/map_files");
        try (Stream<Path> dirStream = Files.list(mapFilesDir)) {
            return dirStream
                    .map(
                            path -> {
                                try {
                                    return path.toRealPath().toString();
                                } catch (IOException e) {
                                    return "";
                                }
                            })
                    .anyMatch(s -> s.toLowerCase().contains("musl"));
        } catch (Exception ignored) {
            // fall back to checking for alpine linux in the event we're using an older kernel which
            // may not fail the above check
            return isAlpineLinux();
        }
    }

    private static boolean isAlpineLinux() {
        try (Stream<String> osLines = Files.lines(Paths.get("/etc/os-release"))) {
            return osLines.anyMatch(l -> l.startsWith("ID") && l.contains("alpine"));
        } catch (Exception ignored2) {
        }
        return false;
    }

    static String getHardwareName() {
        try {
            return processRunner.runAndWaitFor("uname -m");
        } catch (Throwable e) {
            LogHolder.logger.error(() -> "Error while running uname -m", e);
            return "unknown";
        }
    }

    static String resolveArmArchType() {
        if (System.getProperty("os.name").contains("Linux")) {
            String armType = getHardwareName();
            // armType (uname -m) can be armv5t, armv5te, armv5tej, armv5tejl, armv6, armv7, armv7l,
            // aarch64, i686

            // for Android, we fold everything that is not aarch64 into arm
            if (isAndroid()) {
                if (armType.startsWith("aarch64")) {
                    // Use arm64
                    return "aarch64";
                } else {
                    return "arm";
                }
            }

            if (armType.startsWith("armv6")) {
                // Raspberry PI
                return "armv6";
            } else if (armType.startsWith("armv7")) {
                // Generic
                return "armv7";
            } else if (armType.startsWith("armv5")) {
                // Use armv5, soft-float ABI
                return "arm";
            } else if (armType.startsWith("aarch64")) {
                boolean is32bitJVM = "32".equals(System.getProperty("sun.arch.data.model"));
                if (is32bitJVM) {
                    // An aarch64 architecture should support armv7
                    return "armv7";
                } else {
                    // Use arm64
                    return "aarch64";
                }
            }

            // Java 1.8 introduces a system property to determine armel or armhf
            // https://bugs.openjdk.org/browse/JDK-8005545
            String abi = System.getProperty("sun.arch.abi");
            if (abi != null && abi.startsWith("gnueabihf")) {
                return "armv7";
            }

            // For java7, we still need to run some shell commands to determine ABI of JVM
            String javaHome = System.getProperty("java.home");
            try {
                // determine if first JVM found uses ARM hard-float ABI
                int exitCode = Runtime.getRuntime().exec("which readelf").waitFor();
                if (exitCode == 0) {
                    String[] cmdarray = {
                        "/bin/sh",
                        "-c",
                        "find '"
                                + javaHome
                                + "' -name 'libjvm.so' | head -1 | xargs readelf -A | "
                                + "grep 'Tag_ABI_VFP_args: VFP registers'"
                    };
                    exitCode = Runtime.getRuntime().exec(cmdarray).waitFor();
                    if (exitCode == 0) {
                        return "armv7";
                    }
                } else {
                    LogHolder.logger.warn(
                            () ->
                                    "readelf not found. Cannot check if running on an armhf system, armel architecture will be presumed");
                }
            } catch (IOException | InterruptedException e) {
                // ignored: fall back to "arm" arch (soft-float ABI)
            }
        }
        // Use armv5, soft-float ABI
        return "arm";
    }

    public static String getArchName() {
        String override = System.getProperty("org.sqlite.osinfo.architecture");
        if (override != null) {
            return override;
        }

        String osArch = System.getProperty("os.arch");

        if (osArch.startsWith("arm")) {
            osArch = resolveArmArchType();
        } else {
            String lc = osArch.toLowerCase(Locale.US);
            if (archMapping.containsKey(lc)) return archMapping.get(lc);
        }
        return translateArchNameToFolderName(osArch);
    }

    static String translateOSNameToFolderName(String osName) {
        if (osName.contains("Windows")) {
            return "Windows";
        } else if (osName.contains("Mac") || osName.contains("Darwin")) {
            return "Mac";
        } else if (osName.contains("AIX")) {
            return "AIX";
        } else if (isMusl()) {
            return "Linux-Musl";
        } else if (isAndroid()) {
            return "Linux-Android";
        } else if (osName.contains("Linux")) {
            return "Linux";
        } else {
            return osName.replaceAll("\\W", "");
        }
    }

    static String translateArchNameToFolderName(String archName) {
        return archName.replaceAll("\\W", "");
    }

    /**
     * Class-wrapper around the logger object to avoid build-time initialization of the logging
     * framework in native-image
     */
    private static class LogHolder {
        private static final Logger logger = LoggerFactory.getLogger(OSInfo.class);
    }
}
