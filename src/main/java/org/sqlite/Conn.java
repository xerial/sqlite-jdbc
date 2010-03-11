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
import java.util.Map;
import java.util.Properties;

class Conn implements Connection
{
    private final String url;
    private String       fileName;
    private DB           db                   = null;
    private MetaData     meta                 = null;
    private boolean      autoCommit           = true;
    private int          transactionIsolation = TRANSACTION_SERIALIZABLE;
    private int          timeout              = 0;

    public Conn(String url, String fileName) throws SQLException {
        this(url, fileName, new Properties());
    }

    public Conn(String url, String fileName, Properties prop) throws SQLException {
        this.url = url;
        this.fileName = fileName;

        SQLiteConfig config = new SQLiteConfig(prop);
        open(config.getOpenModeFlags());

        boolean enableSharedCache = config.isEnabledSharedCache();
        boolean enableLoadExtension = config.isEnabledLoadExtension();
        db.shared_cache(enableSharedCache);
        db.enable_load_extension(enableLoadExtension);

        // set pragmas
        config.apply(this);
    }

    private static final String RESOURCE_NAME_PREFIX = ":resource:";

    private void open(int openModeFlags) throws SQLException {
        // check the path to the file exists
        if (!":memory:".equals(fileName)) {
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

        // tries to load native library first
        try {
            Class< ? > nativedb = Class.forName("org.sqlite.NativeDB");
            if (((Boolean) nativedb.getDeclaredMethod("load", (Class< ? >[]) null).invoke((Object) null,
                    (Object[]) null)).booleanValue())
                db = (DB) nativedb.newInstance();

        }
        catch (Exception e) {} // fall through to nested library

        // load nested library (pure-java SQLite)
        if (db == null) {
            try {
                db = (DB) Class.forName("org.sqlite.NestedDB").newInstance();
            }
            catch (Exception e) {
                throw new SQLException("no SQLite library found");
            }
        }

        db.open(this, fileName, openModeFlags);
        setTimeout(3000);
    }

    /**
     * @param resourceAddr
     * @return extracted file name
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

    int getTimeout() {
        return timeout;
    }

    void setTimeout(int ms) throws SQLException {
        timeout = ms;
        db.busy_timeout(ms);
    }

    String url() {
        return url;
    }

    String libversion() throws SQLException {
        return db.libversion();
    }

    DB db() {
        return db;
    }

    private void checkOpen() throws SQLException {
        if (db == null)
            throw new SQLException("database connection closed");
    }

    private void checkCursor(int rst, int rsc, int rsh) throws SQLException {
        if (rst != ResultSet.TYPE_FORWARD_ONLY)
            throw new SQLException("SQLite only supports TYPE_FORWARD_ONLY cursors");
        if (rsc != ResultSet.CONCUR_READ_ONLY)
            throw new SQLException("SQLite only supports CONCUR_READ_ONLY cursors");
        if (rsh != ResultSet.CLOSE_CURSORS_AT_COMMIT)
            throw new SQLException("SQLite only supports closing cursors at commit");
    }

    @Override
    public void finalize() throws SQLException {
        close();
    }

    public void close() throws SQLException {
        if (db == null)
            return;
        if (meta != null)
            meta.close();

        db.close();
        db = null;
    }

    public boolean isClosed() throws SQLException {
        return db == null;
    }

    public String getCatalog() throws SQLException {
        checkOpen();
        return null;
    }

    public void setCatalog(String catalog) throws SQLException {
        checkOpen();
    }

    public int getHoldability() throws SQLException {
        checkOpen();
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    public void setHoldability(int h) throws SQLException {
        checkOpen();
        if (h != ResultSet.CLOSE_CURSORS_AT_COMMIT)
            throw new SQLException("SQLite only supports CLOSE_CURSORS_AT_COMMIT");
    }

    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int level) throws SQLException {
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

    public Map getTypeMap() throws SQLException {
        throw new SQLException("not yet implemented");
    }

    public void setTypeMap(Map map) throws SQLException {
        throw new SQLException("not yet implemented");
    }

    public boolean isReadOnly() throws SQLException {
        return false;
    } // FIXME

    public void setReadOnly(boolean ro) throws SQLException {}

    public DatabaseMetaData getMetaData() {
        if (meta == null)
            meta = new MetaData(this);
        return meta;
    }

    public String nativeSQL(String sql) {
        return sql;
    }

    public void clearWarnings() throws SQLException {}

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        return autoCommit;
    }

    public void setAutoCommit(boolean ac) throws SQLException {
        checkOpen();
        if (autoCommit == ac)
            return;
        autoCommit = ac;
        db.exec(autoCommit ? "commit;" : "begin;");
    }

    public void commit() throws SQLException {
        checkOpen();
        if (autoCommit)
            throw new SQLException("database in auto-commit mode");
        db.exec("commit;");
        db.exec("begin;");
    }

    public void rollback() throws SQLException {
        checkOpen();
        if (autoCommit)
            throw new SQLException("database in auto-commit mode");
        db.exec("rollback;");
        db.exec("begin;");
    }

    public Statement createStatement() throws SQLException {
        return createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public Statement createStatement(int rsType, int rsConcurr) throws SQLException {
        return createStatement(rsType, rsConcurr, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public Statement createStatement(int rst, int rsc, int rsh) throws SQLException {
        checkCursor(rst, rsc, rsh);
        return new Stmt(this);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public CallableStatement prepareCall(String sql, int rst, int rsc) throws SQLException {
        return prepareCall(sql, rst, rsc, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public CallableStatement prepareCall(String sql, int rst, int rsc, int rsh) throws SQLException {
        throw new SQLException("SQLite does not support Stored Procedures");
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public PreparedStatement prepareStatement(String sql, int autoC) throws SQLException {
        return prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int[] colInds) throws SQLException {
        return prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, String[] colNames) throws SQLException {
        return prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int rst, int rsc) throws SQLException {
        return prepareStatement(sql, rst, rsc, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public PreparedStatement prepareStatement(String sql, int rst, int rsc, int rsh) throws SQLException {
        checkCursor(rst, rsc, rsh);
        return new PrepStmt(this, sql);
    }

    /** Used to supply DatabaseMetaData.getDriverVersion(). */
    String getDriverVersion() {
        if (db != null) {
            String dbname = db.getClass().getName();
            if (dbname.indexOf("NestedDB") >= 0)
                return "pure";
            if (dbname.indexOf("NativeDB") >= 0)
                return "native";
        }
        return "unloaded";
    }

    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints");
    }

    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLException("unsupported by SQLite");
    }
}
