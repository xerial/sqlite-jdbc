/**
 *  Copyright 2009 Taro L. Saito
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
//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteConfig.java
// Since: Dec 8, 2009
//
// $URL$
// $Author$
//--------------------------------------
package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;

/**
 * SQLite Configuration
 *
 * See also http://www.sqlite.org/pragma.html
 *
 * @author leo
 *
 */
public class SQLiteConfig
{
    private final Properties pragmaTable;
    private int openModeFlag = 0x00;
    private TransactionMode transactionMode;
    public final int busyTimeout;

    /* Date storage class*/
    public final static String DEFAULT_DATE_STRING_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public DateClass dateClass;
    public DatePrecision datePrecision;
    public long dateMultiplier;
    public String dateStringFormat;

    /**
     * Default constructor.
     */
    public SQLiteConfig() {
        this(new Properties());
    }

    /**
     * Creates an SQLite configuration object using values from the given
     * property object.
     * @param prop The properties to apply to the configuration.
     */
    public SQLiteConfig(Properties prop) {
        this.pragmaTable = prop;

        String openMode = pragmaTable.getProperty(Pragma.OPEN_MODE.pragmaName);
        if (openMode != null) {
            openModeFlag = Integer.parseInt(openMode);
        }
        else {
            // set the default open mode of SQLite3
            setOpenMode(SQLiteOpenMode.READWRITE);
            setOpenMode(SQLiteOpenMode.CREATE);
        }
        openMode = pragmaTable.getProperty(Pragma.SHARED_CACHE.pragmaName);
        setOpenMode(SQLiteOpenMode.OPEN_URI); // Enable URI filenames

        transactionMode = TransactionMode.getMode(
                pragmaTable.getProperty(Pragma.TRANSACTION_MODE.pragmaName, TransactionMode.DEFFERED.name()));

        dateClass = DateClass.getDateClass(pragmaTable.getProperty(Pragma.DATE_CLASS.pragmaName, DateClass.INTEGER.name()));
        datePrecision = DatePrecision.getPrecision(pragmaTable.getProperty(Pragma.DATE_PRECISION.pragmaName, DatePrecision.MILLISECONDS.name()));
        dateMultiplier = (datePrecision == DatePrecision.MILLISECONDS) ? 1L : 1000L;
        dateStringFormat = pragmaTable.getProperty(Pragma.DATE_STRING_FORMAT.pragmaName, DEFAULT_DATE_STRING_FORMAT);

        busyTimeout = Integer.parseInt(pragmaTable.getProperty(Pragma.BUSY_TIMEOUT.pragmaName, "3000"));
    }

    /**
     * Create a new JDBC connection using the current configuration
     * @return The connection.
     * @throws SQLException
     */
    public Connection createConnection(String url) throws SQLException {
        return JDBC.createConnection(url, toProperties());
    }

    /**
     * Configures a connection.
     * @param conn The connection to configure.
     * @throws SQLException
     */
    public void apply(Connection conn) throws SQLException {

        HashSet<String> pragmaParams = new HashSet<String>();
        for (Pragma each : Pragma.values()) {
            pragmaParams.add(each.pragmaName);
        }

        pragmaParams.remove(Pragma.OPEN_MODE.pragmaName);
        pragmaParams.remove(Pragma.SHARED_CACHE.pragmaName);
        pragmaParams.remove(Pragma.LOAD_EXTENSION.pragmaName);
        pragmaParams.remove(Pragma.DATE_PRECISION.pragmaName);
        pragmaParams.remove(Pragma.DATE_CLASS.pragmaName);
        pragmaParams.remove(Pragma.DATE_STRING_FORMAT.pragmaName);

        Statement stat = conn.createStatement();
        try {
            for (Object each : pragmaTable.keySet()) {
                String key = each.toString();
                if (!pragmaParams.contains(key)) {
                    continue;
                }

                String value = pragmaTable.getProperty(key);
                if (value != null) {
                    stat.execute(String.format("pragma %s=%s", key, value));
                }
            }
        }
        finally {
            if (stat != null) {
                stat.close();
            }
        }

    }

    /**
     * Sets a pragma to the given boolean value.
     * @param pragma The pragma to set.
     * @param flag The boolean value.
     */
    private void set(Pragma pragma, boolean flag) {
        setPragma(pragma, Boolean.toString(flag));
    }

    /**
     * Sets a pragma to the given int value.
     * @param pragma The pragma to set.
     * @param num The int value.
     */
    private void set(Pragma pragma, int num) {
        setPragma(pragma, Integer.toString(num));
    }

    /**
     * Checks if the provided value is the default for a given pragma.
     * @param pragma The pragma on which to check.
     * @param defaultValue The value to check for.
     * @return True if the given value is the default value; false otherwise.
     */
    private boolean getBoolean(Pragma pragma, String defaultValue) {
        return Boolean.parseBoolean(pragmaTable.getProperty(pragma.pragmaName, defaultValue));
    }

    /**
     * Checks if the shared cache option is turned on.
     * @return True if turned on; false otherwise.
     */
    public boolean isEnabledSharedCache() {
        return getBoolean(Pragma.SHARED_CACHE, "false");
    }

    /**
     * Checks if the load extension option is turned on.
     * @return  True if turned on; false otherwise.
     */
    public boolean isEnabledLoadExtension() {
        return getBoolean(Pragma.LOAD_EXTENSION, "false");
    }

    /**
     * @return The open mode flags.
     */
    public int getOpenModeFlags() {
        return openModeFlag;
    }

    /**
     * Sets a pragma's value.
     * @param pragma The pragma to change.
     * @param value The value to set it to.
     */
    public void setPragma(Pragma pragma, String value) {
        pragmaTable.put(pragma.pragmaName, value);
    }

    /**
     * Convert this configuration into a Properties object, which can be
     * passed to the {@link DriverManager#getConnection(String, Properties)}.
     * @return The property object.
     */
    public Properties toProperties() {
        pragmaTable.setProperty(Pragma.OPEN_MODE.pragmaName, Integer.toString(openModeFlag));
        pragmaTable.setProperty(Pragma.TRANSACTION_MODE.pragmaName, transactionMode.getValue());
        pragmaTable.setProperty(Pragma.DATE_CLASS.pragmaName, dateClass.getValue());
        pragmaTable.setProperty(Pragma.DATE_PRECISION.pragmaName, datePrecision.getValue());
        pragmaTable.setProperty(Pragma.DATE_STRING_FORMAT.pragmaName, dateStringFormat);

        return pragmaTable;
    }

    /**
     * @return Array of DriverPropertyInfo objects.
     */
    static DriverPropertyInfo[] getDriverPropertyInfo() {
        Pragma[] pragma = Pragma.values();
        DriverPropertyInfo[] result = new DriverPropertyInfo[pragma.length];
        int index = 0;
        for (Pragma p : Pragma.values()) {
            DriverPropertyInfo di = new DriverPropertyInfo(p.pragmaName, null);
            di.choices = p.choices;
            di.description = p.description;
            di.required = false;
            result[index++] = di;
        }

        return result;
    }

    private static final String[] OnOff = new String[] { "true", "false" };

    public static enum Pragma {

        // Parameters requiring SQLite3 API invocation
        OPEN_MODE("open_mode", "Database open-mode flag", null),
        SHARED_CACHE("shared_cache", "Enable SQLite Shared-Cache mode, native driver only", OnOff),
        LOAD_EXTENSION("enable_load_extension", "Enable SQLite load_extention() function, native driver only", OnOff),

        // Pragmas that can be set after opening the database
        CACHE_SIZE("cache_size"),
        CASE_SENSITIVE_LIKE("case_sensitive_like", OnOff),
        COUNT_CHANGES("count_changes", OnOff),
        DEFAULT_CACHE_SIZE("default_cache_size"),
        EMPTY_RESULT_CALLBACKS("empty_result_callback", OnOff),
        ENCODING("encoding", toStringArray(Encoding.values())),
        FOREIGN_KEYS("foreign_keys", OnOff),
        FULL_COLUMN_NAMES("full_column_names", OnOff),
        FULL_SYNC("fullsync", OnOff),
        INCREMENTAL_VACUUM("incremental_vacuum"),
        JOURNAL_MODE("journal_mode", toStringArray(JournalMode.values())),
        JOURNAL_SIZE_LIMIT("journal_size_limit"),
        LEGACY_FILE_FORMAT("legacy_file_format", OnOff),
        LOCKING_MODE("locking_mode", toStringArray(LockingMode.values())),
        PAGE_SIZE("page_size"),
        MAX_PAGE_COUNT("max_page_count"),
        READ_UNCOMMITED("read_uncommited", OnOff),
        RECURSIVE_TRIGGERS("recursive_triggers", OnOff),
        REVERSE_UNORDERED_SELECTS("reverse_unordered_selects", OnOff),
        SHORT_COLUMN_NAMES("short_column_names", OnOff),
        SYNCHRONOUS("synchronous", toStringArray(SynchronousMode.values())),
        TEMP_STORE("temp_store", toStringArray(TempStore.values())),
        TEMP_STORE_DIRECTORY("temp_store_directory"),
        USER_VERSION("user_version"),

        // Others
        TRANSACTION_MODE("transaction_mode", toStringArray(TransactionMode.values())),
        DATE_PRECISION("date_precision", "\"seconds\": Read and store integer dates as seconds from the Unix Epoch (SQLite standard).\n\"milliseconds\": (DEFAULT) Read and store integer dates as milliseconds from the Unix Epoch (Java standard).", toStringArray(DatePrecision.values())),
        DATE_CLASS("date_class", "\"integer\": (Default) store dates as number of seconds or milliseconds from the Unix Epoch\n\"text\": store dates as a string of text\n\"real\": store dates as Julian Dates", toStringArray(DateClass.values())),
        DATE_STRING_FORMAT("date_string_format", "Format to store and retrieve dates stored as text. Defaults to \"yyyy-MM-dd HH:mm:ss.SSS\"", null),
        BUSY_TIMEOUT("busy_timeout", null);

        public final String   pragmaName;
        public final String[] choices;
        public final String   description;

        private Pragma(String pragmaName) {
            this(pragmaName, null);
        }

        private Pragma(String pragmaName, String[] choices) {
            this(pragmaName, null, choices);
        }

        private Pragma(String pragmaName, String description, String[] choices) {
            this.pragmaName = pragmaName;
            this.description = description;
            this.choices = choices;
        }

        public final String getPragmaName()
        {
            return pragmaName;
        }
    }

    /**
     * Sets the open mode flags.
     * @param mode The open mode.
     * @see <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    public void setOpenMode(SQLiteOpenMode mode) {
        openModeFlag |= mode.flag;
    }

    /**
     * Re-sets the open mode flags.
     * @param mode The open mode.
     * @see <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    public void resetOpenMode(SQLiteOpenMode mode) {
        openModeFlag &= ~mode.flag;
    }

    /**
     * Enables or disables the sharing of the database cache and schema data
     * structures between connections to the same database.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/c3ref/enable_shared_cache.html">www.sqlite.org/c3ref/enable_shared_cache.html</a>
     */
    public void setSharedCache(boolean enable) {
        set(Pragma.SHARED_CACHE, enable);
    }

    /**
     * Enables or disables extension loading.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/c3ref/load_extension.html">www.sqlite.org/c3ref/load_extension.html</a>
     */
    public void enableLoadExtension(boolean enable) {
        set(Pragma.LOAD_EXTENSION, enable);
    }

    /**
     * Sets the read-write mode for the database.
     * @param readOnly True for read-only; otherwise read-write.
     */
    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            setOpenMode(SQLiteOpenMode.READONLY);
            resetOpenMode(SQLiteOpenMode.CREATE);
            resetOpenMode(SQLiteOpenMode.READWRITE);
        }
        else {
            setOpenMode(SQLiteOpenMode.READWRITE);
            setOpenMode(SQLiteOpenMode.CREATE);
            resetOpenMode(SQLiteOpenMode.READONLY);
        }
    }

    /**
     * Changes the maximum number of database disk pages that SQLite will hold
     * in memory at once per open database file.
     * @param numberOfPages Cache size in number of pages.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_cache_size">www.sqlite.org/pragma.html#pragma_cache_size</a>
     */
    public void setCacheSize(int numberOfPages) {
        set(Pragma.CACHE_SIZE, numberOfPages);
    }

    /**
     * Enables or disables case sensitive for the LIKE operator.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_case_sensitive_like">www.sqlite.org/pragma.html#pragma_case_sensitive_like</a>
     */
    public void enableCaseSensitiveLike(boolean enable) {
        set(Pragma.CASE_SENSITIVE_LIKE, enable);
    }

    /**
     * @deprecated
     * Enables or disables the count-changes flag. When enabled, INSERT, UPDATE
     * and DELETE statements return the number of rows they modified.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_count_changes">www.sqlite.org/pragma.html#pragma_count_changes</a>
     */
    public void enableCountChanges(boolean enable) {
        set(Pragma.COUNT_CHANGES, enable);
    }

    /**
     * Sets the suggested maximum number of database disk pages that SQLite will
     * hold in memory at once per open database file. The cache size set here
     * persists across database connections.
     * @param numberOfPages Cache size in number of pages.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_cache_size">www.sqlite.org/pragma.html#pragma_cache_size</a>
     */
    public void setDefaultCacheSize(int numberOfPages) {
        set(Pragma.DEFAULT_CACHE_SIZE, numberOfPages);
    }

    /**
     * @deprecated
     * Enables or disables the empty_result_callbacks flag.
     * @param enable True to enable; false to disable.
     * false.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_empty_result_callbacks">http://www.sqlite.org/pragma.html#pragma_empty_result_callbacks</a>
     */
    public void enableEmptyResultCallBacks(boolean enable) {
        set(Pragma.EMPTY_RESULT_CALLBACKS, enable);
    }

    /**
     * The common interface for retrieving the available pragma parameter values.
     * @author leo
     */
    private static interface PragmaValue
    {
        public String getValue();
    }

    /**
     * Convert the given enum values to a string array
     * @param list Array if PragmaValue.
     * @return String array of Enum values
     */
    private static String[] toStringArray(PragmaValue[] list) {
        String[] result = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            result[i] = list[i].getValue();
        }
        return result;
    }

    public static enum Encoding implements PragmaValue {
        UTF8("'UTF-8'"),
        UTF16("'UTF-16'"),
        UTF16_LITTLE_ENDIAN("'UTF-16le'"),
        UTF16_BIG_ENDIAN("'UTF-16be'"),
        UTF_8(UTF8),                    // UTF-8
        UTF_16(UTF16),                  // UTF-16
        UTF_16LE(UTF16_LITTLE_ENDIAN),  // UTF-16le
        UTF_16BE(UTF16_BIG_ENDIAN);     // UTF-16be

        public final String typeName;

        private Encoding(String typeName) {
            this.typeName = typeName;
        }

        private Encoding(Encoding encoding) {
           this.typeName = encoding.getValue();
        }

        public String getValue() {
            return typeName;
        }

        public static Encoding getEncoding(String value) {
           return valueOf(value.replaceAll("-", "_").toUpperCase());
        }
    }

    public static enum JournalMode implements PragmaValue {
        DELETE, TRUNCATE, PERSIST, MEMORY, WAL, OFF;

        public String getValue() {
            return name();
        }
    }

    /**
     * Sets the text encoding used by the main database.
     * @param encoding One of {@link Encoding}
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_encoding">www.sqlite.org/pragma.html#pragma_encoding</a>
     */
    public void setEncoding(Encoding encoding) {
        setPragma(Pragma.ENCODING, encoding.typeName);
    }

    /**
     * Whether to enforce foreign key constraints. This setting affects the
     * execution of all statements prepared using the database connection,
     * including those prepared before the setting was changed.
     * @param enforce True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_foreign_keys">www.sqlite.org/pragma.html#pragma_foreign_keys</a>
     */
    public void enforceForeignKeys(boolean enforce) {
        set(Pragma.FOREIGN_KEYS, enforce);
    }

    /**
     * @deprecated
     * Enables or disables the full_column_name flag. This flag together with
     * the short_column_names flag determine the way SQLite assigns names to
     * result columns of SELECT statements.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_full_column_names">www.sqlite.org/pragma.html#pragma_full_column_names</a>
     */
    public void enableFullColumnNames(boolean enable) {
        set(Pragma.FULL_COLUMN_NAMES, enable);
    }

    /**
     * Enables or disables the fullfsync flag. This flag determines whether or
     * not the F_FULLFSYNC syncing method is used on systems that support it.
     * The default value of the fullfsync flag is off. Only Mac OS X supports
     * F_FULLFSYNC.
     * @param enable True to enable; false to disable.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_fullfsync">www.sqlite.org/pragma.html#pragma_fullfsync</a>
     */
    public void enableFullSync(boolean enable) {
        set(Pragma.FULL_SYNC, enable);
    }

    /**
     * Sets the incremental_vacuum value; the number of pages to be removed from
     * the <a href="http://www.sqlite.org/fileformat2.html#freelist">freelist</a>.
     * The database file is truncated by the same amount.
     * @param numberOfPagesToBeRemoved The number of pages to be removed.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_incremental_vacuum">www.sqlite.org/pragma.html#pragma_incremental_vacuum</a>
     */
    public void incrementalVacuum(int numberOfPagesToBeRemoved) {
        set(Pragma.INCREMENTAL_VACUUM, numberOfPagesToBeRemoved);
    }

    /**
     * Sets the journal mode for databases associated with the current database
     * connection.
     * @param mode One of {@link JournalMode}
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_journal_mode">www.sqlite.org/pragma.html#pragma_journal_mode</a>
     */
    public void setJournalMode(JournalMode mode) {
        setPragma(Pragma.JOURNAL_MODE, mode.name());
    }

    //    public void setJournalMode(String databaseName, JournalMode mode) {
    //        setPragma(databaseName, Pragma.JOURNAL_MODE, mode.name());
    //    }

    /**
     * Sets the journal_size_limit. This setting limits the size of the
     * rollback-journal and WAL files left in the file-system after transactions
     * or checkpoints.
     * @param limit Limit value in bytes. A negative number implies no limit.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_journal_size_limit">www.sqlite.org/pragma.html#pragma_journal_size_limit</a>
     */
    public void setJounalSizeLimit(int limit) {
        set(Pragma.JOURNAL_SIZE_LIMIT, limit);
    }

    /**
     * Sets the value of the legacy_file_format flag. When this flag is enabled,
     * new SQLite databases are created in a file format that is readable and
     * writable by all versions of SQLite going back to 3.0.0. When the flag is
     * off, new databases are created using the latest file format which might
     * not be readable or writable by versions of SQLite prior to 3.3.0.
     * @param use True to turn on legacy file format; false to turn off.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_legacy_file_format">www.sqlite.org/pragma.html#pragma_legacy_file_format</a>
     */
    public void useLegacyFileFormat(boolean use) {
        set(Pragma.LEGACY_FILE_FORMAT, use);
    }

    public static enum LockingMode implements PragmaValue {
        NORMAL, EXCLUSIVE;
        public String getValue() {
            return name();
        }
    }

    /**
     * Sets the database connection locking-mode.
     * @param mode One of {@link LockingMode}
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_locking_mode">www.sqlite.org/pragma.html#pragma_locking_mode</a>
     */
    public void setLockingMode(LockingMode mode) {
        setPragma(Pragma.LOCKING_MODE, mode.name());
    }

    //    public void setLockingMode(String databaseName, LockingMode mode) {
    //        setPragma(databaseName, Pragma.LOCKING_MODE, mode.name());
    //    }

    /**
     * Sets the page size of the database. The page size must be a power of two
     * between 512 and 65536 inclusive.
     * @param numBytes A power of two between 512 and 65536 inclusive.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_page_size">www.sqlite.org/pragma.html#pragma_page_size</a>
     */
    public void setPageSize(int numBytes) {
        set(Pragma.PAGE_SIZE, numBytes);
    }

    /**
     * Sets the maximum number of pages in the database file.
     * @param numPages Number of pages.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_max_page_count">www.sqlite.org/pragma.html#pragma_max_page_count</a>
     */
    public void setMaxPageCount(int numPages) {
        set(Pragma.MAX_PAGE_COUNT, numPages);
    }

    /**
     * Enables or disables useReadUncommitedIsolationMode.
     * @param useReadUncommitedIsolationMode True to turn on; false to disable.
     * disabled otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_read_uncommitted">www.sqlite.org/pragma.html#pragma_read_uncommitted</a>
     */
    public void setReadUncommited(boolean useReadUncommitedIsolationMode) {
        set(Pragma.READ_UNCOMMITED, useReadUncommitedIsolationMode);
    }

    /**
     * Enables or disables the recursive trigger capability.
     * @param enable True to enable the recursive trigger capability.
     * @see <a href="www.sqlite.org/pragma.html#pragma_recursive_triggers">www.sqlite.org/pragma.html#pragma_recursive_triggers</a>
     */
    public void enableRecursiveTriggers(boolean enable) {
        set(Pragma.RECURSIVE_TRIGGERS, enable);
    }

    /**
     * Enables or disables the reverse_unordered_selects flag. This setting
     * causes SELECT statements without an ORDER BY clause to emit their results
     * in the reverse order of what they normally would. This can help debug
     * applications that are making invalid assumptions about the result order.
     * @param enable True to enable reverse_unordered_selects.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_reverse_unordered_selects">www.sqlite.org/pragma.html#pragma_reverse_unordered_selects</a>
     */
    public void enableReverseUnorderedSelects(boolean enable) {
        set(Pragma.REVERSE_UNORDERED_SELECTS, enable);
    }

    /**
     * Enables or disables the short_column_names flag. This flag affects the
     * way SQLite names columns of data returned by SELECT statements.
     * @param enable True to enable short_column_names.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_short_column_names">www.sqlite.org/pragma.html#pragma_short_column_names</a>
     */
    public void enableShortColumnNames(boolean enable) {
        set(Pragma.SHORT_COLUMN_NAMES, enable);
    }

    public static enum SynchronousMode implements PragmaValue {
        OFF, NORMAL, FULL;

        public String getValue() {
            return name();
        }
    }

    /**
     * Changes the setting of the "synchronous" flag.
     * @param mode One of {@link SynchronousMode}:<ul>
     * <li> OFF - SQLite continues without syncing as soon as it has handed
     * data off to the operating system</li>
     * <li> NORMAL - the SQLite database engine will still sync at the most
     * critical moments, but less often than in FULL mode</li>
     * <li> FULL - the SQLite database engine will use the xSync method of the
     * VFS to ensure that all content is safely written to the disk surface
     * prior to continuing. This ensures that an operating system crash or power
     * failure will not corrupt the database.</li></ul>
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_synchronous">www.sqlite.org/pragma.html#pragma_synchronous</a>
     */
    public void setSynchronous(SynchronousMode mode) {
        setPragma(Pragma.SYNCHRONOUS, mode.name());
    }

    public static enum TempStore implements PragmaValue {
        DEFAULT, FILE, MEMORY;

        public String getValue() {
            return name();
        }

    }

    /**
     * Changes the setting of the "temp_store" parameter.
     * @param storeType One of {@link TempStore}:<ul>
     * <li> DEFAULT - the compile-time C preprocessor macro SQLITE_TEMP_STORE
     * is used to determine where temporary tables and indices are stored</li>
     * <li>FILE - temporary tables and indices are kept in as if they were pure
     * in-memory databases memory</li>
     * <li>MEMORY - temporary tables and indices are stored in a file.</li></ul>
     * @see <a
     *      href="http://www.sqlite.org/pragma.html#pragma_temp_store">www.sqlite.org/pragma.html#pragma_temp_store</a>
     */
    public void setTempStore(TempStore storeType) {
        setPragma(Pragma.TEMP_STORE, storeType.name());
    }

    /**
     * Changes the value of the sqlite3_temp_directory global variable, which many operating-system
     * interface backends use to determine where to store temporary tables and indices.
     * @param directoryName Directory name for storing temporary tables and indices.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_temp_store_directory">www.sqlite.org/pragma.html#pragma_temp_store_directory</a>
     */
    public void setTempStoreDirectory(String directoryName) {
        setPragma(Pragma.TEMP_STORE_DIRECTORY, String.format("'%s'", directoryName));
    }

    /**
     * Set the value of the user-version. The user-version is not used
     * internally by SQLite. It may be used by applications for any purpose. The
     * value is stored in the database header at offset 60.
     * @param version A big-endian 32-bit signed integer.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_user_version">www.sqlite.org/pragma.html#pragma_user_version</a>
     */
    public void setUserVersion(int version) {
        set(Pragma.USER_VERSION, version);
    }

    public static enum TransactionMode implements PragmaValue {
        DEFFERED, IMMEDIATE, EXCLUSIVE;

        public String getValue() {
            return name();
        }

        public static TransactionMode getMode(String mode) {
            return TransactionMode.valueOf(mode.toUpperCase());
        }
    }

    /**
     * Sets the mode that will be used to start transactions.
     * @param transactionMode One of {@link TransactionMode}.
     * @see <a href="http://www.sqlite.org/lang_transaction.html">http://www.sqlite.org/lang_transaction.html</a>
     */
    public void setTransactionMode(TransactionMode transactionMode) {
        this.transactionMode = transactionMode;
    }

    /**
     * Sets the mode that will be used to start transactions.
     * @param transactionMode One of DEFFERED, IMMEDIATE or EXCLUSIVE.
     * @see <a href="http://www.sqlite.org/lang_transaction.html">http://www.sqlite.org/lang_transaction.html</a>
     */
    public void setTransactionMode(String transactionMode) {
        setTransactionMode(TransactionMode.getMode(transactionMode));
    }

    /**
     * @return The transaction mode.
     */
    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    public static enum DatePrecision implements PragmaValue {
        SECONDS, MILLISECONDS;

        public String getValue() {
            return name();
        }

        public static DatePrecision getPrecision(String precision) {
            return DatePrecision.valueOf(precision.toUpperCase());
        }
    }
 
    /**
     * @param datePrecision One of SECONDS or MILLISECONDS
     * @throws SQLException 
     */
    public void setDatePrecision(String datePrecision) throws SQLException {
        this.datePrecision = DatePrecision.getPrecision(datePrecision);
    }

    public static enum DateClass implements PragmaValue {
        INTEGER, TEXT, REAL;

        public String getValue() {
            return name();
        }

        public static DateClass getDateClass(String dateClass) {
            return DateClass.valueOf(dateClass.toUpperCase());
        }
    }

    /**
     * @param dateClass One of INTEGER, TEXT or REAL
     */
    public void setDateClass(String dateClass) {
        this.dateClass = DateClass.getDateClass(dateClass);
    }

    /**
     * @param dateStringFormat Format of date string
     */
    public void setDateStringFormat(String dateStringFormat) {
        this.dateStringFormat = dateStringFormat;
    }

    /**
     * @param milliseconds Connect to DB timeout in milliseconds
     */
    public void setBusyTimeout(String milliseconds) {
        setPragma(Pragma.BUSY_TIMEOUT, milliseconds);
    }
}
