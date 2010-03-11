/*--------------------------------------------------------------------------
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
    private int              openModeFlag = 0x00;

    public SQLiteConfig() {
        this(new Properties());
    }

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
    }

    /**
     * Create a new JDBC connection using the current configuration
     * 
     * @return
     * @throws SQLException
     */
    public Connection createConnection(String url) throws SQLException {
        return JDBC.createConnection(url, toProperties());
    }

    /**
     * Apply the current configuration to connection
     * 
     * @param conn
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

        Statement stat = conn.createStatement();
        try {
            int count = 0;
            for (Object each : pragmaTable.keySet()) {
                String key = each.toString();
                if (!pragmaParams.contains(key))
                    continue;

                String value = pragmaTable.getProperty(key);
                if (value != null) {
                    String sql = String.format("pragma %s=%s", key, value);
                    stat.addBatch(sql);
                    count++;
                }
            }
            if (count > 0)
                stat.executeBatch();
        }
        finally {
            if (stat != null)
                stat.close();
        }

    }

    private void set(Pragma pragma, boolean flag) {
        setPragma(pragma, Boolean.toString(flag));
    }

    private void set(Pragma pragma, int num) {
        setPragma(pragma, Integer.toString(num));
    }

    private boolean getBoolean(Pragma pragma, String defaultValue) {
        return Boolean.parseBoolean(pragmaTable.getProperty(pragma.pragmaName, defaultValue));
    }

    public boolean isEnabledSharedCache() {
        return getBoolean(Pragma.SHARED_CACHE, "false");
    }

    public boolean isEnabledLoadExtension() {
        return getBoolean(Pragma.LOAD_EXTENSION, "false");
    }

    public int getOpenModeFlags() {
        return openModeFlag;
    }

    /**
     * Set a pragma value. To take effect the pragma settings,
     * 
     * @param pragma
     * @param value
     */
    public void setPragma(Pragma pragma, String value) {
        pragmaTable.put(pragma.pragmaName, value);
    }

    /**
     * Convert this SQLiteConfig settings into a Properties object, that can be
     * passed to the {@link DriverManager#getConnection(String, Properties)}.
     * 
     * @return properties representation of this configuration
     */
    public Properties toProperties() {
        pragmaTable.setProperty(Pragma.OPEN_MODE.pragmaName, Integer.toString(openModeFlag));

        return pragmaTable;
    }

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

    private static enum Pragma {

        // Parameters requiring SQLite3 API invocation
        OPEN_MODE("open_mode", "Database open-mode flag", null),
        SHARED_CACHE("shared_cache", "Enablse SQLite Shared-Cache mode, native driver only", OnOff),
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
        USER_VERSION("user_version");

        public final String   pragmaName;
        public final String[] choices;
        public final String   description;

        private Pragma(String pragmaName) {
            this(pragmaName, null);
        }

        private Pragma(String pragmaName, String[] choices) {
            this(pragmaName, null, null);
        }

        private Pragma(String pragmaName, String description, String[] choices) {
            this.pragmaName = pragmaName;
            this.description = description;
            this.choices = choices;
        }
    }

    /**
     * Set the database open mode
     * 
     * @param mode
     */
    public void setOpenMode(SQLiteOpenMode mode) {
        openModeFlag |= mode.flag;
    }

    /**
     * Reset the specified database open mode flag
     * 
     * @param mode
     */
    public void resetOpenMode(SQLiteOpenMode mode) {
        openModeFlag &= ~mode.flag;
    }

    public void setSharedCache(boolean enable) {
        set(Pragma.SHARED_CACHE, enable);
    }

    public void enableLoadExtension(boolean enable) {
        set(Pragma.LOAD_EXTENSION, enable);
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            setOpenMode(SQLiteOpenMode.READONLY);
            resetOpenMode(SQLiteOpenMode.READWRITE);
        }
        else {
            setOpenMode(SQLiteOpenMode.READWRITE);
            resetOpenMode(SQLiteOpenMode.READONLY);
        }
    }

    public void setCacheSize(int numberOfPages) {
        set(Pragma.CACHE_SIZE, numberOfPages);
    }

    public void enableCaseSensitiveLike(boolean enable) {
        set(Pragma.CASE_SENSITIVE_LIKE, enable);
    }

    public void enableCountChanges(boolean enable) {
        set(Pragma.COUNT_CHANGES, enable);
    }

    /**
     * Set the cache size persistently across database connections
     * 
     * @param numberOfPages
     */
    public void setDefaultCacheSize(int numberOfPages) {
        set(Pragma.DEFAULT_CACHE_SIZE, numberOfPages);
    }

    public void enableEmptyResultCallBacks(boolean enable) {
        set(Pragma.EMPTY_RESULT_CALLBACKS, enable);
    }

    /**
     * The common interface for retrieving the available pragma parameter
     * values.
     * 
     * @author leo
     * 
     */
    private static interface PragmaValue
    {
        public String getValue();
    }

    /**
     * Convert the given Enum values to a string array
     * 
     * @param list
     * @return
     */
    private static String[] toStringArray(PragmaValue[] list) {
        String[] result = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            result[i] = list[i].getValue();
        }
        return result;
    }

    public static enum Encoding implements PragmaValue {
        UTF8("UTF-8"), UTF16("UTF-16"), UTF16_LITTLE_ENDIAN("UTF-16le"), UTF16_BIG_ENDIAN("UTF-16be");
        public final String typeName;

        private Encoding(String typeName) {
            this.typeName = typeName;
        }

        public String getValue() {
            return typeName;
        }
    }

    public static enum JournalMode implements PragmaValue {
        DELETE, TRUNCATE, PERSIST, MEMORY, OFF;

        public String getValue() {
            return name();
        }
    }

    public void setEncoding(Encoding encoding) {
        setPragma(Pragma.ENCODING, encoding.typeName);
    }

    public void enforceForeignKeys(boolean enforce) {
        set(Pragma.FOREIGN_KEYS, enforce);
    }

    public void enableFullColumnNames(boolean enable) {
        set(Pragma.FULL_COLUMN_NAMES, enable);
    }

    public void enableFullSync(boolean enable) {
        set(Pragma.FULL_SYNC, enable);
    }

    public void incrementalVacuum(int numberOfPagesToBeRemoved) {
        set(Pragma.INCREMENTAL_VACUUM, numberOfPagesToBeRemoved);
    }

    public void setJournalMode(JournalMode mode) {
        setPragma(Pragma.JOURNAL_MODE, mode.name());
    }

    //    public void setJournalMode(String databaseName, JournalMode mode) {
    //        setPragma(databaseName, Pragma.JOURNAL_MODE, mode.name());
    //    }

    public void setJounalSizeLimit(int limit) {
        set(Pragma.JOURNAL_SIZE_LIMIT, limit);
    }

    public void useLegacyFileFormat(boolean use) {
        set(Pragma.LEGACY_FILE_FORMAT, use);
    }

    public static enum LockingMode implements PragmaValue {
        NORMAL, EXCLUSIVE;
        public String getValue() {
            return name();
        }
    }

    public void setLockingMode(LockingMode mode) {
        setPragma(Pragma.LOCKING_MODE, mode.name());
    }

    //    public void setLockingMode(String databaseName, LockingMode mode) {
    //        setPragma(databaseName, Pragma.LOCKING_MODE, mode.name());
    //    }

    public void setPageSize(int numBytes) {
        set(Pragma.PAGE_SIZE, numBytes);
    }

    public void setMaxPageCount(int numPages) {
        set(Pragma.MAX_PAGE_COUNT, numPages);
    }

    public void setReadUncommited(boolean useReadUncommitedIsolationMode) {
        set(Pragma.READ_UNCOMMITED, useReadUncommitedIsolationMode);
    }

    public void enableRecursiveTriggers(boolean enable) {
        set(Pragma.RECURSIVE_TRIGGERS, enable);
    }

    public void enableReverseUnorderedSelects(boolean enable) {
        set(Pragma.REVERSE_UNORDERED_SELECTS, enable);
    }

    public void enableShortColumnNames(boolean enable) {
        set(Pragma.SHORT_COLUMN_NAMES, enable);
    }

    public static enum SynchronousMode implements PragmaValue {
        OFF, NORMAL, FULL;

        public String getValue() {
            return name();
        }
    }

    public void setSynchronous(SynchronousMode mode) {
        setPragma(Pragma.SYNCHRONOUS, mode.name());
    }

    public static enum TempStore implements PragmaValue {
        DEFAULT, FILE, MEMORY;

        public String getValue() {
            return name();
        }

    }

    public void setTempStore(TempStore storeType) {
        setPragma(Pragma.TEMP_STORE, storeType.name());
    }

    public void setTempStoreDirectory(String directoryName) {
        setPragma(Pragma.TEMP_STORE_DIRECTORY, String.format("'%s'", directoryName));
    }

    public void setUserVersion(int version) {
        set(Pragma.USER_VERSION, version);
    }

}
