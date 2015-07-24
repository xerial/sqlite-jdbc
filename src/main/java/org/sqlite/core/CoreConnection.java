package org.sqlite.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.sqlite.date.FastDateFormat;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.DateClass;
import org.sqlite.SQLiteConfig.DatePrecision;
import org.sqlite.SQLiteConfig.Pragma;
import org.sqlite.SQLiteConfig.TransactionMode;
import org.sqlite.SQLiteConnection;

public abstract class CoreConnection {
    private static final String RESOURCE_NAME_PREFIX = ":resource:";

    private final String url;
    private String fileName;
    protected DB db = null;
    protected CoreDatabaseMetaData meta = null;
    protected boolean autoCommit = true;
    protected int transactionIsolation = Connection.TRANSACTION_SERIALIZABLE;
    private int busyTimeout = 0;
    protected final int openModeFlags;
    protected TransactionMode transactionMode = TransactionMode.DEFFERED;

    protected final static Map<TransactionMode, String> beginCommandMap =
        new HashMap<SQLiteConfig.TransactionMode, String>();

    private final static Set<String> pragmaSet = new TreeSet<String>();
    static {
        beginCommandMap.put(TransactionMode.DEFFERED, "begin;");
        beginCommandMap.put(TransactionMode.IMMEDIATE, "begin immediate;");
        beginCommandMap.put(TransactionMode.EXCLUSIVE, "begin exclusive;");

        for (Pragma pragma : Pragma.values()) {
        	pragmaSet.add(pragma.pragmaName);
        }
    }

    /* Date storage configuration */
    public final DateClass dateClass;
    public final DatePrecision datePrecision; //Calendar.SECOND or Calendar.MILLISECOND
    public final long dateMultiplier;
    public final FastDateFormat dateFormat;
    public final String dateStringFormat;

    protected CoreConnection(String url, String fileName, Properties prop) throws SQLException
    {
        this.url = url;
        this.fileName = extractPragmasFromFilename(fileName, prop);

        SQLiteConfig config = new SQLiteConfig(prop);
        this.dateClass = config.dateClass;
        this.dateMultiplier = config.dateMultiplier;
        this.dateFormat = FastDateFormat.getInstance(config.dateStringFormat);
        this.dateStringFormat = config.dateStringFormat;
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
        config.apply((Connection)this);

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
    private String extractPragmasFromFilename(String filename, Properties prop) throws SQLException {
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
    		if (pragmaSet.contains(key)) {
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

        db.open((SQLiteConnection)this, fileName, openModeFlags);
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
    public String url() {
        return url;
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
     * @return The class interface to SQLite.
     */
    public DB db() {
        return db;
    }

    /**
     * Whether an SQLite library interface to the database has been established.
     * @throws SQLException.
     */
    protected void checkOpen() throws SQLException {
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
     * @param mode One of {@link TransactionMode}
     * @see <a href="http://www.sqlite.org/lang_transaction.html">http://www.sqlite.org/lang_transaction.html</a>
     */
    protected void setTransactionMode(TransactionMode mode) {
        this.transactionMode = mode;
    }

    /**
     * @return One of "native" or "unloaded".
     */
    public String getDriverVersion() {
        // Used to supply DatabaseMetaData.getDriverVersion()
        return  db != null ? "native" : "unloaded";
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
}
