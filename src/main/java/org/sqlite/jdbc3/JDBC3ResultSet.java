package org.sqlite.jdbc3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sqlite.core.CoreResultSet;
import org.sqlite.core.CoreStatement;
import org.sqlite.core.DB;
import org.sqlite.date.FastDateFormat;

public abstract class JDBC3ResultSet extends CoreResultSet {
    // ResultSet Functions //////////////////////////////////////////

    protected JDBC3ResultSet(CoreStatement stmt) {
        super(stmt);
    }

    /**
     * returns col in [1,x] form
     *
     * @see java.sql.ResultSet#findColumn(java.lang.String)
     */
    public int findColumn(String col) throws SQLException {
        checkOpen();
        Integer index = findColumnIndexInCache(col);
        if (index != null) {
            return index;
        }
        for (int i = 0; i < cols.length; i++) {
            if (col.equalsIgnoreCase(cols[i])) {
                return addColumnIndexInCache(col, i + 1);
            }
        }
        throw new SQLException("no such column: '" + col + "'");
    }

    /** @see java.sql.ResultSet#next() */
    public boolean next() throws SQLException {
        if (!open || emptyResultSet || pastLastRow) {
            return false; // finished ResultSet
        }
        lastCol = -1;

        // first row is loaded by execute(), so do not step() again
        if (row == 0) {
            row++;
            return true;
        }

        // check if we are row limited by the statement or the ResultSet
        if (maxRows != 0 && row == maxRows) {
            return false;
        }

        // do the real work
        int statusCode = stmt.pointer.safeRunInt(DB::step);
        switch (statusCode) {
            case SQLITE_DONE:
                pastLastRow = true;
                return false;
            case SQLITE_ROW:
                row++;
                return true;
            case SQLITE_BUSY:
            default:
                getDatabase().throwex(statusCode);
                return false;
        }
    }

    /** @see java.sql.ResultSet#getType() */
    public int getType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    /** @see java.sql.ResultSet#getFetchSize() */
    public int getFetchSize() {
        return limitRows;
    }

    /** @see java.sql.ResultSet#setFetchSize(int) */
    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || (maxRows != 0 && rows > maxRows)) {
            throw new SQLException("fetch size " + rows + " out of bounds " + maxRows);
        }
        limitRows = rows;
    }

    /** @see java.sql.ResultSet#getFetchDirection() */
    public int getFetchDirection() throws SQLException {
        checkOpen();
        return ResultSet.FETCH_FORWARD;
    }

    /** @see java.sql.ResultSet#setFetchDirection(int) */
    public void setFetchDirection(int d) throws SQLException {
        checkOpen();
        // Only FORWARD_ONLY ResultSets exist in SQLite, so only FETCH_FORWARD is permitted
        if (
        /*getType() == ResultSet.TYPE_FORWARD_ONLY &&*/
        d != ResultSet.FETCH_FORWARD) {
            throw new SQLException("only FETCH_FORWARD direction supported");
        }
    }

    /** @see java.sql.ResultSet#isAfterLast() */
    public boolean isAfterLast() {
        return pastLastRow && !emptyResultSet;
    }

    /** @see java.sql.ResultSet#isBeforeFirst() */
    public boolean isBeforeFirst() {
        return !emptyResultSet && open && row == 0;
    }

    /** @see java.sql.ResultSet#isFirst() */
    public boolean isFirst() {
        return row == 1;
    }

    /** @see java.sql.ResultSet#isLast() */
    public boolean isLast() throws SQLException {
        throw new SQLFeatureNotSupportedException("not supported by sqlite");
    }

    /** @see java.sql.ResultSet#getRow() */
    public int getRow() {
        return row;
    }

    /** @see java.sql.ResultSet#wasNull() */
    public boolean wasNull() throws SQLException {
        return safeGetColumnType(markCol(lastCol)) == SQLITE_NULL;
    }

    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    /** @see java.sql.ResultSet#getBigDecimal(int) */
    public BigDecimal getBigDecimal(int col) throws SQLException {
        switch (safeGetColumnType(checkCol(col))) {
            case SQLITE_NULL:
                return null;
            case SQLITE_INTEGER:
                return BigDecimal.valueOf(safeGetLongCol(col));
            case SQLITE_FLOAT:
                // avoid double precision
            default:
                final String stringValue = safeGetColumnText(col);
                try {
                    return new BigDecimal(stringValue);
                } catch (NumberFormatException e) {
                    throw new SQLException("Bad value for type BigDecimal : " + stringValue);
                }
        }
    }

    /** @see java.sql.ResultSet#getBigDecimal(java.lang.String) */
    public BigDecimal getBigDecimal(String col) throws SQLException {
        return getBigDecimal(findColumn(col));
    }

    /** @see java.sql.ResultSet#getBoolean(int) */
    public boolean getBoolean(int col) throws SQLException {
        return getInt(col) != 0;
    }

    /** @see java.sql.ResultSet#getBoolean(java.lang.String) */
    public boolean getBoolean(String col) throws SQLException {
        return getBoolean(findColumn(col));
    }

    /** @see java.sql.ResultSet#getBinaryStream(int) */
    public InputStream getBinaryStream(int col) throws SQLException {
        byte[] bytes = getBytes(col);
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        } else {
            return null;
        }
    }

    /** @see java.sql.ResultSet#getBinaryStream(java.lang.String) */
    public InputStream getBinaryStream(String col) throws SQLException {
        return getBinaryStream(findColumn(col));
    }

    /** @see java.sql.ResultSet#getByte(int) */
    public byte getByte(int col) throws SQLException {
        return (byte) getInt(col);
    }

    /** @see java.sql.ResultSet#getByte(java.lang.String) */
    public byte getByte(String col) throws SQLException {
        return getByte(findColumn(col));
    }

    /** @see java.sql.ResultSet#getBytes(int) */
    public byte[] getBytes(int col) throws SQLException {
        return stmt.pointer.safeRun((db, ptr) -> db.column_blob(ptr, markCol(col)));
    }

    /** @see java.sql.ResultSet#getBytes(java.lang.String) */
    public byte[] getBytes(String col) throws SQLException {
        return getBytes(findColumn(col));
    }

    /** @see java.sql.ResultSet#getCharacterStream(int) */
    public Reader getCharacterStream(int col) throws SQLException {
        String string = getString(col);
        return string == null ? null : new StringReader(string);
    }

    /** @see java.sql.ResultSet#getCharacterStream(java.lang.String) */
    public Reader getCharacterStream(String col) throws SQLException {
        return getCharacterStream(findColumn(col));
    }

    /** @see java.sql.ResultSet#getDate(int) */
    public Date getDate(int col) throws SQLException {
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    return new Date(
                            getConnectionConfig().getDateFormat().parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing date", e);
                }

            case SQLITE_FLOAT:
                return new Date(julianDateToCalendar(safeGetDoubleCol(col)).getTimeInMillis());

            default: // SQLITE_INTEGER:
                return new Date(safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());
        }
    }

    /** @see java.sql.ResultSet#getDate(int, java.util.Calendar) */
    public Date getDate(int col, Calendar cal) throws SQLException {
        requireCalendarNotNull(cal);
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    FastDateFormat dateFormat =
                            FastDateFormat.getInstance(
                                    getConnectionConfig().getDateStringFormat(), cal.getTimeZone());

                    return new java.sql.Date(dateFormat.parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing time stamp", e);
                }

            case SQLITE_FLOAT:
                return new Date(julianDateToCalendar(safeGetDoubleCol(col), cal).getTimeInMillis());

            default: // SQLITE_INTEGER:
                cal.setTimeInMillis(
                        safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());
                return new Date(cal.getTime().getTime());
        }
    }

    /** @see java.sql.ResultSet#getDate(java.lang.String) */
    public Date getDate(String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance());
    }

    /** @see java.sql.ResultSet#getDate(java.lang.String, java.util.Calendar) */
    public Date getDate(String col, Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal);
    }

    /** @see java.sql.ResultSet#getDouble(int) */
    public double getDouble(int col) throws SQLException {
        if (safeGetColumnType(markCol(col)) == SQLITE_NULL) {
            return 0;
        }
        return safeGetDoubleCol(col);
    }

    /** @see java.sql.ResultSet#getDouble(java.lang.String) */
    public double getDouble(String col) throws SQLException {
        return getDouble(findColumn(col));
    }

    /** @see java.sql.ResultSet#getFloat(int) */
    public float getFloat(int col) throws SQLException {
        if (safeGetColumnType(markCol(col)) == SQLITE_NULL) {
            return 0;
        }
        return (float) safeGetDoubleCol(col);
    }

    /** @see java.sql.ResultSet#getFloat(java.lang.String) */
    public float getFloat(String col) throws SQLException {
        return getFloat(findColumn(col));
    }

    /** @see java.sql.ResultSet#getInt(int) */
    public int getInt(int col) throws SQLException {
        return stmt.pointer.safeRunInt((db, ptr) -> db.column_int(ptr, markCol(col)));
    }

    /** @see java.sql.ResultSet#getInt(java.lang.String) */
    public int getInt(String col) throws SQLException {
        return getInt(findColumn(col));
    }

    /** @see java.sql.ResultSet#getLong(int) */
    public long getLong(int col) throws SQLException {
        return safeGetLongCol(col);
    }

    /** @see java.sql.ResultSet#getLong(java.lang.String) */
    public long getLong(String col) throws SQLException {
        return getLong(findColumn(col));
    }

    /** @see java.sql.ResultSet#getShort(int) */
    public short getShort(int col) throws SQLException {
        return (short) getInt(col);
    }

    /** @see java.sql.ResultSet#getShort(java.lang.String) */
    public short getShort(String col) throws SQLException {
        return getShort(findColumn(col));
    }

    /** @see java.sql.ResultSet#getString(int) */
    public String getString(int col) throws SQLException {
        return safeGetColumnText(col);
    }

    /** @see java.sql.ResultSet#getString(java.lang.String) */
    public String getString(String col) throws SQLException {
        return getString(findColumn(col));
    }

    /** @see java.sql.ResultSet#getTime(int) */
    public Time getTime(int col) throws SQLException {
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    return new Time(
                            getConnectionConfig().getDateFormat().parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing time", e);
                }

            case SQLITE_FLOAT:
                return new Time(julianDateToCalendar(safeGetDoubleCol(col)).getTimeInMillis());

            default: // SQLITE_INTEGER
                return new Time(safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());
        }
    }

    /** @see java.sql.ResultSet#getTime(int, java.util.Calendar) */
    public Time getTime(int col, Calendar cal) throws SQLException {
        requireCalendarNotNull(cal);
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    FastDateFormat dateFormat =
                            FastDateFormat.getInstance(
                                    getConnectionConfig().getDateStringFormat(), cal.getTimeZone());

                    return new Time(dateFormat.parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing time", e);
                }

            case SQLITE_FLOAT:
                return new Time(julianDateToCalendar(safeGetDoubleCol(col), cal).getTimeInMillis());

            default: // SQLITE_INTEGER
                cal.setTimeInMillis(
                        safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());
                return new Time(cal.getTime().getTime());
        }
    }

    /** @see java.sql.ResultSet#getTime(java.lang.String) */
    public Time getTime(String col) throws SQLException {
        return getTime(findColumn(col));
    }

    /** @see java.sql.ResultSet#getTime(java.lang.String, java.util.Calendar) */
    public Time getTime(String col, Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal);
    }

    /** @see java.sql.ResultSet#getTimestamp(int) */
    public Timestamp getTimestamp(int col) throws SQLException {
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    return new Timestamp(
                            getConnectionConfig().getDateFormat().parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing time stamp", e);
                }

            case SQLITE_FLOAT:
                return new Timestamp(julianDateToCalendar(safeGetDoubleCol(col)).getTimeInMillis());

            default: // SQLITE_INTEGER:
                return new Timestamp(
                        safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());
        }
    }

    /** @see java.sql.ResultSet#getTimestamp(int, java.util.Calendar) */
    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        requireCalendarNotNull(cal);
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_NULL:
                return null;

            case SQLITE_TEXT:
                String dateText = safeGetColumnText(col);
                if ("".equals(dateText)) {
                    return null;
                }
                try {
                    FastDateFormat dateFormat =
                            FastDateFormat.getInstance(
                                    getConnectionConfig().getDateStringFormat(), cal.getTimeZone());

                    return new Timestamp(dateFormat.parse(dateText).getTime());
                } catch (Exception e) {
                    throw new SQLException("Error parsing time stamp", e);
                }

            case SQLITE_FLOAT:
                return new Timestamp(julianDateToCalendar(safeGetDoubleCol(col)).getTimeInMillis());

            default: // SQLITE_INTEGER
                cal.setTimeInMillis(
                        safeGetLongCol(col) * getConnectionConfig().getDateMultiplier());

                return new Timestamp(cal.getTime().getTime());
        }
    }

    /** @see java.sql.ResultSet#getTimestamp(java.lang.String) */
    public Timestamp getTimestamp(String col) throws SQLException {
        return getTimestamp(findColumn(col));
    }

    /** @see java.sql.ResultSet#getTimestamp(java.lang.String, java.util.Calendar) */
    public Timestamp getTimestamp(String c, Calendar ca) throws SQLException {
        return getTimestamp(findColumn(c), ca);
    }

    /** @see java.sql.ResultSet#getObject(int) */
    public Object getObject(int col) throws SQLException {
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_INTEGER:
                long val = getLong(col);
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    return new Long(val);
                } else {
                    return new Integer((int) val);
                }
            case SQLITE_FLOAT:
                return new Double(getDouble(col));
            case SQLITE_BLOB:
                return getBytes(col);
            case SQLITE_NULL:
                return null;
            case SQLITE_TEXT:
            default:
                return getString(col);
        }
    }

    /** @see java.sql.ResultSet#getObject(java.lang.String) */
    public Object getObject(String col) throws SQLException {
        return getObject(findColumn(col));
    }

    /** @see java.sql.ResultSet#getStatement() */
    public Statement getStatement() {
        return (Statement) stmt;
    }

    /** @see java.sql.ResultSet#getCursorName() */
    public String getCursorName() {
        return null;
    }

    /** @see java.sql.ResultSet#getWarnings() */
    public SQLWarning getWarnings() {
        return null;
    }

    /** @see java.sql.ResultSet#clearWarnings() */
    public void clearWarnings() {}

    // ResultSetMetaData Functions //////////////////////////////////

    /** Pattern used to extract the column type name from table column definition. */
    protected static final Pattern COLUMN_TYPENAME = Pattern.compile("([^\\(]*)");

    /** Pattern used to extract the column type name from a cast(col as type) */
    protected static final Pattern COLUMN_TYPECAST =
            Pattern.compile("cast\\(.*?\\s+as\\s+(.*?)\\s*\\)");

    /**
     * Pattern used to extract the precision and scale from column meta returned by the JDBC driver.
     */
    protected static final Pattern COLUMN_PRECISION = Pattern.compile(".*?\\((.*?)\\)");

    // we do not need to check the RS is open, only that colsMeta
    // is not null, done with checkCol(int).

    /** @see java.sql.ResultSet#getMetaData() */
    public ResultSetMetaData getMetaData() {
        return (ResultSetMetaData) this;
    }

    /** @see java.sql.ResultSetMetaData#getCatalogName(int) */
    public String getCatalogName(int col) throws SQLException {
        return safeGetColumnTableName(col);
    }

    /** @see java.sql.ResultSetMetaData#getColumnClassName(int) */
    public String getColumnClassName(int col) throws SQLException {
        switch (safeGetColumnType(markCol(col))) {
            case SQLITE_INTEGER:
                long val = getLong(col);
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    return "java.lang.Long";
                } else {
                    return "java.lang.Integer";
                }
            case SQLITE_FLOAT:
                return "java.lang.Double";
            case SQLITE_BLOB:
            case SQLITE_NULL:
                return "java.lang.Object";
            case SQLITE_TEXT:
            default:
                return "java.lang.String";
        }
    }

    /** @see java.sql.ResultSetMetaData#getColumnCount() */
    public int getColumnCount() throws SQLException {
        checkCol(1);
        return colsMeta.length;
    }

    /** @see java.sql.ResultSetMetaData#getColumnDisplaySize(int) */
    public int getColumnDisplaySize(int col) {
        return Integer.MAX_VALUE;
    }

    /** @see java.sql.ResultSetMetaData#getColumnLabel(int) */
    public String getColumnLabel(int col) throws SQLException {
        return getColumnName(col);
    }

    /** @see java.sql.ResultSetMetaData#getColumnName(int) */
    public String getColumnName(int col) throws SQLException {
        return safeGetColumnName(col);
    }

    /** @see java.sql.ResultSetMetaData#getColumnType(int) */
    public int getColumnType(int col) throws SQLException {
        String typeName = getColumnTypeName(col);
        int valueType = safeGetColumnType(checkCol(col));

        if (valueType == SQLITE_INTEGER || valueType == SQLITE_NULL) {
            if ("BOOLEAN".equals(typeName)) {
                return Types.BOOLEAN;
            }

            if ("TINYINT".equals(typeName)) {
                return Types.TINYINT;
            }

            if ("SMALLINT".equals(typeName) || "INT2".equals(typeName)) {
                return Types.SMALLINT;
            }

            if ("BIGINT".equals(typeName)
                    || "INT8".equals(typeName)
                    || "UNSIGNED BIG INT".equals(typeName)) {
                return Types.BIGINT;
            }

            if ("DATE".equals(typeName) || "DATETIME".equals(typeName)) {
                return Types.DATE;
            }

            if ("TIMESTAMP".equals(typeName)) {
                return Types.TIMESTAMP;
            }

            if (valueType == SQLITE_INTEGER
                    || "INT".equals(typeName)
                    || "INTEGER".equals(typeName)
                    || "MEDIUMINT".equals(typeName)) {
                long val = getLong(col);
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    return Types.BIGINT;
                } else {
                    return Types.INTEGER;
                }
            }
        }

        if (valueType == SQLITE_FLOAT || valueType == SQLITE_NULL) {
            if ("DECIMAL".equals(typeName)) {
                return Types.DECIMAL;
            }

            if ("DOUBLE".equals(typeName) || "DOUBLE PRECISION".equals(typeName)) {
                return Types.DOUBLE;
            }

            if ("NUMERIC".equals(typeName)) {
                return Types.NUMERIC;
            }

            if ("REAL".equals(typeName)) {
                return Types.REAL;
            }

            if (valueType == SQLITE_FLOAT || "FLOAT".equals(typeName)) {
                return Types.FLOAT;
            }
        }

        if (valueType == SQLITE_TEXT || valueType == SQLITE_NULL) {
            if ("CHARACTER".equals(typeName)
                    || "NCHAR".equals(typeName)
                    || "NATIVE CHARACTER".equals(typeName)
                    || "CHAR".equals(typeName)) {
                return Types.CHAR;
            }

            if ("CLOB".equals(typeName)) {
                return Types.CLOB;
            }

            if ("DATE".equals(typeName) || "DATETIME".equals(typeName)) {
                return Types.DATE;
            }

            if ("TIMESTAMP".equals(typeName)) {
                return Types.TIMESTAMP;
            }

            if (valueType == SQLITE_TEXT
                    || "VARCHAR".equals(typeName)
                    || "VARYING CHARACTER".equals(typeName)
                    || "NVARCHAR".equals(typeName)
                    || "TEXT".equals(typeName)) {
                return Types.VARCHAR;
            }
        }

        if (valueType == SQLITE_BLOB || valueType == SQLITE_NULL) {
            if ("BINARY".equals(typeName)) {
                return Types.BINARY;
            }

            if (valueType == SQLITE_BLOB || "BLOB".equals(typeName)) {
                return Types.BLOB;
            }
        }

        return Types.NUMERIC;
    }

    /**
     * @return The data type from either the 'create table' statement, or CAST(expr AS TYPE)
     *     otherwise sqlite3_value_type.
     * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
     */
    public String getColumnTypeName(int col) throws SQLException {
        String declType = getColumnDeclType(col);

        if (declType != null) {
            Matcher matcher = COLUMN_TYPENAME.matcher(declType);

            matcher.find();
            return matcher.group(1).toUpperCase(Locale.ENGLISH);
        }

        switch (safeGetColumnType(checkCol(col))) {
            case SQLITE_INTEGER:
                return "INTEGER";
            case SQLITE_FLOAT:
                return "FLOAT";
            case SQLITE_BLOB:
                return "BLOB";
            case SQLITE_TEXT:
                return "TEXT";
            case SQLITE_NULL:
            default:
                return "NUMERIC";
        }
    }

    /** @see java.sql.ResultSetMetaData#getPrecision(int) */
    public int getPrecision(int col) throws SQLException {
        String declType = getColumnDeclType(col);

        if (declType != null) {
            Matcher matcher = COLUMN_PRECISION.matcher(declType);

            return matcher.find() ? Integer.parseInt(matcher.group(1).split(",")[0].trim()) : 0;
        }

        return 0;
    }

    private String getColumnDeclType(int col) throws SQLException {
        String declType = stmt.pointer.safeRun((db, ptr) -> db.column_decltype(ptr, checkCol(col)));

        if (declType == null) {
            Matcher matcher = COLUMN_TYPECAST.matcher(safeGetColumnName(col));
            declType = matcher.find() ? matcher.group(1) : null;
        }

        return declType;
    }

    /** @see java.sql.ResultSetMetaData#getScale(int) */
    public int getScale(int col) throws SQLException {
        String declType = getColumnDeclType(col);

        if (declType != null) {
            Matcher matcher = COLUMN_PRECISION.matcher(declType);

            if (matcher.find()) {
                String[] array = matcher.group(1).split(",");

                if (array.length == 2) {
                    return Integer.parseInt(array[1].trim());
                }
            }
        }

        return 0;
    }

    /** @see java.sql.ResultSetMetaData#getSchemaName(int) */
    public String getSchemaName(int col) {
        return "";
    }

    /** @see java.sql.ResultSetMetaData#getTableName(int) */
    public String getTableName(int col) throws SQLException {
        final String tableName = safeGetColumnTableName(col);
        if (tableName == null) {
            // JDBC specifies an empty string instead of null
            return "";
        }
        return tableName;
    }

    /** @see java.sql.ResultSetMetaData#isNullable(int) */
    public int isNullable(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][0]
                ? ResultSetMetaData.columnNoNulls
                : ResultSetMetaData.columnNullable;
    }

    /** @see java.sql.ResultSetMetaData#isAutoIncrement(int) */
    public boolean isAutoIncrement(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][2];
    }

    /** @see java.sql.ResultSetMetaData#isCaseSensitive(int) */
    public boolean isCaseSensitive(int col) {
        return true;
    }

    /** @see java.sql.ResultSetMetaData#isCurrency(int) */
    public boolean isCurrency(int col) {
        return false;
    }

    /** @see java.sql.ResultSetMetaData#isDefinitelyWritable(int) */
    public boolean isDefinitelyWritable(int col) {
        return true;
    } // FIXME: check db file constraints?

    /** @see java.sql.ResultSetMetaData#isReadOnly(int) */
    public boolean isReadOnly(int col) {
        return false;
    }

    /** @see java.sql.ResultSetMetaData#isSearchable(int) */
    public boolean isSearchable(int col) {
        return true;
    }

    /** @see java.sql.ResultSetMetaData#isSigned(int) */
    public boolean isSigned(int col) throws SQLException {
        String typeName = getColumnTypeName(col);

        return "NUMERIC".equals(typeName) || "INTEGER".equals(typeName) || "REAL".equals(typeName);
    }

    /** @see java.sql.ResultSetMetaData#isWritable(int) */
    public boolean isWritable(int col) {
        return true;
    }

    /** @see java.sql.ResultSet#getConcurrency() */
    public int getConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    /** @see java.sql.ResultSet#rowDeleted() */
    public boolean rowDeleted() {
        return false;
    }

    /** @see java.sql.ResultSet#rowInserted() */
    public boolean rowInserted() {
        return false;
    }

    /** @see java.sql.ResultSet#rowUpdated() */
    public boolean rowUpdated() {
        return false;
    }

    /** Transforms a Julian Date to java.util.Calendar object. */
    private Calendar julianDateToCalendar(Double jd) {
        return julianDateToCalendar(jd, Calendar.getInstance());
    }

    /**
     * Transforms a Julian Date to java.util.Calendar object. Based on Guine Christian's function
     * found here:
     * http://java.ittoolbox.com/groups/technical-functional/java-l/java-function-to-convert-julian-date-to-calendar-date-1947446
     */
    private Calendar julianDateToCalendar(Double jd, Calendar cal) {
        if (jd == null) {
            return null;
        }

        int yyyy, dd, mm, hh, mn, ss, ms, A;

        double w = jd + 0.5;
        int Z = (int) w;
        double F = w - Z;

        if (Z < 2299161) {
            A = Z;
        } else {
            int alpha = (int) ((Z - 1867216.25) / 36524.25);
            A = Z + 1 + alpha - (int) (alpha / 4.0);
        }

        int B = A + 1524;
        int C = (int) ((B - 122.1) / 365.25);
        int D = (int) (365.25 * C);
        int E = (int) ((B - D) / 30.6001);

        //  month
        mm = E - ((E < 13.5) ? 1 : 13);

        // year
        yyyy = C - ((mm > 2.5) ? 4716 : 4715);

        // Day
        double jjd = B - D - (int) (30.6001 * E) + F;
        dd = (int) jjd;

        // Hour
        double hhd = jjd - dd;
        hh = (int) (24 * hhd);

        // Minutes
        double mnd = (24 * hhd) - hh;
        mn = (int) (60 * mnd);

        // Seconds
        double ssd = (60 * mnd) - mn;
        ss = (int) (60 * ssd);

        // Milliseconds
        double msd = (60 * ssd) - ss;
        ms = (int) (1000 * msd);

        cal.set(yyyy, mm - 1, dd, hh, mn, ss);
        cal.set(Calendar.MILLISECOND, ms);

        if (yyyy < 1) {
            cal.set(Calendar.ERA, GregorianCalendar.BC);
            cal.set(Calendar.YEAR, -(yyyy - 1));
        }

        return cal;
    }

    private void requireCalendarNotNull(Calendar cal) throws SQLException {
        if (cal == null) {
            throw new SQLException("Expected a calendar instance.", new IllegalArgumentException());
        }
    }

    protected int safeGetColumnType(int col) throws SQLException {
        return stmt.pointer.safeRunInt((db, ptr) -> db.column_type(ptr, col));
    }

    private long safeGetLongCol(int col) throws SQLException {
        return stmt.pointer.safeRunLong((db, ptr) -> db.column_long(ptr, markCol(col)));
    }

    private double safeGetDoubleCol(int col) throws SQLException {
        return stmt.pointer.safeRunDouble((db, ptr) -> db.column_double(ptr, markCol(col)));
    }

    private String safeGetColumnText(int col) throws SQLException {
        return stmt.pointer.safeRun((db, ptr) -> db.column_text(ptr, markCol(col)));
    }

    private String safeGetColumnTableName(int col) throws SQLException {
        return stmt.pointer.safeRun((db, ptr) -> db.column_table_name(ptr, checkCol(col)));
    }

    private String safeGetColumnName(int col) throws SQLException {
        return stmt.pointer.safeRun((db, ptr) -> db.column_name(ptr, checkCol(col)));
    }
}
