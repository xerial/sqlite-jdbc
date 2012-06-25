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
    * @return Database Connection object .
    * @throws SQLException
    */
   public Connection createConnection(String url) throws SQLException {
       return JDBC.createConnection(url, toProperties());
   }

   /**
    * Apply the current configuration to connection
    * @param conn Database Connection object .
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

   /**
    * Sets a pragma to the given boolean value.
    * @param pragma Pragma object.
    * @param flag The boolean value to be set for the pragma.
    */
   private void set(Pragma pragma, boolean flag) {
      setPragma(pragma, Boolean.toString(flag));
   }

   /**
    * Sets a pragma to the given int value.
    * @param pragma Pragma object.
    * @param num The int value to be set for the pragma.
    */
   private void set(Pragma pragma, int num) {
      setPragma(pragma, Integer.toString(num));
   }

   /**
    * Checks if the given value is the default value of the given Pragma object.
    * @param pragma  Pragma object.
    * @param defaultValue The given value
    * @return True if the given value is the default value of the given Pragma object; false otherwise.
    */
   private boolean getBoolean(Pragma pragma, String defaultValue) {
      return Boolean.parseBoolean(pragmaTable.getProperty(pragma.pragmaName, defaultValue));
   }

   /**
    * Checks if the share cache of SQLite database is enabled. 
    * @return True if the share cache of SQLite database is ebabled; false otherwise.
    */
   public boolean isEnabledSharedCache() {
      return getBoolean(Pragma.SHARED_CACHE, "false");
   }

   /**
    * Checks if the Load Extension of SQLite database is enabled. 
    * @return  True if the Load Extension of SQLite database is ebabled; false otherwise.
    */
   public boolean isEnabledLoadExtension() {
      return getBoolean(Pragma.LOAD_EXTENSION, "false");
   }

   /**
    * @return
    */
   public int getOpenModeFlags() {
      return openModeFlag;
   }

   /**
    * Sets a pragma value. To take effect the pragma settings,
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
    * Sets the database open mode
    * @param mode <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">Flags for file open operations.</a>
    */
   public void setOpenMode(SQLiteOpenMode mode) {
       openModeFlag |= mode.flag;
   }

   /**
    * ReSets the specified database open mode flag
    * @param mode <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">Flags for file open operations.</a>
    */
   public void resetOpenMode(SQLiteOpenMode mode) {
       openModeFlag &= ~mode.flag;
   }

   /**
     * Enables or disables the sharing of the database cache and schema data structures between
     * connections to the same database.
    * @param enable Sharing is enabled if the argument is true and disabled if the argument is false.
     * @see <a href="http://www.sqlite.org/c3ref/enable_shared_cache.html">www.sqlite.org/c3ref/enable_shared_cache.html</a>
    */
   public void setSharedCache(boolean enable) {
       set(Pragma.SHARED_CACHE, enable);
   }

   /**
    * Enables or disables extension loading.
    * @param enable Extension loading is enabled if the argument is true and disabled if the argument is false.
     * @see <a href="http://www.sqlite.org/c3ref/load_extension.html">www.sqlite.org/c3ref/load_extension.html</a>
    */
   public void enableLoadExtension(boolean enable) {
       set(Pragma.LOAD_EXTENSION, enable);
   }

   /**
    * Sets the specified database to the open mode read-only; otherwise Sets to the open mode of read-write .
    * @param readOnly Sets the specified database to read-only open mode if the argument is true; Sets to read-write open 
    * mode otherwise.
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
     * Changes the suggested maximum number of database disk pages that SQLite will hold in memory at once per open database file using PRAGMA statement.
    * @param numberOfPages Cache size in number of pages.
     * @See <a href="http://www.sqlite.org/pragma.html#pragma_cache_size">www.sqlite.org/pragma.html#pragma_cache_size</a>
    */
   public void setCacheSize(int numberOfPages) {
       set(Pragma.CACHE_SIZE, numberOfPages);
   }

   /**
    * Enables or disables case sensitive for the LIKE operator using SQLite case_sensitive_like pragma.
    * @param enable Case sensitive feature for LIKE operator is enable if the argument is true; disable otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_case_sensitive_like">www.sqlite.org/pragma.html#pragma_case_sensitive_like</a>
    */
   public void enableCaseSensitiveLike(boolean enable) {
       set(Pragma.CASE_SENSITIVE_LIKE, enable);
   }

   /**
    * Enables or disables the count-changes flag. when the count-changes flag is disabled, INSERT, UPDATE and DELETE 
    * statements return no data. When count-changes is enabled, each of these commands returns a single row of data 
    * consisting of one integer value - the number of rows inserted, modified or deleted by the command.
    * @param enable The count-changes flag is enabled if the argument is true and disabled if the argument is false.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_count_changes">www.sqlite.org/pragma.html#pragma_count_changes</a>
    */
   public void enableCountChanges(boolean enable) {
       set(Pragma.COUNT_CHANGES, enable);
   }

   /**
    * Sets the suggested maximum number of database disk pages that SQLite will hold in memory at once per open 
    * database file using PRAGMA statement, and the cache size set here persists across database connections..
    * @param numberOfPages Cache size in number of pages.
     * @See <a href="http://www.sqlite.org/pragma.html#pragma_cache_size">www.sqlite.org/pragma.html#pragma_cache_size</a>
    */
   public void setDefaultCacheSize(int numberOfPages) {
       set(Pragma.DEFAULT_CACHE_SIZE, numberOfPages);
   }

   /**
    * Enables or disables the empty_result_callbacks flag using pragma statement. When empty-result-callbacks is enabled, 
    * the callback function is invoked exactly once, with the third parameter set to 0 (NULL). when the 
    * empty-result-callbacks flag is disabled, the callback function supplied to the sqlite3_exec() is not invoked for 
    * commands that return zero rows of data.
    * @param enable The empty_result_callbacks flag is enabled if the argument is true and disabled if the argument is 
    * false.
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
    * Convert the given Enum values to a string array
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

   /**
    * Sets the text encoding used by the main database through pragma statement.
    * @param encoding One of "UTF-8", "UTF-16le" (little-endian UTF-16 encoding) or "UTF-16be" (big-endian UTF-16 encoding).
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_encoding">www.sqlite.org/pragma.html#pragma_encoding</a>
    */
   public void setEncoding(Encoding encoding) {
       setPragma(Pragma.ENCODING, encoding.typeName);
   }

   /**
    * Enforces the foreign key constraints using pragma statement. This setting affects the execution of all statements 
    * prepared using the database connection, including those prepared before the setting was changed.  
    * @param enforce The foreign key constraints is enforced if the argument is true; cleared otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_foreign_keys">www.sqlite.org/pragma.html#pragma_foreign_keys</a>
    */
   public void enforceForeignKeys(boolean enforce) {
       set(Pragma.FOREIGN_KEYS, enforce);
   }

   /**
    * Enables or disables the full_column_name flag using pragma statement. This flag together with the short_column_names 
    * flag determine the way SQLite assigns names to result columns of SELECT statements.
    * @param enable The efull_column_name flag is enabled if the argument is true and disabled if the argument is false.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_full_column_names">www.sqlite.org/pragma.html#pragma_full_column_names</a>
    */
   public void enableFullColumnNames(boolean enable) {
       set(Pragma.FULL_COLUMN_NAMES, enable);
   }

   /**
    * Enables or disables the fullfsync flag using pragma statement. This flag determines whether or not the F_FULLFSYNC 
    * syncing method is used on systems that support it. The default value of the fullfsync flag is off. Only Mac OS X 
    * supports F_FULLFSYNC.
    * @param enable The fullfsync flag is enabled if the argument is true; disabled otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_fullfsync">www.sqlite.org/pragma.html#pragma_fullfsync</a>
    */
   public void enableFullSync(boolean enable) {
       set(Pragma.FULL_SYNC, enable);
   }

   /**
    * Sets the incremental_vacuum value using pragma statement. This setting causes up to the set number of pages to be 
    * removed from the <a href="http://www.sqlite.org/fileformat2.html#freelist">freelist</a>. The database file is 
    * truncated by the same amount.
    * @param numberOfPagesToBeRemoved The umber of pages to be removed.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_incremental_vacuum">www.sqlite.org/pragma.html#pragma_incremental_vacuum</a>
    */
   public void incrementalVacuum(int numberOfPagesToBeRemoved) {
       set(Pragma.INCREMENTAL_VACUUM, numberOfPagesToBeRemoved);
   }

   /**
    * Sets the journal mode for databases associated with the current database connection. using pragma statement.
     * @param mode One of DELETE, TRUNCATE, PERSIST, MEMORY, WAL or OFF
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_journal_mode">www.sqlite.org/pragma.html#pragma_journal_mode</a>
    */
   public void setJournalMode(JournalMode mode) {
       setPragma(Pragma.JOURNAL_MODE, mode.name());
   }

   //    public void setJournalMode(String databaseName, JournalMode mode) {
   //        setPragma(databaseName, Pragma.JOURNAL_MODE, mode.name());
   //    }

   /**
    * Sets the journal_size_limit using pragma statement. This setting limits the size of rollback-journal and WAL files 
    * left in the file-system after transactions or checkpoints.
    * @param limit Limit value in bytes for the specified database. A negative number implies no limit.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_journal_size_limit">www.sqlite.org/pragma.html#pragma_journal_size_limit</a>
    */
   public void setJounalSizeLimit(int limit) {
       set(Pragma.JOURNAL_SIZE_LIMIT, limit);
   }

   /**
    * Sets the value of the legacy_file_format flag using pragma statement. When this flag is on, new SQLite databases 
    * are created in a file format that is readable and writable by all versions of SQLite going back to 3.0.0. When the 
    * flag is off, new databases are created using the latest file format which might not be readable or writable by 
    * versions of SQLite prior to 3.3.0.
    * @param use The legacy_file_format flag is on if the argument is true; off otherwise.
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
    * Sets the database connection locking-mode using pragma statement. The locking-mode is either NORMAL or EXCLUSIVE. 
    * @param mode Either NORMAL or EXCLUSIVE.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_locking_mode">www.sqlite.org/pragma.html#pragma_locking_mode</a>
    */
   public void setLockingMode(LockingMode mode) {
       setPragma(Pragma.LOCKING_MODE, mode.name());
   }

   //    public void setLockingMode(String databaseName, LockingMode mode) {
   //        setPragma(databaseName, Pragma.LOCKING_MODE, mode.name());
   //    }

   /**
    * Sets the page size of the database using pragma statement. The page size must be a power of two between 512 and 
    * 65536 inclusive. 
    * @param numBytes A power of two between 512 and 65536 inclusive. 
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_page_size">www.sqlite.org/pragma.html#pragma_page_size</a>
    */
   public void setPageSize(int numBytes) {
       set(Pragma.PAGE_SIZE, numBytes);
   }

   /**
    * Sets the maximum number of pages in the database file using pragma statement.
    * @param numPages Number of pages.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_max_page_count">www.sqlite.org/pragma.html#pragma_max_page_count</a>
    */
   public void setMaxPageCount(int numPages) {
       set(Pragma.MAX_PAGE_COUNT, numPages);
   }

   /**
    * Enables or disables useReadUncommitedIsolationMode using pragma statement.
    * @param useReadUncommitedIsolationMode The useReadUncommitedIsolationMode is enabled if the argument is true; 
    * disabled otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_read_uncommitted">www.sqlite.org/pragma.html#pragma_read_uncommitted</a>
    */
   public void setReadUncommited(boolean useReadUncommitedIsolationMode) {
       set(Pragma.READ_UNCOMMITED, useReadUncommitedIsolationMode);
   }

   /**
    * Enables or disables the recursive trigger capability using pragma statement.
    * @param enable The recursive trigger capability is enabled if the argument is true; disabled otherwise.
    * @see <a href="www.sqlite.org/pragma.html#pragma_recursive_triggers">www.sqlite.org/pragma.html#pragma_recursive_triggers</a>
    */
   public void enableRecursiveTriggers(boolean enable) {
       set(Pragma.RECURSIVE_TRIGGERS, enable);
   }

   /**
    * Enables or disables the reverse_unordered_selects flag using pragma statement. This setting causes SELECT 
    * statements without an ORDER BY clause to emit their results in the reverse order of what they normally would. 
    * This can help debug applications that are making invalid assumptions about the result order.
    * @param enable The reverse_unordered_selects is enabled if the argument is true; disabled otherwise.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_reverse_unordered_selects">www.sqlite.org/pragma.html#pragma_reverse_unordered_selects</a>
    */
   public void enableReverseUnorderedSelects(boolean enable) {
       set(Pragma.REVERSE_UNORDERED_SELECTS, enable);
   }

   /**
    * Enables or disables the short_column_names flag using pragma statement. This flag affects the way SQLite names 
    * columns of data returned by SELECT statements.
    * @param enable The short_column_names is enabled if the argument is true; disabled otherwise.
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
    * Changes the setting of the "synchronous" flag using pragma statement. 
    * @param mode Value for enum SynchronousMode::<br/> OFF - SQLite continues without syncing as soon as it has handed 
    * data off to the operating system, <br/>NORMAL - the SQLite database engine will still sync at the most critical 
    * moments, but less often than in FULL mode,<br/>FULL - the SQLite database engine will use the xSync method of the 
    * VFS to ensure that all content is safely written to the disk surface prior to continuing. This ensures that an 
    * operating system crash or power failure will not corrupt the database.
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
    * Changes the setting of the "temp_store" parameter using pragma statement. 
    * @param storeType Value for enum TempStore:<br/> DEFAULT - the compile-time C preprocessor macro SQLITE_TEMP_STORE 
    * is used to determine where temporary tables and indices are stored, <br/>FILE - temporary tables and indices are 
    * kept in as if they were pure in-memory databases memory,<br/>MEMORY - temporary tables and indices are stored in 
    * a file.
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
    * Set the value of the user-version using pragma statement. The user-version is not used internally by SQLite. It 
    * may be used by applications for any purpose.
    * @param version The big-endian 32-bit signed integers stored in the database header at offsets 60.
     * @see <a href="http://www.sqlite.org/pragma.html#pragma_user_version">www.sqlite.org/pragma.html#pragma_user_version</a>
    */
   public void setUserVersion(int version) {
       set(Pragma.USER_VERSION, version);
   }

}
