# News
*   2021-08-30: sqlite-jdbc-3.36.0.3
    * Fixes for GraalVM
    * Internal update: Migrate to JUnit5. Add CI with GraalVM
*   2021-08-25: sqlite-jdbc-3.36.0.2
    * New Features:
        * Support custom collation creation (#627)
    * Newly Supported OS and Arch:
        * Windows armv7 and arm64 (e.g., Surface Pro X) (#644)
        * FreeBSD aarch64 (#642)
        * Bring back Linux armv6 support (#628)
        * FreeBSD x86 and x86_64 (#639)
        * Dropped DragonFlyBSD support (#641)
    * Other Internal Fixes
        * Add reflect-config, jni-config and native-image.properties to graalvm native image compilation (#631)
        * Fix multipleClassLoader test when directory is renamed (#647)
        * CI tests for Windows and MacOS (#645)
    * Special thanks to @gotson for adding collation support and build configurations for more OS and CPU types!
*   2021-06-30: sqlite-jdbc-3.36.0.1
    * Fixed a date parsing issue #88
    * Added CI for testing JDK16 compatibility. sqlite-jdbc works for JDK8 to JDK16
*   2021-06-27: sqlite-jdbc-3.36.0
    * Upgrade to SQLite 3.36.0
*   2021-06-27: sqlite-jdbc-3.35.0.1
    * Upgraded to SQLite 3.35.0
    * Avoid using slower ByteBuffer decode() method (#575)
    * Allow increasing SQLite limits (#568)
    * Add Automatic-Module-Name for OSGi (#558)
    * Avoid using shared resource streams between class loaders when extracting the native library. (#578)
    * Accept `READ_COMMITTED` and `REPEATABLE_READ` isolation levels (not natively supported) by treating as `SERIALIZABLE`
    * Accept (but ignore) fetch direction hint
    * **Note**: Don't use 3.35.0 if you are Apple Silicon (M1) user. 3.35.0 failed to include M1 binary
*   2020-12-10: sqlite-jdbc-3.34.0
    * Improved the performance of reading String columns
    * Support URI file names (file://...) in backup/restore commands https://www.sqlite.org/uri.html
    * Show SQL strings in PreparedStatements.toString()
*   2020-12-08: sqlite-jdbc-3.32.3.3
    * Apple Silicon (M1) support
*   2020-07-28: sqlite-jdbc-3.32.3.2
    * Enable SQLITE_MAX_MMAP_SIZE compile option again.
    * Fixes issues when using Arm Cortex A8, A9 (32-bit architecture)
*   2020-07-15: sqlite-jdbc-3.32.3.1
    * Remove SQLITE_MAX_MMAP_SIZE compile option, which might be causing performance issues.
*   2020-06-18: sqlite-jdbc-3.32.3
    * Fix multiple CVE reported issues https://github.com/xerial/sqlite-jdbc/issues/501
*   2020-05-04: sqlite-jdbc-3.31.1
    * Upgrade to sqlite 3.31.1
    * Support update/commit/rollback event notifications #350
    * Remove sparse index checks #476
    * Support alpine linux (Linux-alpine)
    * Enabled SQLITE_ENABLE_STAT4 flag
*   2019-12-23: sqlite-jdbc-3.30.1
    * Upgrade to sqlite 3.30.1
    * Various fixes
*   2019-06-24: sqlite-jdbc-3.28.0
    * Upgrade to sqlite 3.28.0
*   2019-03-20: sqlite-jdbc-3.27.2.1
    * Make smaller the jar size by using -Os compiler option
    * Performance improvement for concurrent access.
*   2019-03-18: sqlite-jdbc-3.27.2
    * Upgrade to SQLite [3.27.2](https://www.sqlite.org/releaselog/3_27_2.html)
*   2018-10-01: sqlite-jdbc-3.25.2
    * Upgrade to SQLite [3.25.2](https://www.sqlite.org/releaselog/3_25_2.html)
    * Fixes #74, #318, #349, #363, #365
    * Upsert is supported since this version.
*   2018-05-25: sqlite-jdbc-3.23.1
    * Upgrade to SQLite [3.23.1](https://www.sqlite.org/releaselog/3_23_1.html)
    * Fixes #312, #321, #323, #328
    * Dropped linux armv6 support temporarily
*   2017-12-07: sqlite-jdbc-3.21.0.1
    * Metadata query fixes
    * Fix for Android
*   2017-11-14: sqlite-jdbc-3.21.0
    * Upgrade to SQLite [3.21.0](https://www.sqlite.org/releaselog/3_21_0.html)
    * Various fixes for metadata queries
*   2017-10-08: sqlite-jdbc-3.20.1
    * Upgrade to SQLite [3.20.1](https://www.sqlite.org/releaselog/3_20_1.html)
    * Various bug fixes
*   2017-08-04: sqlite-jdbc-3.20.0
    * Upgrade to SQLite [3.20.0](https://www.sqlite.org/releaselog/3_20_0.html)
    * Support Linux aarch64
    * Fix #239
*   2017-06-22: sqlite-jdbc-3.19.3
    * Upgrade to SQLite [3.19.3](https://www.sqlite.org/releaselog/3_19_3.html)
*   2017-05-18: sqlite-jdbc-3.18.0
    * Upgrade to SQLite [3.18.0](https://www.sqlite.org/releaselog/3_18_0.html)
*   2017-01-10: sqlite-jdbc-3.16.1
    * Upgrade to SQLite [3.16.1](https://www.sqlite.org/releaselog/3_16_1.html)
    * Add experimental support for ppc64, armv5, v6 (Raspberry PI), v7 and android-arm.
    * Fix a bug in prepared statements #74
    * Building all native libraries using cross compilers in docker images
*   2016-11-04: sqlite-jdbc-3.15.1
    * Upgrade to SQLite [3.15.1](https://www.sqlite.org/releaselog/3_15_1.html)
*   2016-11-04: sqlite-jdbc-3.15.0
    * Upgrade to SQLite [3.15.0](https://www.sqlite.org/releaselog/3_15_0.html)
    * Cleanup extracted temp library files upon start
    * Fix various metadata problems

*   2016-09-30: sqlite-jdbc-3.14.2.1
    * Improved the performance for single-threaded applications (#162)

*   2016 09-26: sqlite-jdbc-3.14.2
    * Updated binaries (Using docker for the ease of cross compiling)
    * Fixes native libraries for Raspberry-Pi
    * Dropped support for Mac x86 (The last Mac OS X supporting this architecture was Snow Leopard, 7-year ago!)
    * Default support of JSON1 extension (#76, #127)
    * Implement query progress callback (#137)
    * Use extended error codes (#119)
*   2015 Oct 3rd: sqlite-jdbc-3.8.11.2
    * Fix for Raspberry-Pi 2
    * Add multiple table support for DatabaseMetaData.getColumns
*   2015 August 3rd: sqlite-jdbc-3.8.11.1
    * Fix for Linux ARM native library
*   2015 July 29th: sqlite-jdbc-3.8.11 release.
    * General performance improvement
    * warning: No update for FreeBSD binary (need a contribution of native library!)
*   2015 July 27th: sqlite-jdbc-3.8.10.2 release (Thread-safe date time)
*   2015 May 11th: sqlite-jdbc-3.8.10.1 release
*   2015 May 7th: sqlite-jdbc-3.8.9.1 release
*   2014 October 20th: sqlite-jdbc-3.8.7 released.
    * Fixed the native code loading mechanism to allow loading sqlite-jdbc from multiple class loaders.
*   2014 October 8th: sqlite-jdbc-3.8.6 released.
*   2014 August 7th: sqlite-jdbc-3.8.5-pre1 released.
*   2014 January 5th: sqlite-jdbc-3.8.2-SNAPSHOT Introduced JDBC4 version of driver. (Requires at least Java 6).
    *   Source code is on branch feature/jdbc4
*   2013 August 27th: sqlite-jdbc-3.8.0 snapshot version is [available](https://oss.sonatype.org/content/repositories/snapshots/org/xerial/sqlite-jdbc/3.8.0-SNAPSHOT/)
*   2013 August 19th: sqlite-jdbc-3.7.15-M1
*   2013 March 24th : sqlite-jdbc-3.7.15-SNAPSHOT-2
*   2013 January 22nd: The repositories and documentations were moved to the bitbucket.
*   2012 December 15th: sqlite-jdbc-3.7.15-SNAPSHOT
    *   Removed pure-java.
*   2010 August 27th: [sqlite-jdbc-3.7.2](http://www.xerial.org/maven/repository/snapshot/org/xerial/sqlite-jdbc/) released
*   2010 April 3rd: [beta release of sqlite-jdbc-3.6.23.1-SNAPSHOT](http://www.xerial.org/maven/repository/snapshot/org/xerial/sqlite-jdbc/)
    *   Added online backup/restore functions. Syntax: `backup to (file name)`, `restore from (file name)`.
*   2009 December 10th: [sqlite-jdbc-3.6.20.1](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.20.1/) release.
    *   Read-only connection, recursive trigger, foreign key validation support etc. using SQLiteConfig class.

        ```java
        SQLiteConfig config = new SQLiteConfig();
        // config.setReadOnly(true);
        config.setSharedCache(true);
        config.recursiveTriggers(true);
        // ... other configuration can be set via SQLiteConfig object
        Connection conn = DriverManager.getConnection("jdbc:sqlite:sample.db", config.toProperties());
        ```

*   2009 November 12th: [sqlite-jdbc-3.6.19](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.19/) released.
    *   added 64-bit OS support: 64-bit native SQLite binaries for Windows (x86\_64), Mac (x86\_64) and Linux (adm64) are available.
*   2009 August 19th: [sqlite-jdbc-3.6.17.1](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.17.1/) released.
*   2009 July 2nd: [sqlite-jdbc-3.6.16](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.16/) release.
*   2009 June 4th: [sqlite-jdbc-3.6.14.2](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.14.2/) released.
*   2009 May 19th: [sqlite-jdbc-3.6.14.1](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.6.14.1/) released.
    *   This version supports "jdbc:sqlite::resource:" syntax to access read-only
        DB files contained in JAR archives, or external resources specified via URL, local files address etc. (see also the [details](http://groups.google.com/group/xerial/browse_thread/thread/39acb38f99eb2469/fc6afceabeaa0f76?lnk=gst&q=resource#fc6afceabeaa0f76))


*   2009 February 18th: sqlite-jdbc-3.6.11 released.
    *   Fixed a bug in `PrepStmt`, which does not clear the batch contents after `executeBatch()`.
        [Discussion](http://groups.google.com/group/xerial/browse_thread/thread/1fa83eb36f6d5dab).


*   2009 January 19th: sqlite-jdbc-3.6.10 released. This version is compatible with
    sqlite version 3.6.10. <https://www.sqlite.org/releaselog/3_6_10.html>
    * Added `READ_UNCOMMITTED` mode support for better query performance: (see also <https://www.sqlite.org/sharedcache.html> )

        ```java
        // READ_UNCOMMITTED mode works only in shared_cache mode.
         Properties prop = new Properties();
         prop.setProperty("shared_cache", "true");
         Connection conn = DriverManager.getConnection("jdbc:sqlite:", prop);
         conn.setTransactionIsolation(Conn.TRANSACTION_READ_UNCOMMITTED);
        ```


*   2008 December 17th: sqlite-jdbc-3.6.7 released.
    *   Related information: <https://www.sqlite.org/releaselog/3_6_7.html>
*   2008 December 1st: sqlite-jdbc-3.6.6.2 released,
    *   Fixed a bug incorporated in the version 3.6.6 <https://www.sqlite.org/releaselog/3_6_6_2.html>
*   2008 November 20th: sqlite-jdbc-3.6.6 release.
    *   Related information sqlite-3.6.6 changes: <https://www.sqlite.org/releaselog/3_6_6.html>
*   2008 November 11th: sqlite-jdbc-3.6.4.1. A bug fix release
    *   Pure-java version didn't work correctly. Fixed in both 3.6.4.1 and 3.6.4.
        If you have already downloaded 3.6.4, please obtain the latest one on the download page.
*   2008 October 16th: sqlite-jdbc-3.6.4 released.
    *   Changes from SQLite 3.6.3: <https://www.sqlite.org/releaselog/3_6_4.html>
    *   `R*-Tree` index and `UPDATE/DELETE` syntax with `LIMIT` clause are available from this build.
*   2008 October 14th: sqlite-jdbc-3.6.3 released. Compatible with SQLite 3.6.3.
*   2008 September 18th: sqlite-jdbc-3.6.2 released. Compatible with SQLite 3.6.2
    and contains pure-java and native versions.
*   2008 July 17th: sqlite-jdbc-3.6.0 released. Compatible with SQLite 3.6.0, and
    includes both pure-java and native versions.
*   2008 July 3rd: [sqlite-jdbc-3.5.9-universal](http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.5.9-universal) released.
    This version contains both native and pure-java SQLite libraries, so it probably works in any OS environment.


*   2008 May 29th: Current development revision (sqlite-jdbc-3.5.9-1) can be compiled
    with JDK 6. No need to use JDK 1.5 for compiling SQLiteJDBC.
*   2008 May 20th: sqlite-jdbc-3.5.9 released.
*   2008 May 20th: sqlite-jdbc-3.5.8 released (corresponding to SQLite 3.5.8 and
    sqlite-jdbc-v047). From this release, Windows, Mac OS X, Linux (i386, amd64)
    and Solaris (SunOS, sparcv9) libraries are bundled into one jar file.
*   2008 May 1st: sqlite-jdbc is now in the maven central repository!
    [How to use SQLiteJDBC with Maven2](#using-sqlite-jdbc-with-maven2)
*   2008 Mar. 18th: sqlite-jdbc-3.5.7 released.
    *   This version corresponds to [SQLite 3.5.7](https://www.sqlite.org/releaselog/3_5_7.html).


*   2008 Mar. 10th: sqlite-jdbc-v042 released.
    *   Corresponding to SQLite 3.5.6, which integrates FTS3 (full text search).
*   2008 Jan. 31st: sqlite-jdbc-v038.4 released.
    *   SQLiteJDBCLoader.initialize() is no longer required.
*   2008 Jan. 11th: The Jar files for Windows, Mac OS X and Linux are packed into
    a single Jar file! So, no longer need to use an OS-specific jar file.
*   2007 Dec. 31th: Upgraded to sqlitejdbc-v038
