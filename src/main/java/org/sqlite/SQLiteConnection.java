package org.sqlite;

import org.sqlite.core.CoreDatabaseMetaData;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDB;
import org.sqlite.jdbc4.JDBC4DatabaseMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 *
 */
public abstract class SQLiteConnection
        implements Connection
{
    private static final String RESOURCE_NAME_PREFIX = ":resource:";
    private final DB db;
    private CoreDatabaseMetaData meta = null;
    private final SQLiteConnectionConfig connectionConfig;

    /**
     * Connection constructor for reusing an existing DB handle
     * @param db
     */
    public SQLiteConnection(DB db) {
        this.db = db;
        connectionConfig = db.getConfig().newConnectionConfig();
    }

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
        this.db = open(url, fileName, prop);
        SQLiteConfig config = db.getConfig();
        this.connectionConfig = db.getConfig().newConnectionConfig();

        config.apply(this);
    }

    public SQLiteConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public CoreDatabaseMetaData getSQLiteDatabaseMetaData() throws SQLException {
        checkOpen();

        if (meta == null) {
            meta = new JDBC4DatabaseMetaData(this);
        }

        return meta;
    }

    @Override
    public DatabaseMetaData getMetaData()
            throws SQLException
    {
        return (DatabaseMetaData) getSQLiteDatabaseMetaData();
    }

    public String getUrl() {
        return db.getUrl();
    }

    public void setSchema(String schema) throws SQLException {
        // TODO
    }

    public String getSchema() throws SQLException {
        // TODO
        return null;
    }
    public void abort(Executor executor) throws SQLException {
        // TODO
    }
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        // TODO
    }
    public int getNetworkTimeout() throws SQLException {
        // TODO
        return 0;
    }

    /**
     * Checks whether the type, concurrency, and holdability settings for a
     * {@link ResultSet} are supported by the SQLite interface. Supported
     * settings are:<ul>
     *  <li>type: {@link ResultSet#TYPE_FORWARD_ONLY}</li>
     *  <li>concurrency: {@link ResultSet#CONCUR_READ_ONLY})</li>
     *  <li>holdability: {@link ResultSet#CLOSE_CURSORS_AT_COMMIT}</li></ul>
     * @param rst the type setting.
     * @param rsc the concurrency setting.
     * @param rsh the holdability setting.
     * @throws SQLException
     */
    protected void checkCursor(int rst, int rsc, int rsh) throws SQLException {
        if (rst != ResultSet.TYPE_FORWARD_ONLY)
            throw new SQLException("SQLite only supports TYPE_FORWARD_ONLY cursors");
        if (rsc != ResultSet.CONCUR_READ_ONLY)
            throw new SQLException("SQLite only supports CONCUR_READ_ONLY cursors");
        if (rsh != ResultSet.CLOSE_CURSORS_AT_COMMIT)
            throw new SQLException("SQLite only supports closing cursors at commit");
    }

    /**
     * Sets the mode that will be used to start transactions on this connection.
     * @param mode One of {@link SQLiteConfig.TransactionMode}
     * @see <a href="http://www.sqlite.org/lang_transaction.html">http://www.sqlite.org/lang_transaction.html</a>
     */
    protected void setTransactionMode(SQLiteConfig.TransactionMode mode) {
        connectionConfig.setTransactionMode(mode);
    }

    /**
     * @see java.sql.Connection#getTransactionIsolation()
     */
    @Override
    public int getTransactionIsolation() {
        return connectionConfig.getTransactionIsolation();
    }

    /**
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    public void setTransactionIsolation(int level) throws SQLException {
        checkOpen();

        switch (level) {
            case java.sql.Connection.TRANSACTION_SERIALIZABLE:
                getDatabase().exec("PRAGMA read_uncommitted = false;", getAutoCommit());
                break;
            case java.sql.Connection.TRANSACTION_READ_UNCOMMITTED:
                getDatabase().exec("PRAGMA read_uncommitted = true;", getAutoCommit());
                break;
            default:
                throw new SQLException("SQLite supports only TRANSACTION_SERIALIZABLE and TRANSACTION_READ_UNCOMMITTED.");
        }
        connectionConfig.setTransactionIsolation(level);
    }

    /**
     * Opens a connection to the database using an SQLite library.
     *      * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/c_open_autoproxy.html">http://www.sqlite.org/c3ref/c_open_autoproxy.html</a>
     */
    private static DB open(String url, String origFileName, Properties props) throws SQLException {
        // Create a copy of the given properties
        Properties newProps = new Properties();
        newProps.putAll(props);

        // Extract pragma as properties
        String fileName = extractPragmasFromFilename(url, origFileName, newProps);
        SQLiteConfig config = new SQLiteConfig(newProps);

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
        DB db = null;
        try {
            NativeDB.load();
            db = new NativeDB(url, fileName, config);
        }
        catch (Exception e) {
            SQLException err = new SQLException("Error opening connection");
            err.initCause(e);
            throw err;
        }
        db.open(fileName, config.getOpenModeFlags());
        return db;
    }

    /**
     * Returns a file name from the given resource address.
     * @param resourceAddr The resource address.
     * @return The extracted file name.
     * @throws IOException
     */
    private static File extractResource(URL resourceAddr) throws IOException {
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

    public DB getDatabase() {
        return db;
    }

    /**
     * @see java.sql.Connection#getAutoCommit()
     */
    @Override
    public boolean getAutoCommit() throws SQLException {
        checkOpen();

        return connectionConfig.isAutoCommit();
    }

    /**
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    @Override
    public void setAutoCommit(boolean ac) throws SQLException {
        checkOpen();
        if (connectionConfig.isAutoCommit() == ac)
            return;

        connectionConfig.setAutoCommit(ac);
        db.exec(connectionConfig.isAutoCommit() ? "commit;" : connectionConfig.transactionPrefix(), ac);
    }

    /**
     * @return The busy timeout value for the connection.
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     */
    public int getBusyTimeout() {
        return db.getConfig().getBusyTimeout();
    }

    /**
     * Sets the timeout value for the connection.
     * A timeout value less than or equal to zero turns off all busy handlers.
     * @see <a href="http://www.sqlite.org/c3ref/busy_timeout.html">http://www.sqlite.org/c3ref/busy_timeout.html</a>
     * @param timeoutMillis The timeout value in milliseconds.
     * @throws SQLException
     */
    public void setBusyTimeout(int timeoutMillis)
            throws SQLException
    {
        db.getConfig().setBusyTimeout(timeoutMillis);
        db.busy_timeout(timeoutMillis);
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return db.isClosed();
    }

    /**
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() throws SQLException {
        if (isClosed())
            return;
        if (meta != null)
            meta.close();

        db.close();
    }

    /**
     * Whether an SQLite library interface to the database has been established.
     * @throws SQLException
     */
    protected void checkOpen() throws SQLException {
        if (isClosed())
            throw new SQLException("database connection closed");
    }

    /**
     * @return Compile-time library version numbers.
     * @throws SQLException
     * @see <a href="http://www.sqlite.org/c3ref/c_source_id.html">http://www.sqlite.org/c3ref/c_source_id.html</a>
     */
    public String libversion() throws SQLException {
        checkOpen();

        return db.libversion();
    }

    /**
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws SQLException {
        checkOpen();
        if (connectionConfig.isAutoCommit())
            throw new SQLException("database in auto-commit mode");
        db.exec("commit;", getAutoCommit());
        db.exec(connectionConfig.transactionPrefix(), getAutoCommit());
    }

    /**
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws SQLException {
        checkOpen();
        if (connectionConfig.isAutoCommit())
            throw new SQLException("database in auto-commit mode");
        db.exec("rollback;", getAutoCommit());
        db.exec(connectionConfig.transactionPrefix(), getAutoCommit());
    }


    /**
     * Extracts PRAGMA values from the filename and sets them into the Properties
     * object which will be used to build the SQLConfig.  The sanitized filename
     * is returned.
     *
     * @param filename
     * @param prop
     * @return a PRAGMA-sanitized filename
     * @throws SQLException
     */
    protected static String extractPragmasFromFilename(String url, String filename, Properties prop) throws SQLException {
        int parameterDelimiter = filename.indexOf('?');
        if (parameterDelimiter == -1) {
            // nothing to extract
            return filename;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(filename.substring(0, parameterDelimiter));

        int nonPragmaCount = 0;
        String [] parameters = filename.substring(parameterDelimiter + 1).split("&");
        for (int i = 0; i < parameters.length; i++) {
            // process parameters in reverse-order, last specified pragma value wins
            String parameter = parameters[parameters.length - 1 - i].trim();

            if (parameter.isEmpty()) {
                // duplicated &&& sequence, drop
                continue;
            }

            String [] kvp = parameter.split("=");
            String key = kvp[0].trim().toLowerCase();
            if (SQLiteConfig.pragmaSet.contains(key)) {
                if (kvp.length == 1) {
                    throw new SQLException(String.format("Please specify a value for PRAGMA %s in URL %s", key, url));
                }
                String value = kvp[1].trim();
                if (!value.isEmpty()) {
                    if (prop.containsKey(key)) {
                        //
                        // IGNORE
                        //
                        // this allows DriverManager.getConnection(String, Properties)
                        // to override URL parameters programmatically.
                        //
                        // It also ignores duplicate pragma keys in the URL. The reversed
                        // processing order ensures the last-supplied pragma value is used.
                    } else {
                        prop.setProperty(key,  value);
                    }
                }
            } else {
                // not a Pragma, retain as part of filename
                sb.append(nonPragmaCount == 0 ? '?' : '&');
                sb.append(parameter);
                nonPragmaCount++;
            }
        }

        final String newFilename = sb.toString();
        return newFilename;
    }

}
