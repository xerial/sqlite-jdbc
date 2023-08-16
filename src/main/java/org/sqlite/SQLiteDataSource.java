/*--------------------------------------------------------------------------
 *  Copyright 2010 Taro L. Saito
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
// SQLiteDataSource.java
// Since: Mar 11, 2010
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.sqlite.SQLiteConfig.*;

/**
 * Provides {@link DataSource} API for configuring SQLite database connection
 *
 * @author leo
 */
public class SQLiteDataSource implements DataSource {
    private SQLiteConfig config;
    private transient PrintWriter logger;
    private int loginTimeout = 1;

    private String url = JDBC.PREFIX; // use memory database in default
    private String databaseName = ""; // the name of the current database

    /** Default constructor. */
    public SQLiteDataSource() {
        this.config = new SQLiteConfig(); // default configuration
    }

    /**
     * Creates a data source based on the provided configuration.
     *
     * @param config The configuration for the data source.
     */
    public SQLiteDataSource(SQLiteConfig config) {
        this.config = config;
    }

    /**
     * Sets a data source's configuration.
     *
     * @param config The configuration.
     */
    public void setConfig(SQLiteConfig config) {
        this.config = config;
    }

    /** @return The configuration for the data source. */
    public SQLiteConfig getConfig() {
        return config;
    }

    /**
     * Sets the location of the database file.
     *
     * @param url The location of the database file.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /** @return The location of the database file. */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the database name.
     *
     * @param databaseName The name of the database
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return The name of the database if one was set.
     * @see SQLiteDataSource#setDatabaseName(String)
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Enables or disables the sharing of the database cache and schema data structures between
     * connections to the same database.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/c3ref/enable_shared_cache.html">https://www.sqlite.org/c3ref/enable_shared_cache.html</a>
     */
    public void setSharedCache(boolean enable) {
        config.setSharedCache(enable);
    }

    /**
     * Enables or disables extension loading.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/c3ref/load_extension.html">https://www.sqlite.org/c3ref/load_extension.html</a>
     */
    public void setLoadExtension(boolean enable) {
        config.enableLoadExtension(enable);
    }

    /**
     * Sets the database to be opened in read-only mode
     *
     * @param readOnly True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/c3ref/c_open_autoproxy.html">https://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    public void setReadOnly(boolean readOnly) {
        config.setReadOnly(readOnly);
    }

    /**
     * Sets the amount of time that the connection's busy handler will wait when a table is locked.
     *
     * @param milliseconds The number of milliseconds to wait.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_busy_timeout">https://www.sqlite.org/pragma.html#pragma_busy_timeout</a>
     */
    public void setBusyTimeout(int milliseconds) {
        config.setBusyTimeout(milliseconds);
    }

    /**
     * Sets the suggested maximum number of database disk pages that SQLite will hold in memory at
     * once per open database file.
     *
     * @param numberOfPages The number of database disk pages.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_cache_size">https://www.sqlite.org/pragma.html#pragma_cache_size</a>
     */
    public void setCacheSize(int numberOfPages) {
        config.setCacheSize(numberOfPages);
    }

    /**
     * Enables or disables case sensitivity for the built-in LIKE operator.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/compile.html#case_sensitive_like">https://www.sqlite.org/compile.html#case_sensitive_like</a>
     */
    public void setCaseSensitiveLike(boolean enable) {
        config.enableCaseSensitiveLike(enable);
    }

    /**
     * Enables or disables the count-changes flag. When enabled INSERT, UPDATE and DELETE statements
     * return the number of rows they modified.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_count_changes">https://www.sqlite.org/pragma.html#pragma_count_changes</a>
     */
    public void setCountChanges(boolean enable) {
        config.enableCountChanges(enable);
    }

    /**
     * Sets the default maximum number of database disk pages that SQLite will hold in memory at
     * once per open database file.
     *
     * @param numberOfPages The default suggested cache size.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_cache_size">https://www.sqlite.org/pragma.html#pragma_cache_size</a>
     */
    public void setDefaultCacheSize(int numberOfPages) {
        config.setDefaultCacheSize(numberOfPages);
    }

    /**
     * Sets the text encoding used by the main database.
     *
     * @param encoding One of "UTF-8", "UTF-16le" (little-endian UTF-16) or "UTF-16be" (big-endian
     *     UTF-16).
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_encoding">
     *     https://www.sqlite.org/pragma.html#pragma_encoding</a>
     */
    public void setEncoding(String encoding) {
        config.setEncoding(Encoding.getEncoding(encoding));
    }

    /**
     * Enables or disables the enforcement of foreign key constraints.
     *
     * @param enforce True to enable; false to disable.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_foreign_keys">
     *     https://www.sqlite.org/pragma.html#pragma_foreign_keys</a>
     */
    public void setEnforceForeignKeys(boolean enforce) {
        config.enforceForeignKeys(enforce);
    }

    /**
     * Enables or disables the full_column_names flag. This flag together with the
     * short_column_names flag determine the way SQLite assigns names to result columns of SELECT
     * statements.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_full_column_names">https://www.sqlite.org/pragma.html#pragma_full_column_names</a>
     */
    public void setFullColumnNames(boolean enable) {
        config.enableFullColumnNames(enable);
    }

    /**
     * Enables or disables the fullfsync flag. This flag determines whether or not the F_FULLFSYNC
     * syncing method is used on systems that support it.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_fullfsync">https://www.sqlite.org/pragma.html#pragma_fullfsync</a>
     */
    public void setFullSync(boolean enable) {
        config.enableFullSync(enable);
    }

    /**
     * Set the incremental_vacuum value that causes up to N pages to be removed from the <a
     * href="https://www.sqlite.org/fileformat2.html#freelist">https://www.sqlite.org/fileformat2.html#freelist</a>.
     *
     * @param numberOfPagesToBeRemoved
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_incremental_vacuum">
     *     https://www.sqlite.org/pragma.html#pragma_incremental_vacuum</a>
     */
    public void setIncrementalVacuum(int numberOfPagesToBeRemoved) {
        config.incrementalVacuum(numberOfPagesToBeRemoved);
    }

    /**
     * Sets the journal mode for databases associated with the current database connection.
     *
     * @param mode One of DELETE, TRUNCATE, PERSIST, MEMORY, WAL or OFF.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_journal_mode">
     *     https://www.sqlite.org/pragma.html#pragma_journal_mode</a>
     */
    public void setJournalMode(String mode) {
        config.setJournalMode(JournalMode.valueOf(mode));
    }

    /**
     * Sets the limit of the size of rollback-journal and WAL files left in the file-system after
     * transactions or checkpoints.
     *
     * @param limit The default journal size limit is -1 (no limit).
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_journal_size_limit">
     *     https://www.sqlite.org/pragma.html#pragma_journal_size_limit</a>
     */
    public void setJournalSizeLimit(int limit) {
        config.setJournalSizeLimit(limit);
    }

    /**
     * Set the value of the legacy_file_format flag. When this flag is on, new databases are created
     * in a file format that is readable and writable by all versions of SQLite going back to 3.0.0.
     * When the flag is off, new databases are created using the latest file format which might not
     * be readable or writable by versions of SQLite prior to 3.3.0.
     *
     * @param use True to turn on; false to turn off.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_legacy_file_format">https://www.sqlite.org/pragma.html#pragma_legacy_file_format</a>
     */
    public void setLegacyFileFormat(boolean use) {
        config.useLegacyFileFormat(use);
    }

    /**
     * Sets the value of the legacy_alter_table flag. When this flag is on, the ALTER TABLE RENAME
     * command (for changing the name of a table) works as it did in SQLite 3.24.0 (2018-06-04) and
     * earlier.When the flag is off, using the ALTER TABLE RENAME command will mean that all
     * references to the table anywhere in the schema will be converted to the new name.
     *
     * @param flag True to turn on legacy alter table behaviour; false to turn off.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_legacy_alter_table</a>
     */
    public void setLegacyAlterTable(boolean flag) {
        config.setLegacyAlterTable(flag);
    }

    /**
     * Sets the database connection locking-mode.
     *
     * @param mode Either NORMAL or EXCLUSIVE.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_locking_mode">
     *     https://www.sqlite.org/pragma.html#pragma_locking_mode</a>
     */
    public void setLockingMode(String mode) {
        config.setLockingMode(LockingMode.valueOf(mode));
    }

    /**
     * Set the page size of the database.
     *
     * @param numBytes The page size must be a power of two between 512 and 65536 inclusive.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_page_size">
     *     https://www.sqlite.org/pragma.html#pragma_page_size</a>
     */
    public void setPageSize(int numBytes) {
        config.setPageSize(numBytes);
    }

    /**
     * Set the maximum number of pages in the database file.
     *
     * @param numPages The maximum page count cannot be reduced below the current database size.
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_max_page_count">
     *     https://www.sqlite.org/pragma.html#pragma_max_page_count</a>
     */
    public void setMaxPageCount(int numPages) {
        config.setMaxPageCount(numPages);
    }

    /**
     * Set READ UNCOMMITTED isolation
     *
     * @param useReadUncommittedIsolationMode True to turn on; false to turn off.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_read_uncommitted">https://www.sqlite.org/pragma.html#pragma_read_uncommitted</a>
     */
    public void setReadUncommitted(boolean useReadUncommittedIsolationMode) {
        config.setReadUncommitted(useReadUncommittedIsolationMode);
    }

    /**
     * Enables or disables the recursive trigger capability. Changing the recursive_triggers setting
     * affects the execution of all statements prepared using the database connection, including
     * those prepared before the setting was changed.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_recursive_triggers">https://www.sqlite.org/pragma.html#pragma_recursive_triggers</a>
     */
    public void setRecursiveTriggers(boolean enable) {
        config.enableRecursiveTriggers(enable);
    }

    /**
     * Enables or disables the reverse_unordered_selects flag. When enabled it causes SELECT
     * statements without an ORDER BY clause to emit their results in the reverse order of what they
     * normally would.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_reverse_unordered_selects">https://www.sqlite.org/pragma.html#pragma_reverse_unordered_selects</a>
     */
    public void setReverseUnorderedSelects(boolean enable) {
        config.enableReverseUnorderedSelects(enable);
    }

    /**
     * Enables or disables the short_column_names flag. This flag affects the way SQLite names
     * columns of data returned by SELECT statements.
     *
     * @param enable True to enable; false to disable.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_short_column_names">https://www.sqlite.org/pragma.html#pragma_short_column_names</a>
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_fullfsync">https://www.sqlite.org/pragma.html#pragma_fullfsync</a>
     */
    public void setShortColumnNames(boolean enable) {
        config.enableShortColumnNames(enable);
    }

    /**
     * Sets the setting of the "synchronous" flag.
     *
     * @param mode One of OFF, NORMAL or FULL;
     * @see <a href="https://www.sqlite.org/pragma.html#pragma_synchronous">
     *     https://www.sqlite.org/pragma.html#pragma_synchronous</a>
     */
    public void setSynchronous(String mode) {
        config.setSynchronous(SynchronousMode.valueOf(mode));
    }

    /**
     * Set the temp_store type which is used to determine where temporary tables and indices are
     * stored.
     *
     * @param storeType One of "DEFAULT", "FILE", "MEMORY"
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_temp_store">https://www.sqlite.org/pragma.html#pragma_temp_store</a>
     */
    public void setTempStore(String storeType) {
        config.setTempStore(TempStore.valueOf(storeType));
    }

    /**
     * Set the value of the sqlite3_temp_directory global variable, which many operating-system
     * interface backends use to determine where to store temporary tables and indices.
     *
     * @param directoryName The temporary directory name.
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_temp_store_directory">https://www.sqlite.org/pragma.html#pragma_temp_store_directory</a>
     */
    public void setTempStoreDirectory(String directoryName) {
        config.setTempStoreDirectory(directoryName);
    }

    /**
     * Sets the mode that will be used to start transactions for this database.
     *
     * @param transactionMode One of DEFERRED, IMMEDIATE or EXCLUSIVE.
     * @see <a
     *     href="https://www.sqlite.org/lang_transaction.html">https://www.sqlite.org/lang_transaction.html</a>
     */
    public void setTransactionMode(String transactionMode) {
        config.setTransactionMode(transactionMode);
    }

    /**
     * Sets the value of the user-version. It is a big-endian 32-bit signed integer stored in the
     * database header at offset 60.
     *
     * @param version
     * @see <a
     *     href="https://www.sqlite.org/pragma.html#pragma_schema_version">https://www.sqlite.org/pragma.html#pragma_schema_version</a>
     */
    public void setUserVersion(int version) {
        config.setUserVersion(version);
    }

    // codes for the DataSource interface

    /** @see javax.sql.DataSource#getConnection() */
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    /** @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String) */
    public SQLiteConnection getConnection(String username, String password) throws SQLException {
        Properties p = config.toProperties();
        if (username != null) p.put("user", username);
        if (password != null) p.put("pass", password);
        return JDBC.createConnection(url, p);
    }

    /** @see javax.sql.DataSource#getLogWriter() */
    public PrintWriter getLogWriter() throws SQLException {
        return logger;
    }

    /** @see javax.sql.DataSource#getLoginTimeout() */
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger");
    }

    /** @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter) */
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logger = out;
    }

    /** @see javax.sql.DataSource#setLoginTimeout(int) */
    public void setLoginTimeout(int seconds) throws SQLException {
        loginTimeout = seconds;
    }

    /**
     * Determines if this object wraps a given class.
     *
     * @param iface The class to check.
     * @return True if it is an instance of the current class; false otherwise.
     * @throws SQLException
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    /**
     * Casts this object to the given class.
     *
     * @param iface The class to cast to.
     * @return The casted class.
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return (T) this;
    }
}
