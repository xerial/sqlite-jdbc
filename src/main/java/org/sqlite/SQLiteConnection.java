/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.sqlite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sqlite.SQLiteConfig.DateClass;
import org.sqlite.SQLiteConfig.DatePrecision;
import org.sqlite.SQLiteConfig.TransactionMode;

public class SQLiteConnection implements Connection
{
    private static final String RESOURCE_NAME_PREFIX = ":resource:";

    private final String url;
    private String       fileName;
    private DB           db                   = null;
    private MetaData     meta                 = null;
    private boolean      autoCommit           = true;
    private int          transactionIsolation = TRANSACTION_SERIALIZABLE;
    private int          busyTimeout              = 0;
    private final int    openModeFlags;
    private TransactionMode
                         transactionMode      = TransactionMode.DEFFERED;

    private final static Map<TransactionMode, String> beginCommandMap =
        new HashMap<SQLiteConfig.TransactionMode, String>();

    static {
        beginCommandMap.put(TransactionMode.DEFFERED, "begin;");
        beginCommandMap.put(TransactionMode.IMMEDIATE, "begin immediate;");
        beginCommandMap.put(TransactionMode.EXCLUSIVE, "begin exclusive;");
    }

    /* Date storage configuration */
    public final DateClass dateClass;
    public final DatePrecision datePrecision; //Calendar.SECOND or Calendar.MILLISECOND
    public final long dateMultiplier;
    public final DateFormat dateFormat;

    /**
     * Constructor to create a connection to a database at the given location.
     * @param url The location of the database.
     * @param fileName The database.
     * @throws SQLException
     */
    public SQLiteConnection(String url, String fileName) throws SQLException {
        this(url, fileName, new Properties());
    }

    /**
     * Constructor to create a pre-configured connection to a database at the
     * given location.
     * @param url The location of the database file.
     * @param fileName The database.
     * @param prop The configurations to apply.
     * @throws SQLException
     */
    public SQLiteConnection(String url, String fileName, Properties prop) throws SQLException {
        this.url = url;
        this.fileName = fileName;

        SQLiteConfig config = new SQLiteConfig(prop);
        this.dateClass = config.dateClass;
        this.dateMultiplier = config.dateMultiplier;
        this.dateFormat = new SimpleDateFormat(config.dateStringFormat);
        this.datePrecision = config.datePrecision;
        this.transactionMode = config.getTransactionMode();
        this.openModeFlags = config.getOpenModeFlags();

        open(openModeFlags, config.busyTimeout);

        if (fileName.startsWith("file:") && !fileName.contains("cache="))
        {   // URI cache overrides flags
            db.shared_cache(config.isEnabledSharedCache());
        }
        db.enable_load_extension(config.isEnabledLoadExtension());

        // set pragmas
        config.apply(this);
    }

    /**
     * Opens a connection to the database using an SQLite library.
     * @param openModeFlags Flags for file open operations.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    private void open(int openModeFlags, int busyTimeout) throws SQLException {
        // check the path to the file exists
        if (!":memory:".equals(fileName) && !fileName.startsWith("file:") && !fileName.contains("mode=memory")) {
            if (fileName.startsWith(RESOURCE_NAME_PREFIX)) {
                String resourceName = fileName.substring(RESOURCE_NAME_PREFIX.length());

                // search the class path
                ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
                URL resourceAddr = contextCL.getResource(resourceName);
                if (resourceAddr == null) {
                    try {
                        resourceAddr = new URL(resourceName);
                    }
                    catch (MalformedURLException e) {
                        throw new SQLException(String.format("resource %s not found: %s", resourceName, e));
                    }
                }

                try {
                    fileName = extractResource(resourceAddr).getAbsolutePath();
                }
                catch (IOException e) {
                    throw new SQLException(String.format("failed to load %s: %s", resourceName, e));
                }
            }
            else {
                File file = new File(fileName).getAbsoluteFile();
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    for (File up = parent; up != null && !up.exists();) {
                        parent = up;
                        up = up.getParentFile();
                    }
                    throw new SQLException("path to '" + fileName + "': '" + parent + "' does not exist");
                }

                // check write access if file does not exist
                try {
                    // The extra check to exists() is necessary as createNewFile()
                    // does not follow the JavaDoc when used on read-only shares.
                    if (!file.exists() && file.createNewFile())
                        file.delete();
                }
                catch (Exception e) {
                    throw new SQLException("opening db: '" + fileName + "': " + e.getMessage());
                }
                fileName = file.getAbsolutePath();
            }
        }

        // load the native DB
        try {
            NativeDB.load();
            db = new NativeDB();
        }
        catch (Exception e) {
            SQLException err = new SQLException("Error opening connection");
            err.initCause(e);
            throw err;
        }

        db.open(this, fileName, openModeFlags);
        setBusyTimeout(busyTimeout);
    }

    /**
     * Returns a file name from the given resource address.
     * @param resourceAddr The resource address.
     * @return The extracted file name.
     * @throws IOException
     */
    private File extractResource(URL resourceAddr) throws IOException {
        if (resourceAddr.getProtocol().equals("file")) {
            try {
                return new File(resourceAddr.toURI());
            }
            catch (URISyntaxException e) {
                throw new IOException(e.getMessage());
            }
        }

        String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String dbFileName = String.format("sqlite-jdbc-tmp-%d.db", resourceAddr.hashCode());
        File dbFile = new File(tempFolder, dbFileName);

        if (dbFile.exists()) {
            long resourceLastModified = resourceAddr.openConnection().getLastModified();
            long tmpFileLastModified = dbFile.lastModified();
            if (resourceLastModified < tmpFileLastModified) {
                return dbFile;
            }
            else {
                // remove the old DB file
                boolean deletionSucceeded = dbFile.delete();
                if (!deletionSucceeded) {
                    throw new IOException("failed to remove existing DB file: " + dbFile.getAbsolutePath());
                }
            }

            //            String md5sum1 = SQLiteJDBCLoader.md5sum(resourceAddr.openStream());
            //            String md5sum2 = SQLiteJDBCLoader.md5sum(new FileInputStream(dbFile));
            //
            //            if (md5sum1.equals(md5sum2))
            //                return dbFile; // no need to extract the DB file
            //            else
            //            {
            //            }
        }

        byte[] buffer = new byte[8192]; // 8K buffer
        FileOutputStream writer = new FileOutputStream(dbFile);
        InputStream reader = resourceAddr.openStream();
        try {
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }
            return dbFile;
        }
        finally {
            writer.close();
            reader.close();
        }

    }

    /**
     * @return The busy timeout value for the connection.
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     */
    public int getBusyTimeout() {
        return busyTimeout;
    }

    /**
     * Sets the timeout value for the connection.
     * A timeout value less than or equal to zero turns off all busy handlers.
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     * @param milliseconds The timeout value in milliseconds.
     * @throws SQLException
     */
    public void setBusyTimeout(int milliseconds) throws SQLException {
        busyTimeout = milliseconds;
        db.busy_timeout(busyTimeout);
    }

    /**
     * @return Where the database is located.
     */
    String url() {
        return url;
    }

    /**
     * @return Compile-time library version numbers.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/c_source_id.html">http://www.sqlite.org/c3ref/c_source_id.html</a>
     */
    String libversion() throws SQLException {
        checkOpen();

        return db.libversion();
    }

    /**
     * @return The class interface to SQLite.
     */
    DB db() {
        return db;
    }

    /**
     * Whether an SQLite library interface to the database has been established.
     * @throws SQLException.
     */
    private void checkOpen() throws SQLException {
        if (db == null)
            throw new SQLException("database connection closed");
    }

    /**
     * Checks whether the type, concurrency, and holdability settings for a
     * {@link ResultSet} are supported by the SQLite interface. Supported
     * settings are:<ul>
     *  <li>type: {@link ResultSet.TYPE_FORWARD_ONLY}</li>
     *  <li>concurrency: {@link ResultSet.CONCUR_READ_ONLY})</li>
     *  <li>holdability: {@link ResultSet.CLOSE_CURSORS_AT_COMMIT}</li></ul>
     * @param rst the type setting.
     * @param rsc the concurrency setting.
     * @param rsh the holdability setting.
     * @throws SQLException
     */
    private void checkCursor(int rst, int rsc, int rsh) throws SQLException {
        if (rst != ResultSet.TYPE_FORWARD_ONLY)
            throw new SQLException("SQLite only supports TYPE_FORWARD_ONLY cursors");
        if (rsc != ResultSet.CONCUR_READ_ONLY)
            throw new SQLException("SQLite only supports CONCUR_READ_ONLY cursors");
        if (rsh != ResultSet.CLOSE_CURSORS_AT_COMMIT)
            throw new SQLException("SQLite only supports closing cursors at commit");
    }

    /**
     * @see java.lang.Object#finalize()
     */
    @Override
    public void finalize() throws SQLException {
        close();
    }

    /**
     * @see java.sql.Connection#close()
     */
    public void close() throws SQLException {
        if (db == null)
            return;
        if (meta != null)
            meta.close();

        db.close();
        db = null;
    }

    /**
     * @see java.sql.Connection#isClosed()
     */
    public boolean isClosed() throws SQLException {
        return db == null;
    }

    /**
     * @see java.sql.Connection#getCatalog()
     */
    public String getCatalog() throws SQLException {
        checkOpen();
        return null;
    }

    /**
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    public void setCatalog(String catalog) throws SQLException {
        checkOpen();
    }

    /**
     * @see java.sql.Connection#getHoldability()
     */
    public int getHoldability() throws SQLException {
        checkOpen();
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /**
     * @see java.sql.Connection#setHoldability(int)
     */
    public void setHoldability(int h) throws SQLException {
        checkOpen();
        if (h != ResultSet.CLOSE_CURSORS_AT_COMMIT)
            throw new SQLException("SQLite only supports CLOSE_CURSORS_AT_COMMIT");
    }

    /**
     * @see java.sql.Connection#getTransactionIsolation()
     */
    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    /**
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    public void setTransactionIsolation(int level) throws SQLException {
        checkOpen();

        switch (level) {
        case TRANSACTION_SERIALIZABLE:
            db.exec("PRAGMA read_uncommitted = false;");
            break;
        case TRANSACTION_READ_UNCOMMITTED:
            db.exec("PRAGMA read_uncommitted = true;");
            break;
        default:
            throw new SQLException("SQLite supports only TRANSACTION_SERIALIZABLE and TRANSACTION_READ_UNCOMMITTED.");
        }
        transactionIsolation = level;
    }

    /**
     * Sets the mode that will be used to start transactions on this connection.
     * @param mode One of {@link TransactionMode}
     * @see <a href="http://www.sqlite.org/lang_transaction.html">http://www.sqlite.org/lang_transaction.html</a>
     */
    protected void setTransactionMode(TransactionMode mode) {
        this.transactionMode = mode;
    }

    /**
     * @see java.sql.Connection#getTypeMap()
     */
    public Map<String,Class<?>> getTypeMap() throws SQLException {
        throw new SQLException("not yet implemented");
    }

    /**
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    @SuppressWarnings("rawtypes")
    public void setTypeMap(Map map) throws SQLException {
        throw new SQLException("not yet implemented");
    }

    /**
     * @see java.sql.Connection#isReadOnly()
     */
    public boolean isReadOnly() throws SQLException {
        return (openModeFlags & SQLiteOpenMode.READONLY.flag) != 0;
    }

    /**
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    public void setReadOnly(boolean ro) throws SQLException {
        // trying to change read-only flag
        if (ro != isReadOnly()) {
            throw new SQLException(
                "Cannot change read-only flag after establishing a connection." +
                " Use SQLiteConfig#setReadOnly and SQLiteConfig.createConnection().");
        }
    }

    /**
     * @throws SQLException 
     * @see java.sql.Connection#getMetaData()
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        checkOpen();

        if (meta == null) {
            meta = new MetaData(this);
        }

        return meta;
    }

    /**
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    public String nativeSQL(String sql) {
        return sql;
    }

    /**
     * @see java.sql.Connection#clearWarnings()
     */
    public void clearWarnings() throws SQLException {}

    /**
     * @see java.sql.Connection#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    /**
     * @see java.sql.Connection#getAutoCommit()
     */
    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        return autoCommit;
    }

    /**
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    public void setAutoCommit(boolean ac) throws SQLException {
        checkOpen();
        if (autoCommit == ac)
            return;
        autoCommit = ac;
        db.exec(autoCommit ? "commit;" : beginCommandMap.get(transactionMode));
    }

    /**
     * @see java.sql.Connection#commit()
     */
    public void commit() throws SQLException {
        checkOpen();
        if (autoCommit)
            throw new SQLException("database in auto-commit mode");
        db.exec("commit;");
        db.exec(beginCommandMap.get(transactionMode));
    }

    /**
     * @see java.sql.Connection#rollback()
     */
    public void rollback() throws SQLException {
        checkOpen();
        if (autoCommit)
            throw new SQLException("database in auto-commit mode");
        db.exec("rollback;");
        db.exec(beginCommandMap.get(transactionMode));
    }

    /**
     * @see java.sql.Connection#createStatement()
     */
    public Statement createStatement() throws SQLException {
        return createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /**
     * @see java.sql.Connection#createStatement(int, int)
     */
    public Statement createStatement(int rsType, int rsConcurr) throws SQLException {
        return createStatement(rsType, rsConcurr, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /**
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    public Statement createStatement(int rst, int rsc, int rsh) throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new Stmt(this);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        return prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    public CallableStatement prepareCall(String sql, int rst, int rsc) throws SQLException {
        return prepareCall(sql, rst, rsc, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    public CallableStatement prepareCall(String sql, int rst, int rsc, int rsh) throws SQLException {
        throw new SQLException("SQLite does not support Stored Procedures");
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    public PreparedStatement prepareStatement(String sql, int autoC) throws SQLException {
        return prepareStatement(sql);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    public PreparedStatement prepareStatement(String sql, int[] colInds) throws SQLException {
        return prepareStatement(sql);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    public PreparedStatement prepareStatement(String sql, String[] colNames) throws SQLException {
        return prepareStatement(sql);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int rst, int rsc) throws SQLException {
        return prepareStatement(sql, rst, rsc, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int rst, int rsc, int rsh) throws SQLException {
        checkOpen();
        checkCursor(rst, rsc, rsh);

        return new PrepStmt(this, sql);
    }

    /** 
     * @return One of "native" or "unloaded".
     */
    String getDriverVersion() {
        // Used to supply DatabaseMetaData.getDriverVersion()
        return  db != null ? "native" : "unloaded";
    }

    // UNUSED FUNCTIONS /////////////////////////////////////////////

    /**
     * @see java.sql.Connection#setSavepoint()
     */
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    /**
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    /**
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    /**
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLException("unsupported by SQLite");
    }
}
