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
//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteDataSource.java
// Since: Mar 11, 2010
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.sqlite.SQLiteConfig.Encoding;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.LockingMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;

/**
 * Provides {@link DataSource} API for configuring SQLite database connection
 * 
 * @author leo
 * 
 */
public class SQLiteDataSource implements DataSource
{
    private SQLiteConfig          config;
    private transient PrintWriter logger;
    private int                   loginTimeout = 1;

    private String                url          = JDBC.PREFIX; // use memory database in default

    public SQLiteDataSource() {
        this.config = new SQLiteConfig(); // default configuration
    }

    public SQLiteDataSource(SQLiteConfig config) {
        this.config = config;
    }

    /**
     * Set the configuration parameters via {@link SQLiteConfig} object
     * 
     * @param config
     */
    public void setConfig(SQLiteConfig config) {
        this.config = config;
    }

    public SQLiteConfig getConfig() {
        return config;
    }

    // configuration parameters
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setSharedCache(boolean enable) {
        config.setSharedCache(enable);
    }

    public void setLoadExtension(boolean enable) {
        config.enableLoadExtension(enable);
    }

    public void setReadOnly(boolean readOnly) {
        config.setReadOnly(readOnly);
    }

    public void setCacheSize(int numberOfPages) {
        config.setCacheSize(numberOfPages);
    }

    public void setCaseSensitiveLike(boolean enable) {
        config.enableCaseSensitiveLike(enable);
    }

    public void setCouncChanges(boolean enable) {
        config.enableCountChanges(enable);
    }

    public void setDefaultCacheSize(int numberOfPages) {
        config.setDefaultCacheSize(numberOfPages);
    }

    public void setEncoding(String encoding) {
        config.setEncoding(Encoding.valueOf(encoding));
    }

    public void setEnforceForeinKeys(boolean enforce) {
        config.enforceForeignKeys(enforce);
    }

    public void setFullColumnNames(boolean enable) {
        config.enableFullColumnNames(enable);
    }

    public void setFullSync(boolean enable) {
        config.enableFullSync(enable);
    }

    public void setIncrementalVacuum(int numberOfPagesToBeRemoved) {
        config.incrementalVacuum(numberOfPagesToBeRemoved);
    }

    public void setJournalMode(String mode) {
        config.setJournalMode(JournalMode.valueOf(mode));
    }

    public void setJournalSizeLimit(int limit) {
        config.setJounalSizeLimit(limit);
    }

    public void setLegacyFileFormat(boolean use) {
        config.useLegacyFileFormat(use);
    }

    public void setLockingMode(String mode) {
        config.setLockingMode(LockingMode.valueOf(mode));
    }

    public void setPageSize(int numBytes) {
        config.setPageSize(numBytes);
    }

    public void setMaxPageCount(int numPages) {
        config.setMaxPageCount(numPages);
    }

    public void setReadUncommited(boolean useReadUncommitedIsolationMode) {
        config.setReadUncommited(useReadUncommitedIsolationMode);
    }

    public void setRecursiveTriggers(boolean enable) {
        config.enableRecursiveTriggers(enable);
    }

    public void setReverseUnorderedSelects(boolean enable) {
        config.enableReverseUnorderedSelects(enable);
    }

    public void setShortColumnNames(boolean enable) {
        config.enableShortColumnNames(enable);
    }

    public void setSynchronous(String mode) {
        config.setSynchronous(SynchronousMode.valueOf(mode));
    }

    public void setTempStore(String storeType) {
        config.setTempStore(TempStore.valueOf(storeType));
    }

    public void setTempStoreDirectory(String directoryName) {
        config.setTempStoreDirectory(directoryName);
    }

    public void setUserVersion(int version) {
        config.setUserVersion(version);
    }

    // codes for the DataSource interface    

    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Properties p = config.toProperties();
        if (username != null)
            p.put("user", username);
        if (password != null)
            p.put("pass", password);
        return JDBC.createConnection(url, p);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return logger;
    }

    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logger = out;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        loginTimeout = seconds;
    }

    public boolean isWrapperFor(Class< ? > iface) throws SQLException {
        return iface.isInstance(this);
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return (T) this;
    }

}
