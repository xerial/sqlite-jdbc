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

import java.util.HashMap;

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
    private HashMap<String, String> pragmaName_statement = new HashMap<String, String>();

    private void set(Pragma pragma, boolean flag) {
        set(pragma, Boolean.toString(flag));
    }

    private void set(Pragma pragma, int num) {
        set(pragma, Integer.toString(num));
    }

    private void set(Pragma pragma, String value) {
        pragmaName_statement.put(pragma.pragmaName, String.format("pragma %s = %s", pragma.pragmaName, value));
    }

    private void set(String databaseName, Pragma pragma, String value) {
        pragmaName_statement.put(pragma.pragmaName, String.format("pragma %s.%s = %s", databaseName, pragma.pragmaName,
                value));
    }

    private static final String[] OnOff = new String[] { "true", "false" };

    public static enum Pragma {

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
        public final String[] chioices;

        private Pragma(String pragmaName) {
            this(pragmaName, null);
        }

        private Pragma(String pragmaName, String[] choices) {
            this.pragmaName = pragmaName;
            this.chioices = choices;
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
     * Common interface to retrieve available pragma parameter values.
     * 
     * @author leo
     * 
     */
    private static interface PragmaValue
    {
        public String getValue();
    }

    /**
     * Convert enum values to string arrays
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
        set(Pragma.ENCODING, encoding.typeName);
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
        set(Pragma.JOURNAL_MODE, mode.name());
    }

    public void setJournalMode(String databaseName, JournalMode mode) {
        set(databaseName, Pragma.JOURNAL_MODE, mode.name());
    }

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
        set(Pragma.LOCKING_MODE, mode.name());
    }

    public void setLockingMode(String databaseName, LockingMode mode) {
        set(databaseName, Pragma.LOCKING_MODE, mode.name());
    }

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
        set(Pragma.SYNCHRONOUS, mode.name());
    }

    public static enum TempStore implements PragmaValue {
        DEFAULT, FILE, MEMORY;

        public String getValue() {
            return name();
        }

    }

    public void setTempStore(TempStore storeType) {
        set(Pragma.TEMP_STORE, storeType.name());
    }

    public void setTempStoreDirectory(String directoryName) {
        set(Pragma.TEMP_STORE_DIRECTORY, String.format("'%s'", directoryName));
    }

    public void setUserVersion(int version) {
        set(Pragma.USER_VERSION, version);
    }

}
