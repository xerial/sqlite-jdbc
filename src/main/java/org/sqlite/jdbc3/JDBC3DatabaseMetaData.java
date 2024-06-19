package org.sqlite.jdbc3;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConnection;
import org.sqlite.core.CoreDatabaseMetaData;
import org.sqlite.core.CoreStatement;
import org.sqlite.jdbc3.JDBC3DatabaseMetaData.ImportedKeyFinder.ForeignKey;
import org.sqlite.util.QueryUtils;
import org.sqlite.util.StringUtils;

public abstract class JDBC3DatabaseMetaData extends CoreDatabaseMetaData {

    private static String driverName;
    private static String driverVersion;

    static {
        try (InputStream sqliteJdbcPropStream =
                JDBC3DatabaseMetaData.class
                        .getClassLoader()
                        .getResourceAsStream("sqlite-jdbc.properties")) {
            if (sqliteJdbcPropStream == null) {
                throw new IOException("Cannot load sqlite-jdbc.properties from jar");
            }
            final Properties sqliteJdbcProp = new Properties();
            sqliteJdbcProp.load(sqliteJdbcPropStream);
            driverName = sqliteJdbcProp.getProperty("name");
            driverVersion = sqliteJdbcProp.getProperty("version");
        } catch (Exception e) {
            // Default values
            driverName = "SQLite JDBC";
            driverVersion = "3.0.0-UNKNOWN";
        }
    }

    protected JDBC3DatabaseMetaData(SQLiteConnection conn) {
        super(conn);
    }

    /** @see java.sql.DatabaseMetaData#getConnection() */
    public Connection getConnection() {
        return conn;
    }

    /** @see java.sql.DatabaseMetaData#getDatabaseMajorVersion() */
    public int getDatabaseMajorVersion() throws SQLException {
        return Integer.parseInt(conn.libversion().split("\\.")[0]);
    }

    /** @see java.sql.DatabaseMetaData#getDatabaseMinorVersion() */
    public int getDatabaseMinorVersion() throws SQLException {
        return Integer.parseInt(conn.libversion().split("\\.")[1]);
    }

    /** @see java.sql.DatabaseMetaData#getDriverMajorVersion() */
    public int getDriverMajorVersion() {
        return Integer.parseInt(driverVersion.split("\\.")[0]);
    }

    /** @see java.sql.DatabaseMetaData#getDriverMinorVersion() */
    public int getDriverMinorVersion() {
        return Integer.parseInt(driverVersion.split("\\.")[1]);
    }

    /** @see java.sql.DatabaseMetaData#getJDBCMajorVersion() */
    public int getJDBCMajorVersion() {
        return 4;
    }

    /** @see java.sql.DatabaseMetaData#getJDBCMinorVersion() */
    public int getJDBCMinorVersion() {
        return 2;
    }

    /** @see java.sql.DatabaseMetaData#getDefaultTransactionIsolation() */
    public int getDefaultTransactionIsolation() {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    /** @see java.sql.DatabaseMetaData#getMaxBinaryLiteralLength() */
    public int getMaxBinaryLiteralLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxCatalogNameLength() */
    public int getMaxCatalogNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxCharLiteralLength() */
    public int getMaxCharLiteralLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnNameLength() */
    public int getMaxColumnNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnsInGroupBy() */
    public int getMaxColumnsInGroupBy() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnsInIndex() */
    public int getMaxColumnsInIndex() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnsInOrderBy() */
    public int getMaxColumnsInOrderBy() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnsInSelect() */
    public int getMaxColumnsInSelect() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxColumnsInTable() */
    public int getMaxColumnsInTable() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxConnections() */
    public int getMaxConnections() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxCursorNameLength() */
    public int getMaxCursorNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxIndexLength() */
    public int getMaxIndexLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxProcedureNameLength() */
    public int getMaxProcedureNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxRowSize() */
    public int getMaxRowSize() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxSchemaNameLength() */
    public int getMaxSchemaNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxStatementLength() */
    public int getMaxStatementLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxStatements() */
    public int getMaxStatements() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxTableNameLength() */
    public int getMaxTableNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxTablesInSelect() */
    public int getMaxTablesInSelect() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getMaxUserNameLength() */
    public int getMaxUserNameLength() {
        return 0;
    }

    /** @see java.sql.DatabaseMetaData#getResultSetHoldability() */
    public int getResultSetHoldability() {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /** @see java.sql.DatabaseMetaData#getSQLStateType() */
    public int getSQLStateType() {
        return DatabaseMetaData.sqlStateSQL99;
    }

    /** @see java.sql.DatabaseMetaData#getDatabaseProductName() */
    public String getDatabaseProductName() {
        return "SQLite";
    }

    /** @see java.sql.DatabaseMetaData#getDatabaseProductVersion() */
    public String getDatabaseProductVersion() throws SQLException {
        return conn.libversion();
    }

    /** @see java.sql.DatabaseMetaData#getDriverName() */
    public String getDriverName() {
        return driverName;
    }

    /** @see java.sql.DatabaseMetaData#getDriverVersion() */
    public String getDriverVersion() {
        return driverVersion;
    }

    /** @see java.sql.DatabaseMetaData#getExtraNameCharacters() */
    public String getExtraNameCharacters() {
        return "";
    }

    /** @see java.sql.DatabaseMetaData#getCatalogSeparator() */
    public String getCatalogSeparator() {
        return ".";
    }

    /** @see java.sql.DatabaseMetaData#getCatalogTerm() */
    public String getCatalogTerm() {
        return "catalog";
    }

    /** @see java.sql.DatabaseMetaData#getSchemaTerm() */
    public String getSchemaTerm() {
        return "schema";
    }

    /** @see java.sql.DatabaseMetaData#getProcedureTerm() */
    public String getProcedureTerm() {
        return "not_implemented";
    }

    /** @see java.sql.DatabaseMetaData#getSearchStringEscape() */
    public String getSearchStringEscape() {
        return "\\";
    }

    /** @see java.sql.DatabaseMetaData#getIdentifierQuoteString() */
    public String getIdentifierQuoteString() {
        return "\"";
    }

    /**
     * @see java.sql.DatabaseMetaData#getSQLKeywords()
     * @see <a href="https://www.sqlite.org/lang_keywords.html">SQLite Keywords</a>
     */
    public String getSQLKeywords() {
        return "ABORT,ACTION,AFTER,ANALYZE,ATTACH,AUTOINCREMENT,BEFORE,"
                + "CASCADE,CONFLICT,DATABASE,DEFERRABLE,DEFERRED,DESC,DETACH,"
                + "EXCLUSIVE,EXPLAIN,FAIL,GLOB,IGNORE,INDEX,INDEXED,INITIALLY,INSTEAD,ISNULL,"
                + "KEY,LIMIT,NOTNULL,OFFSET,PLAN,PRAGMA,QUERY,"
                + "RAISE,REGEXP,REINDEX,RENAME,REPLACE,RESTRICT,"
                + "TEMP,TEMPORARY,TRANSACTION,VACUUM,VIEW,VIRTUAL";
    }

    /** @see java.sql.DatabaseMetaData#getNumericFunctions() */
    public String getNumericFunctions() {
        return "";
    }

    /** @see java.sql.DatabaseMetaData#getStringFunctions() */
    public String getStringFunctions() {
        return "";
    }

    /** @see java.sql.DatabaseMetaData#getSystemFunctions() */
    public String getSystemFunctions() {
        return "";
    }

    /** @see java.sql.DatabaseMetaData#getTimeDateFunctions() */
    public String getTimeDateFunctions() {
        return "DATE,TIME,DATETIME,JULIANDAY,STRFTIME";
    }

    /** @see java.sql.DatabaseMetaData#getURL() */
    public String getURL() {
        return conn.getUrl();
    }

    /** @see java.sql.DatabaseMetaData#getUserName() */
    public String getUserName() {
        return null;
    }

    /** @see java.sql.DatabaseMetaData#allProceduresAreCallable() */
    public boolean allProceduresAreCallable() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#allTablesAreSelectable() */
    public boolean allTablesAreSelectable() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit() */
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#dataDefinitionIgnoredInTransactions() */
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#doesMaxRowSizeIncludeBlobs() */
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#deletesAreDetected(int) */
    public boolean deletesAreDetected(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#insertsAreDetected(int) */
    public boolean insertsAreDetected(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#isCatalogAtStart() */
    public boolean isCatalogAtStart() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#locatorsUpdateCopy() */
    public boolean locatorsUpdateCopy() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#nullPlusNonNullIsNull() */
    public boolean nullPlusNonNullIsNull() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#nullsAreSortedAtEnd() */
    public boolean nullsAreSortedAtEnd() {
        return !nullsAreSortedAtStart();
    }

    /** @see java.sql.DatabaseMetaData#nullsAreSortedAtStart() */
    public boolean nullsAreSortedAtStart() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#nullsAreSortedHigh() */
    public boolean nullsAreSortedHigh() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#nullsAreSortedLow() */
    public boolean nullsAreSortedLow() {
        return !nullsAreSortedHigh();
    }

    /** @see java.sql.DatabaseMetaData#othersDeletesAreVisible(int) */
    public boolean othersDeletesAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#othersInsertsAreVisible(int) */
    public boolean othersInsertsAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#othersUpdatesAreVisible(int) */
    public boolean othersUpdatesAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#ownDeletesAreVisible(int) */
    public boolean ownDeletesAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#ownInsertsAreVisible(int) */
    public boolean ownInsertsAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#ownUpdatesAreVisible(int) */
    public boolean ownUpdatesAreVisible(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#storesLowerCaseIdentifiers() */
    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#storesLowerCaseQuotedIdentifiers() */
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#storesMixedCaseIdentifiers() */
    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#storesMixedCaseQuotedIdentifiers() */
    public boolean storesMixedCaseQuotedIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#storesUpperCaseIdentifiers() */
    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#storesUpperCaseQuotedIdentifiers() */
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsAlterTableWithAddColumn() */
    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsAlterTableWithDropColumn() */
    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsANSI92EntryLevelSQL() */
    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsANSI92FullSQL() */
    public boolean supportsANSI92FullSQL() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsANSI92IntermediateSQL() */
    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsBatchUpdates() */
    public boolean supportsBatchUpdates() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsCatalogsInDataManipulation() */
    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsCatalogsInIndexDefinitions() */
    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions() */
    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsCatalogsInProcedureCalls() */
    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsCatalogsInTableDefinitions() */
    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsColumnAliasing() */
    public boolean supportsColumnAliasing() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsConvert() */
    public boolean supportsConvert() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsConvert(int, int) */
    public boolean supportsConvert(int fromType, int toType) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsCorrelatedSubqueries() */
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsDataDefinitionAndDataManipulationTransactions() */
    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsDataManipulationTransactionsOnly() */
    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsDifferentTableCorrelationNames() */
    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsExpressionsInOrderBy() */
    public boolean supportsExpressionsInOrderBy() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsMinimumSQLGrammar() */
    public boolean supportsMinimumSQLGrammar() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsCoreSQLGrammar() */
    public boolean supportsCoreSQLGrammar() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsExtendedSQLGrammar() */
    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsLimitedOuterJoins() */
    public boolean supportsLimitedOuterJoins() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsFullOuterJoins() */
    public boolean supportsFullOuterJoins() throws SQLException {
        String[] version = conn.libversion().split("\\.");
        return Integer.parseInt(version[0]) >= 3 && Integer.parseInt(version[1]) >= 39;
    }

    /** @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys() */
    public boolean supportsGetGeneratedKeys() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsGroupBy() */
    public boolean supportsGroupBy() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsGroupByBeyondSelect() */
    public boolean supportsGroupByBeyondSelect() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsGroupByUnrelated() */
    public boolean supportsGroupByUnrelated() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsIntegrityEnhancementFacility() */
    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsLikeEscapeClause() */
    public boolean supportsLikeEscapeClause() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsMixedCaseIdentifiers() */
    public boolean supportsMixedCaseIdentifiers() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsMixedCaseQuotedIdentifiers() */
    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsMultipleOpenResults() */
    public boolean supportsMultipleOpenResults() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsMultipleResultSets() */
    public boolean supportsMultipleResultSets() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsMultipleTransactions() */
    public boolean supportsMultipleTransactions() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsNamedParameters() */
    public boolean supportsNamedParameters() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsNonNullableColumns() */
    public boolean supportsNonNullableColumns() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossCommit() */
    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossRollback() */
    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossCommit() */
    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossRollback() */
    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsOrderByUnrelated() */
    public boolean supportsOrderByUnrelated() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsOuterJoins() */
    public boolean supportsOuterJoins() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsPositionedDelete() */
    public boolean supportsPositionedDelete() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsPositionedUpdate() */
    public boolean supportsPositionedUpdate() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsResultSetConcurrency(int, int) */
    public boolean supportsResultSetConcurrency(int t, int c) {
        return t == ResultSet.TYPE_FORWARD_ONLY && c == ResultSet.CONCUR_READ_ONLY;
    }

    /** @see java.sql.DatabaseMetaData#supportsResultSetHoldability(int) */
    public boolean supportsResultSetHoldability(int h) {
        return h == ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /** @see java.sql.DatabaseMetaData#supportsResultSetType(int) */
    public boolean supportsResultSetType(int t) {
        return t == ResultSet.TYPE_FORWARD_ONLY;
    }

    /** @see java.sql.DatabaseMetaData#supportsSavepoints() */
    public boolean supportsSavepoints() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsSchemasInDataManipulation() */
    public boolean supportsSchemasInDataManipulation() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSchemasInIndexDefinitions() */
    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSchemasInPrivilegeDefinitions() */
    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSchemasInProcedureCalls() */
    public boolean supportsSchemasInProcedureCalls() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSchemasInTableDefinitions() */
    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSelectForUpdate() */
    public boolean supportsSelectForUpdate() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsStatementPooling() */
    public boolean supportsStatementPooling() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsStoredProcedures() */
    public boolean supportsStoredProcedures() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSubqueriesInComparisons() */
    public boolean supportsSubqueriesInComparisons() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsSubqueriesInExists() */
    public boolean supportsSubqueriesInExists() {
        return true;
    } // TODO: check

    /** @see java.sql.DatabaseMetaData#supportsSubqueriesInIns() */
    public boolean supportsSubqueriesInIns() {
        return true;
    } // TODO: check

    /** @see java.sql.DatabaseMetaData#supportsSubqueriesInQuantifieds() */
    public boolean supportsSubqueriesInQuantifieds() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsTableCorrelationNames() */
    public boolean supportsTableCorrelationNames() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#supportsTransactionIsolationLevel(int) */
    public boolean supportsTransactionIsolationLevel(int level) {
        return level == Connection.TRANSACTION_SERIALIZABLE;
    }

    /** @see java.sql.DatabaseMetaData#supportsTransactions() */
    public boolean supportsTransactions() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsUnion() */
    public boolean supportsUnion() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#supportsUnionAll() */
    public boolean supportsUnionAll() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#updatesAreDetected(int) */
    public boolean updatesAreDetected(int type) {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#usesLocalFilePerTable() */
    public boolean usesLocalFilePerTable() {
        return false;
    }

    /** @see java.sql.DatabaseMetaData#usesLocalFiles() */
    public boolean usesLocalFiles() {
        return true;
    }

    /** @see java.sql.DatabaseMetaData#isReadOnly() */
    public boolean isReadOnly() throws SQLException {
        return conn.isReadOnly();
    }

    /**
     * @see java.sql.DatabaseMetaData#getAttributes(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String)
     */
    public ResultSet getAttributes(String c, String s, String t, String a) throws SQLException {
        if (getAttributes == null) {
            getAttributes =
                    conn.prepareStatement(
                            "select null as TYPE_CAT, null as TYPE_SCHEM, "
                                    + "null as TYPE_NAME, null as ATTR_NAME, null as DATA_TYPE, "
                                    + "null as ATTR_TYPE_NAME, null as ATTR_SIZE, null as DECIMAL_DIGITS, "
                                    + "null as NUM_PREC_RADIX, null as NULLABLE, null as REMARKS, null as ATTR_DEF, "
                                    + "null as SQL_DATA_TYPE, null as SQL_DATETIME_SUB, null as CHAR_OCTET_LENGTH, "
                                    + "null as ORDINAL_POSITION, null as IS_NULLABLE, null as SCOPE_CATALOG, "
                                    + "null as SCOPE_SCHEMA, null as SCOPE_TABLE, null as SOURCE_DATA_TYPE limit 0;");
        }

        return getAttributes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getBestRowIdentifier(java.lang.String, java.lang.String,
     *     java.lang.String, int, boolean)
     */
    public ResultSet getBestRowIdentifier(String c, String s, String t, int scope, boolean n)
            throws SQLException {
        if (getBestRowIdentifier == null) {
            getBestRowIdentifier =
                    conn.prepareStatement(
                            "select null as SCOPE, null as COLUMN_NAME, "
                                    + "null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, "
                                    + "null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }

        return getBestRowIdentifier.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getColumnPrivileges(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String)
     */
    public ResultSet getColumnPrivileges(String c, String s, String t, String colPat)
            throws SQLException {
        if (getColumnPrivileges == null) {
            getColumnPrivileges =
                    conn.prepareStatement(
                            "select null as TABLE_CAT, null as TABLE_SCHEM, "
                                    + "null as TABLE_NAME, null as COLUMN_NAME, null as GRANTOR, null as GRANTEE, "
                                    + "null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }

        return getColumnPrivileges.executeQuery();
    }

    // Column type patterns
    protected static final Pattern TYPE_INTEGER = Pattern.compile(".*(INT|BOOL).*");
    protected static final Pattern TYPE_VARCHAR = Pattern.compile(".*(CHAR|CLOB|TEXT|BLOB).*");
    protected static final Pattern TYPE_FLOAT = Pattern.compile(".*(REAL|FLOA|DOUB|DEC|NUM).*");

    /**
     * @see java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String)
     */
    public ResultSet getColumns(String c, String s, String tblNamePattern, String colNamePattern)
            throws SQLException {

        // get the list of tables matching the pattern (getTables)
        // create a Matrix Cursor for each of the tables
        // create a merge cursor from all the Matrix Cursors
        // and return the columname and type from:
        //    "PRAGMA table_xinfo(tablename)"
        // which returns data like this:
        //        sqlite> PRAGMA lastyear.table_info(gross_sales);
        //        cid|name|type|notnull|dflt_value|pk
        //        0|year|INTEGER|0|'2006'|0
        //        1|month|TEXT|0||0
        //        2|monthlygross|REAL|0||0
        //        3|sortcol|INTEGER|0||0
        //        sqlite>

        // and then make the cursor have these columns
        //        TABLE_CAT String => table catalog (may be null)
        //        TABLE_SCHEM String => table schema (may be null)
        //        TABLE_NAME String => table name
        //        COLUMN_NAME String => column name
        //        DATA_TYPE int => SQL type from java.sql.Types
        //        TYPE_NAME String => Data source dependent type name, for a UDT the type name is
        // fully qualified
        //        COLUMN_SIZE int => column size.
        //        BUFFER_LENGTH is not used.
        //        DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data
        // types where DECIMAL_DIGITS is not applicable.
        //        NUM_PREC_RADIX int => Radix (typically either 10 or 2)
        //        NULLABLE int => is NULL allowed.
        //        columnNoNulls - might not allow NULL values
        //        columnNullable - definitely allows NULL values
        //        columnNullableUnknown - nullability unknown
        //        REMARKS String => comment describing column (may be null)
        //        COLUMN_DEF String => default value for the column, which should be interpreted as
        // a string when the value is enclosed in single quotes (may be null)
        //        SQL_DATA_TYPE int => unused
        //        SQL_DATETIME_SUB int => unused
        //        CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
        //        ORDINAL_POSITION int => index of column in table (starting at 1)
        //        IS_NULLABLE String => ISO rules are used to determine the nullability for a
        // column.
        //        YES --- if the parameter can include NULLs
        //        NO --- if the parameter cannot include NULLs
        //        empty string --- if the nullability for the parameter is unknown
        //        SCOPE_CATALOG String => catalog of table that is the scope of a reference
        // attribute
        // (null if DATA_TYPE isn't REF)
        //        SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute
        // (null if the DATA_TYPE isn't REF)
        //        SCOPE_TABLE String => table name that this the scope of a reference attribute
        // (null if the DATA_TYPE isn't REF)
        //        SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref
        // type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated
        // REF)
        //        IS_AUTOINCREMENT String => Indicates whether this column is auto incremented
        //        YES --- if the column is auto incremented
        //        NO --- if the column is not auto incremented
        //        empty string --- if it cannot be determined whether the column is auto incremented
        // parameter is unknown
        //        IS_GENERATEDCOLUMN String => Indicates whether this column is auto incremented
        //        YES --- if the column is generated
        //        NO --- if the column is not generated
        //        empty string --- if it cannot be determined whether the column is auto incremented
        // parameter is unknown
        checkOpen();

        StringBuilder sql = new StringBuilder(700);
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, tblname as TABLE_NAME, ")
                .append(
                        "cn as COLUMN_NAME, ct as DATA_TYPE, tn as TYPE_NAME, colSize as COLUMN_SIZE, ")
                .append(
                        "2000000000 as BUFFER_LENGTH, colDecimalDigits as DECIMAL_DIGITS, 10   as NUM_PREC_RADIX, ")
                .append("colnullable as NULLABLE, null as REMARKS, colDefault as COLUMN_DEF, ")
                .append(
                        "0    as SQL_DATA_TYPE, 0    as SQL_DATETIME_SUB, 2000000000 as CHAR_OCTET_LENGTH, ")
                .append(
                        "ordpos as ORDINAL_POSITION, (case colnullable when 0 then 'NO' when 1 then 'YES' else '' end)")
                .append("    as IS_NULLABLE, null as SCOPE_CATALOG, null as SCOPE_SCHEMA, ")
                .append("null as SCOPE_TABLE, null as SOURCE_DATA_TYPE, ")
                .append(
                        "(case colautoincrement when 0 then 'NO' when 1 then 'YES' else '' end) as IS_AUTOINCREMENT, ")
                .append(
                        "(case colgenerated when 0 then 'NO' when 1 then 'YES' else '' end) as IS_GENERATEDCOLUMN from (");

        boolean colFound = false;

        ResultSet rs = null;
        try {
            // Get all tables implied by the input
            rs = getTables(c, s, tblNamePattern, null);
            while (rs.next()) {
                String tableName = rs.getString(3);

                boolean isAutoIncrement;

                Statement statColAutoinc = conn.createStatement();
                ResultSet rsColAutoinc = null;
                try {
                    statColAutoinc = conn.createStatement();
                    rsColAutoinc =
                            statColAutoinc.executeQuery(
                                    "SELECT LIKE('%autoincrement%', LOWER(sql)) FROM sqlite_schema "
                                            + "WHERE LOWER(name) = LOWER('"
                                            + escape(tableName)
                                            + "') AND TYPE IN ('table', 'view')");
                    rsColAutoinc.next();
                    isAutoIncrement = rsColAutoinc.getInt(1) == 1;
                } finally {
                    if (rsColAutoinc != null) {
                        try {
                            rsColAutoinc.close();
                        } catch (Exception e) {
                            LogHolder.logger.error("Could not close ResultSet", e);
                        }
                    }
                    if (statColAutoinc != null) {
                        try {
                            statColAutoinc.close();
                        } catch (Exception e) {
                            LogHolder.logger.error("Could not close statement", e);
                        }
                    }
                }

                // For each table, get the column info and build into overall SQL
                String pragmaStatement = "PRAGMA table_xinfo('" + escape(tableName) + "')";
                try (Statement colstat = conn.createStatement();
                        ResultSet rscol = colstat.executeQuery(pragmaStatement)) {

                    for (int i = 0; rscol.next(); i++) {
                        String colName = rscol.getString(2);
                        String colType = rscol.getString(3);
                        String colNotNull = rscol.getString(4);
                        String colDefault = rscol.getString(5);
                        boolean isPk = "1".equals(rscol.getString(6));
                        String colHidden = rscol.getString(7);

                        int colNullable = 2;
                        if (colNotNull != null) {
                            colNullable = colNotNull.equals("0") ? 1 : 0;
                        }

                        if (colFound) {
                            sql.append(" union all ");
                        }
                        colFound = true;

                        // default values
                        int iColumnSize = 2000000000;
                        int iDecimalDigits = 10;

                        /*
                         * improved column types
                         * ref https://www.sqlite.org/datatype3.html - 2.1 Determination Of Column Affinity
                         * plus some degree of artistic-license applied
                         */
                        colType = colType == null ? "TEXT" : colType.toUpperCase();

                        int colAutoIncrement = 0;
                        if (isPk && isAutoIncrement) {
                            colAutoIncrement = 1;
                        }
                        int colJavaType;
                        // rule #1 + boolean
                        if (TYPE_INTEGER.matcher(colType).find()) {
                            colJavaType = Types.INTEGER;
                            // there are no decimal digits
                            iDecimalDigits = 0;
                        } else if (TYPE_VARCHAR.matcher(colType).find()) {
                            colJavaType = Types.VARCHAR;
                            // there are no decimal digits
                            iDecimalDigits = 0;
                        } else if (TYPE_FLOAT.matcher(colType).find()) {
                            colJavaType = Types.FLOAT;
                        } else {
                            // catch-all
                            colJavaType = Types.VARCHAR;
                        }
                        // try to find an (optional) length/dimension of the column
                        int iStartOfDimension = colType.indexOf('(');
                        if (iStartOfDimension > 0) {
                            // find end of dimension
                            int iEndOfDimension = colType.indexOf(')', iStartOfDimension);
                            if (iEndOfDimension > 0) {
                                String sInteger, sDecimal;
                                // check for two values (integer part, fraction) divided by
                                // comma
                                int iDimensionSeparator = colType.indexOf(',', iStartOfDimension);
                                if (iDimensionSeparator > 0) {
                                    sInteger =
                                            colType.substring(
                                                    iStartOfDimension + 1, iDimensionSeparator);
                                    sDecimal =
                                            colType.substring(
                                                    iDimensionSeparator + 1, iEndOfDimension);
                                }
                                // only a single dimension
                                else {
                                    sInteger =
                                            colType.substring(
                                                    iStartOfDimension + 1, iEndOfDimension);
                                    sDecimal = null;
                                }
                                // try to parse the values
                                try {
                                    int iInteger = Integer.parseUnsignedInt(sInteger);
                                    // parse decimals?
                                    if (sDecimal != null) {
                                        iDecimalDigits = Integer.parseUnsignedInt(sDecimal);
                                        // columns size equals sum of integer and decimal part
                                        // of dimension
                                        iColumnSize = iInteger + iDecimalDigits;
                                    } else {
                                        // no decimals
                                        iDecimalDigits = 0;
                                        // columns size equals dimension
                                        iColumnSize = iInteger;
                                    }
                                } catch (NumberFormatException ex) {
                                    // just ignore invalid dimension formats here
                                }
                            }
                            // "TYPE_NAME" (colType) is without the length/ dimension
                            colType = colType.substring(0, iStartOfDimension).trim();
                        }

                        int colGenerated = 0;
                        if ("2".equals(colHidden) || "3".equals(colHidden)) {
                            colGenerated = 1;
                        }

                        sql.append("select ")
                                .append(i + 1)
                                .append(" as ordpos, ")
                                .append(colNullable)
                                .append(" as colnullable,")
                                .append(colJavaType)
                                .append(" as ct, ")
                                .append(iColumnSize)
                                .append(" as colSize, ")
                                .append(iDecimalDigits)
                                .append(" as colDecimalDigits, ")
                                .append("'")
                                .append(tableName)
                                .append("' as tblname, ")
                                .append("'")
                                .append(escape(colName))
                                .append("' as cn, ")
                                .append("'")
                                .append(escape(colType))
                                .append("' as tn, ")
                                .append(quote(colDefault == null ? null : escape(colDefault)))
                                .append(" as colDefault,")
                                .append(colAutoIncrement)
                                .append(" as colautoincrement,")
                                .append(colGenerated)
                                .append(" as colgenerated");

                        if (colNamePattern != null) {
                            sql.append(" where upper(cn) like upper('")
                                    .append(escape(colNamePattern))
                                    .append("') ESCAPE '")
                                    .append(getSearchStringEscape())
                                    .append("'");
                        }
                    }
                }
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    LogHolder.logger.error("Could not close ResultSet", e);
                }
            }
        }

        if (colFound) {
            sql.append(") order by TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION;");
        } else {
            sql.append(
                    "select null as ordpos, null as colnullable, null as ct, null as colsize, null as colDecimalDigits, null as tblname, null as cn, null as tn, null as colDefault, null as colautoincrement, null as colgenerated) limit 0;");
        }

        Statement stat = conn.createStatement();
        return ((CoreStatement) stat).executeQuery(sql.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getCrossReference(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public ResultSet getCrossReference(
            String pc, String ps, String pt, String fc, String fs, String ft) throws SQLException {
        if (pt == null) {
            return getExportedKeys(fc, fs, ft);
        }

        if (ft == null) {
            return getImportedKeys(pc, ps, pt);
        }

        String query =
                "select "
                        + quote(pc)
                        + " as PKTABLE_CAT, "
                        + quote(ps)
                        + " as PKTABLE_SCHEM, "
                        + quote(pt)
                        + " as PKTABLE_NAME, "
                        + "'' as PKCOLUMN_NAME, "
                        + quote(fc)
                        + " as FKTABLE_CAT, "
                        + quote(fs)
                        + " as FKTABLE_SCHEM, "
                        + quote(ft)
                        + " as FKTABLE_NAME, "
                        + "'' as FKCOLUMN_NAME, -1 as KEY_SEQ, 3 as UPDATE_RULE, 3 as DELETE_RULE, '' as FK_NAME, '' as PK_NAME, "
                        + DatabaseMetaData.importedKeyInitiallyDeferred
                        + " as DEFERRABILITY limit 0 ";

        return ((CoreStatement) conn.createStatement()).executeQuery(query, true);
    }

    /** @see java.sql.DatabaseMetaData#getSchemas() */
    public ResultSet getSchemas() throws SQLException {
        if (getSchemas == null) {
            getSchemas =
                    conn.prepareStatement(
                            "select null as TABLE_SCHEM, null as TABLE_CATALOG limit 0;");
        }

        return getSchemas.executeQuery();
    }

    /** @see java.sql.DatabaseMetaData#getCatalogs() */
    public ResultSet getCatalogs() throws SQLException {
        if (getCatalogs == null) {
            getCatalogs = conn.prepareStatement("select null as TABLE_CAT limit 0;");
        }

        return getCatalogs.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getPrimaryKeys(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getPrimaryKeys(String c, String s, String table) throws SQLException {
        PrimaryKeyFinder pkFinder = new PrimaryKeyFinder(table);
        String[] columns = pkFinder.getColumns();

        Statement stat = conn.createStatement();
        StringBuilder sql = new StringBuilder(512);
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, '")
                .append(escape(table))
                .append("' as TABLE_NAME, cn as COLUMN_NAME, ks as KEY_SEQ, pk as PK_NAME from (");

        if (columns == null) {
            sql.append("select null as cn, null as pk, 0 as ks) limit 0;");

            return ((CoreStatement) stat).executeQuery(sql.toString(), true);
        }

        String pkName = pkFinder.getName();
        if (pkName != null) {
            pkName = "'" + pkName + "'";
        }

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sql.append(" union ");
            sql.append("select ")
                    .append(pkName)
                    .append(" as pk, '")
                    .append(escape(unquoteIdentifier(columns[i])))
                    .append("' as cn, ")
                    .append(i + 1)
                    .append(" as ks");
        }

        return ((CoreStatement) stat).executeQuery(sql.append(") order by cn;").toString(), true);
    }

    private static final Map<String, Integer> RULE_MAP = new HashMap<>();

    static {
        RULE_MAP.put("NO ACTION", DatabaseMetaData.importedKeyNoAction);
        RULE_MAP.put("CASCADE", DatabaseMetaData.importedKeyCascade);
        RULE_MAP.put("RESTRICT", DatabaseMetaData.importedKeyRestrict);
        RULE_MAP.put("SET NULL", DatabaseMetaData.importedKeySetNull);
        RULE_MAP.put("SET DEFAULT", DatabaseMetaData.importedKeySetDefault);
    }

    /**
     * @see java.sql.DatabaseMetaData#getExportedKeys(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getExportedKeys(String catalog, String schema, String table)
            throws SQLException {
        PrimaryKeyFinder pkFinder = new PrimaryKeyFinder(table);
        String[] pkColumns = pkFinder.getColumns();
        Statement stat = conn.createStatement();

        catalog = (catalog != null) ? quote(catalog) : null;
        schema = (schema != null) ? quote(schema) : null;

        StringBuilder exportedKeysQuery = new StringBuilder(512);

        String target = null;
        int count = 0;
        if (pkColumns != null) {
            // retrieve table list
            ArrayList<String> tableList;
            try (ResultSet rs =
                    stat.executeQuery("select name from sqlite_schema where type = 'table'")) {
                tableList = new ArrayList<>();

                while (rs.next()) {
                    String tblname = rs.getString(1);
                    tableList.add(tblname);
                    if (tblname.equalsIgnoreCase(table)) {
                        // get the correct case as in the database
                        // (not uppercase nor lowercase)
                        target = tblname;
                    }
                }
            }

            // find imported keys for each table
            for (String tbl : tableList) {
                final ImportedKeyFinder impFkFinder = new ImportedKeyFinder(tbl);
                List<ForeignKey> fkNames = impFkFinder.getFkList();

                for (ForeignKey foreignKey : fkNames) {
                    String PKTabName = foreignKey.getPkTableName();

                    if (PKTabName == null || !PKTabName.equalsIgnoreCase(target)) {
                        continue;
                    }

                    for (int j = 0; j < foreignKey.getColumnMappingCount(); j++) {
                        int keySeq = j + 1;
                        String[] columnMapping = foreignKey.getColumnMapping(j);
                        String PKColName = columnMapping[1];
                        PKColName = (PKColName == null) ? "" : PKColName;
                        String FKColName = columnMapping[0];
                        FKColName = (FKColName == null) ? "" : FKColName;

                        boolean usePkName = false;
                        for (String pkColumn : pkColumns) {
                            if (pkColumn != null && pkColumn.equalsIgnoreCase(PKColName)) {
                                usePkName = true;
                                break;
                            }
                        }
                        String pkName =
                                (usePkName && pkFinder.getName() != null) ? pkFinder.getName() : "";

                        exportedKeysQuery
                                .append(count > 0 ? " union all select " : "select ")
                                .append(keySeq)
                                .append(" as ks, '")
                                .append(escape(tbl))
                                .append("' as fkt, '")
                                .append(escape(FKColName))
                                .append("' as fcn, '")
                                .append(escape(PKColName))
                                .append("' as pcn, '")
                                .append(escape(pkName))
                                .append("' as pkn, ")
                                .append(RULE_MAP.get(foreignKey.getOnUpdate()))
                                .append(" as ur, ")
                                .append(RULE_MAP.get(foreignKey.getOnDelete()))
                                .append(" as dr, ");

                        String fkName = foreignKey.getFkName();

                        if (fkName != null) {
                            exportedKeysQuery.append("'").append(escape(fkName)).append("' as fkn");
                        } else {
                            exportedKeysQuery.append("'' as fkn");
                        }

                        count++;
                    }
                }
            }
        }

        boolean hasImportedKey = (count > 0);
        StringBuilder sql = new StringBuilder(512);
        sql.append("select ")
                .append(catalog)
                .append(" as PKTABLE_CAT, ")
                .append(schema)
                .append(" as PKTABLE_SCHEM, ")
                .append(quote(target))
                .append(" as PKTABLE_NAME, ")
                .append(hasImportedKey ? "pcn" : "''")
                .append(" as PKCOLUMN_NAME, ")
                .append(catalog)
                .append(" as FKTABLE_CAT, ")
                .append(schema)
                .append(" as FKTABLE_SCHEM, ")
                .append(hasImportedKey ? "fkt" : "''")
                .append(" as FKTABLE_NAME, ")
                .append(hasImportedKey ? "fcn" : "''")
                .append(" as FKCOLUMN_NAME, ")
                .append(hasImportedKey ? "ks" : "-1")
                .append(" as KEY_SEQ, ")
                .append(hasImportedKey ? "ur" : "3")
                .append(" as UPDATE_RULE, ")
                .append(hasImportedKey ? "dr" : "3")
                .append(" as DELETE_RULE, ")
                .append(hasImportedKey ? "fkn" : "''")
                .append(" as FK_NAME, ")
                .append(hasImportedKey ? "pkn" : "''")
                .append(" as PK_NAME, ")
                .append(DatabaseMetaData.importedKeyInitiallyDeferred) // FIXME: Check for pragma
                // foreign_keys = true ?
                .append(" as DEFERRABILITY ");

        if (hasImportedKey) {
            sql.append("from (")
                    .append(exportedKeysQuery)
                    .append(") ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, KEY_SEQ");
        } else {
            sql.append("limit 0");
        }

        return ((CoreStatement) stat).executeQuery(sql.toString(), true);
    }

    private StringBuilder appendDummyForeignKeyList(StringBuilder sql) {
        sql.append("select -1 as ks, '' as ptn, '' as fcn, '' as pcn, ")
                .append(DatabaseMetaData.importedKeyNoAction)
                .append(" as ur, ")
                .append(DatabaseMetaData.importedKeyNoAction)
                .append(" as dr, ")
                .append(" '' as fkn, ")
                .append(" '' as pkn ")
                .append(") limit 0;");
        return sql;
    }

    /**
     * @see java.sql.DatabaseMetaData#getImportedKeys(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getImportedKeys(String catalog, String schema, String table)
            throws SQLException {
        ResultSet rs;
        Statement stat = conn.createStatement();
        StringBuilder sql = new StringBuilder(700);

        sql.append("select ")
                .append(quote(catalog))
                .append(" as PKTABLE_CAT, ")
                .append(quote(schema))
                .append(" as PKTABLE_SCHEM, ")
                .append("ptn as PKTABLE_NAME, pcn as PKCOLUMN_NAME, ")
                .append(quote(catalog))
                .append(" as FKTABLE_CAT, ")
                .append(quote(schema))
                .append(" as FKTABLE_SCHEM, ")
                .append(quote(table))
                .append(" as FKTABLE_NAME, ")
                .append(
                        "fcn as FKCOLUMN_NAME, ks as KEY_SEQ, ur as UPDATE_RULE, dr as DELETE_RULE, fkn as FK_NAME, pkn as PK_NAME, ")
                .append(DatabaseMetaData.importedKeyInitiallyDeferred)
                .append(" as DEFERRABILITY from (");

        // Use a try catch block to avoid "query does not return ResultSet" error
        try {
            rs = stat.executeQuery("pragma foreign_key_list('" + escape(table) + "');");
        } catch (SQLException e) {
            sql = appendDummyForeignKeyList(sql);
            return ((CoreStatement) stat).executeQuery(sql.toString(), true);
        }

        final ImportedKeyFinder impFkFinder = new ImportedKeyFinder(table);
        List<ForeignKey> fkNames = impFkFinder.getFkList();

        int i = 0;
        for (; rs.next(); i++) {
            int keySeq = rs.getInt(2) + 1;
            int keyId = rs.getInt(1);
            String PKTabName = rs.getString(3);
            String FKColName = rs.getString(4);
            String PKColName = rs.getString(5);

            String pkName = null;
            try {
                PrimaryKeyFinder pkFinder = new PrimaryKeyFinder(PKTabName);
                pkName = pkFinder.getName();
                if (PKColName == null) {
                    PKColName = pkFinder.getColumns()[0];
                }
            } catch (SQLException ignored) {
            }

            String updateRule = rs.getString(6);
            String deleteRule = rs.getString(7);

            if (i > 0) {
                sql.append(" union all ");
            }

            String fkName = null;
            if (fkNames.size() > keyId) fkName = fkNames.get(keyId).getFkName();

            sql.append("select ")
                    .append(keySeq)
                    .append(" as ks,")
                    .append("'")
                    .append(escape(PKTabName))
                    .append("' as ptn, '")
                    .append(escape(FKColName))
                    .append("' as fcn, '")
                    .append(escape(PKColName))
                    .append("' as pcn,")
                    .append("case '")
                    .append(escape(updateRule))
                    .append("'")
                    .append(" when 'NO ACTION' then ")
                    .append(DatabaseMetaData.importedKeyNoAction)
                    .append(" when 'CASCADE' then ")
                    .append(DatabaseMetaData.importedKeyCascade)
                    .append(" when 'RESTRICT' then ")
                    .append(DatabaseMetaData.importedKeyRestrict)
                    .append(" when 'SET NULL' then ")
                    .append(DatabaseMetaData.importedKeySetNull)
                    .append(" when 'SET DEFAULT' then ")
                    .append(DatabaseMetaData.importedKeySetDefault)
                    .append(" end as ur, ")
                    .append("case '")
                    .append(escape(deleteRule))
                    .append("'")
                    .append(" when 'NO ACTION' then ")
                    .append(DatabaseMetaData.importedKeyNoAction)
                    .append(" when 'CASCADE' then ")
                    .append(DatabaseMetaData.importedKeyCascade)
                    .append(" when 'RESTRICT' then ")
                    .append(DatabaseMetaData.importedKeyRestrict)
                    .append(" when 'SET NULL' then ")
                    .append(DatabaseMetaData.importedKeySetNull)
                    .append(" when 'SET DEFAULT' then ")
                    .append(DatabaseMetaData.importedKeySetDefault)
                    .append(" end as dr, ")
                    .append(fkName == null ? "''" : quote(fkName))
                    .append(" as fkn, ")
                    .append(pkName == null ? "''" : quote(pkName))
                    .append(" as pkn");
        }
        rs.close();

        if (i == 0) {
            sql = appendDummyForeignKeyList(sql);
        } else {
            sql.append(") ORDER BY PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, KEY_SEQ;");
        }

        return ((CoreStatement) stat).executeQuery(sql.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String,
     *     java.lang.String, boolean, boolean)
     */
    public ResultSet getIndexInfo(String c, String s, String table, boolean u, boolean approximate)
            throws SQLException {
        ResultSet rs;
        Statement stat = conn.createStatement();
        StringBuilder sql = new StringBuilder(500);

        // define the column header
        // this is from the JDBC spec, it is part of the driver protocol
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, '")
                .append(escape(table))
                .append(
                        "' as TABLE_NAME, un as NON_UNIQUE, null as INDEX_QUALIFIER, n as INDEX_NAME, ")
                .append(Integer.toString(DatabaseMetaData.tableIndexOther))
                .append(" as TYPE, op as ORDINAL_POSITION, ")
                .append(
                        "cn as COLUMN_NAME, null as ASC_OR_DESC, 0 as CARDINALITY, 0 as PAGES, null as FILTER_CONDITION from (");

        // this always returns a result set now, previously threw exception
        rs = stat.executeQuery("pragma index_list('" + escape(table) + "');");

        ArrayList<ArrayList<Object>> indexList = new ArrayList<>();
        while (rs.next()) {
            indexList.add(new ArrayList<>());
            indexList.get(indexList.size() - 1).add(rs.getString(2));
            indexList.get(indexList.size() - 1).add(rs.getInt(3));
        }
        rs.close();
        if (indexList.size() == 0) {
            // if pragma index_list() returns no information, use this null block
            sql.append("select null as un, null as n, null as op, null as cn) limit 0;");
            return ((CoreStatement) stat).executeQuery(sql.toString(), true);
        } else {
            // loop over results from pragma call, getting specific info for each index

            Iterator<ArrayList<Object>> indexIterator = indexList.iterator();
            ArrayList<Object> currentIndex;

            ArrayList<String> unionAll = new ArrayList<>();

            while (indexIterator.hasNext()) {
                currentIndex = indexIterator.next();
                String indexName = currentIndex.get(0).toString();
                rs = stat.executeQuery("pragma index_info('" + escape(indexName) + "');");

                while (rs.next()) {

                    StringBuilder sqlRow = new StringBuilder();

                    String colName = rs.getString(3);
                    sqlRow.append("select ")
                            .append(1 - (Integer) currentIndex.get(1))
                            .append(" as un,'")
                            .append(escape(indexName))
                            .append("' as n,")
                            .append(rs.getInt(1) + 1)
                            .append(" as op,");
                    if (colName == null) { // expression index
                        sqlRow.append("null");
                    } else {
                        sqlRow.append("'").append(escape(colName)).append("'");
                    }
                    sqlRow.append(" as cn");

                    unionAll.add(sqlRow.toString());
                }

                rs.close();
            }

            String sqlBlock = StringUtils.join(unionAll, " union all ");

            return ((CoreStatement) stat)
                    .executeQuery(sql.append(sqlBlock).append(");").toString(), true);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureColumns(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String)
     */
    public ResultSet getProcedureColumns(String c, String s, String p, String colPat)
            throws SQLException {
        if (getProcedureColumns == null) {
            getProcedureColumns =
                    conn.prepareStatement(
                            "select null as PROCEDURE_CAT, "
                                    + "null as PROCEDURE_SCHEM, null as PROCEDURE_NAME, null as COLUMN_NAME, "
                                    + "null as COLUMN_TYPE, null as DATA_TYPE, null as TYPE_NAME, null as PRECISION, "
                                    + "null as LENGTH, null as SCALE, null as RADIX, null as NULLABLE, "
                                    + "null as REMARKS limit 0;");
        }
        return getProcedureColumns.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedures(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getProcedures(String c, String s, String p) throws SQLException {
        if (getProcedures == null) {
            getProcedures =
                    conn.prepareStatement(
                            "select null as PROCEDURE_CAT, null as PROCEDURE_SCHEM, "
                                    + "null as PROCEDURE_NAME, null as UNDEF1, null as UNDEF2, null as UNDEF3, "
                                    + "null as REMARKS, null as PROCEDURE_TYPE limit 0;");
        }
        return getProcedures.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTables(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getSuperTables(String c, String s, String t) throws SQLException {
        if (getSuperTables == null) {
            getSuperTables =
                    conn.prepareStatement(
                            "select null as TABLE_CAT, null as TABLE_SCHEM, "
                                    + "null as TABLE_NAME, null as SUPERTABLE_NAME limit 0;");
        }
        return getSuperTables.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTypes(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getSuperTypes(String c, String s, String t) throws SQLException {
        if (getSuperTypes == null) {
            getSuperTypes =
                    conn.prepareStatement(
                            "select null as TYPE_CAT, null as TYPE_SCHEM, "
                                    + "null as TYPE_NAME, null as SUPERTYPE_CAT, null as SUPERTYPE_SCHEM, "
                                    + "null as SUPERTYPE_NAME limit 0;");
        }
        return getSuperTypes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTablePrivileges(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getTablePrivileges(String c, String s, String t) throws SQLException {
        if (getTablePrivileges == null) {
            getTablePrivileges =
                    conn.prepareStatement(
                            "select  null as TABLE_CAT, "
                                    + "null as TABLE_SCHEM, null as TABLE_NAME, null as GRANTOR, null "
                                    + "GRANTEE,  null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }
        return getTablePrivileges.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String,
     *     java.lang.String, java.lang.String[])
     */
    public synchronized ResultSet getTables(
            String c, String s, String tblNamePattern, String[] types) throws SQLException {

        checkOpen();

        tblNamePattern =
                (tblNamePattern == null || "".equals(tblNamePattern))
                        ? "%"
                        : escape(tblNamePattern);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT").append("\n");
        sql.append("  NULL AS TABLE_CAT,").append("\n");
        sql.append("  NULL AS TABLE_SCHEM,").append("\n");
        sql.append("  NAME AS TABLE_NAME,").append("\n");
        sql.append("  TYPE AS TABLE_TYPE,").append("\n");
        sql.append("  NULL AS REMARKS,").append("\n");
        sql.append("  NULL AS TYPE_CAT,").append("\n");
        sql.append("  NULL AS TYPE_SCHEM,").append("\n");
        sql.append("  NULL AS TYPE_NAME,").append("\n");
        sql.append("  NULL AS SELF_REFERENCING_COL_NAME,").append("\n");
        sql.append("  NULL AS REF_GENERATION").append("\n");
        sql.append("FROM").append("\n");
        sql.append("  (").append("\n");
        sql.append("    SELECT\n");
        sql.append("      'sqlite_schema' AS NAME,\n");
        sql.append("      'SYSTEM TABLE' AS TYPE");
        sql.append("    UNION ALL").append("\n");
        sql.append("    SELECT").append("\n");
        sql.append("      NAME,").append("\n");
        sql.append("      UPPER(TYPE) AS TYPE").append("\n");
        sql.append("    FROM").append("\n");
        sql.append("      sqlite_schema").append("\n");
        sql.append("    WHERE").append("\n");
        sql.append("      NAME NOT LIKE 'sqlite\\_%' ESCAPE '\\'").append("\n");
        sql.append("      AND UPPER(TYPE) IN ('TABLE', 'VIEW')").append("\n");
        sql.append("    UNION ALL").append("\n");
        sql.append("    SELECT").append("\n");
        sql.append("      NAME,").append("\n");
        sql.append("      'GLOBAL TEMPORARY' AS TYPE").append("\n");
        sql.append("    FROM").append("\n");
        sql.append("      sqlite_temp_master").append("\n");
        sql.append("    UNION ALL").append("\n");
        sql.append("    SELECT").append("\n");
        sql.append("      NAME,").append("\n");
        sql.append("      'SYSTEM TABLE' AS TYPE").append("\n");
        sql.append("    FROM").append("\n");
        sql.append("      sqlite_schema").append("\n");
        sql.append("    WHERE").append("\n");
        sql.append("      NAME LIKE 'sqlite\\_%' ESCAPE '\\'").append("\n");
        sql.append("  )").append("\n");
        sql.append(" WHERE TABLE_NAME LIKE '");
        sql.append(tblNamePattern);
        sql.append("' ESCAPE '");
        sql.append(getSearchStringEscape());
        sql.append("'");

        if (types != null && types.length != 0) {
            sql.append(" AND TABLE_TYPE IN (");
            sql.append(
                    Arrays.stream(types)
                            .map((t) -> "'" + t.toUpperCase() + "'")
                            .collect(Collectors.joining(",")));
            sql.append(")");
        }

        sql.append(" ORDER BY TABLE_TYPE, TABLE_NAME;");

        return ((CoreStatement) conn.createStatement()).executeQuery(sql.toString(), true);
    }

    /** @see java.sql.DatabaseMetaData#getTableTypes() */
    public ResultSet getTableTypes() throws SQLException {
        checkOpen();

        String sql =
                "SELECT 'TABLE' AS TABLE_TYPE "
                        + "UNION "
                        + "SELECT 'VIEW' AS TABLE_TYPE "
                        + "UNION "
                        + "SELECT 'SYSTEM TABLE' AS TABLE_TYPE "
                        + "UNION "
                        + "SELECT 'GLOBAL TEMPORARY' AS TABLE_TYPE;";

        if (getTableTypes == null) {
            getTableTypes = conn.prepareStatement(sql);
        }
        getTableTypes.clearParameters();
        return getTableTypes.executeQuery();
    }

    /** @see java.sql.DatabaseMetaData#getTypeInfo() */
    public ResultSet getTypeInfo() throws SQLException {
        if (getTypeInfo == null) {
            String sql =
                    QueryUtils.valuesQuery(
                                    Arrays.asList(
                                            "TYPE_NAME",
                                            "DATA_TYPE",
                                            "PRECISION",
                                            "LITERAL_PREFIX",
                                            "LITERAL_SUFFIX",
                                            "CREATE_PARAMS",
                                            "NULLABLE",
                                            "CASE_SENSITIVE",
                                            "SEARCHABLE",
                                            "UNSIGNED_ATTRIBUTE",
                                            "FIXED_PREC_SCALE",
                                            "AUTO_INCREMENT",
                                            "LOCAL_TYPE_NAME",
                                            "MINIMUM_SCALE",
                                            "MAXIMUM_SCALE",
                                            "SQL_DATA_TYPE",
                                            "SQL_DATETIME_SUB",
                                            "NUM_PREC_RADIX"),
                                    Arrays.asList(
                                            Arrays.asList(
                                                    "BLOB",
                                                    Types.BLOB,
                                                    0,
                                                    null,
                                                    null,
                                                    null,
                                                    DatabaseMetaData.typeNullable,
                                                    0,
                                                    DatabaseMetaData.typeSearchable,
                                                    1,
                                                    0,
                                                    0,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    0,
                                                    10),
                                            Arrays.asList(
                                                    "INTEGER",
                                                    Types.INTEGER,
                                                    0,
                                                    null,
                                                    null,
                                                    null,
                                                    DatabaseMetaData.typeNullable,
                                                    0,
                                                    DatabaseMetaData.typeSearchable,
                                                    0,
                                                    0,
                                                    1,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    0,
                                                    10),
                                            Arrays.asList(
                                                    "NULL",
                                                    Types.NULL,
                                                    0,
                                                    null,
                                                    null,
                                                    null,
                                                    DatabaseMetaData.typeNullable,
                                                    0,
                                                    DatabaseMetaData.typeSearchable,
                                                    1,
                                                    0,
                                                    0,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    0,
                                                    10),
                                            Arrays.asList(
                                                    "REAL",
                                                    Types.REAL,
                                                    0,
                                                    null,
                                                    null,
                                                    null,
                                                    DatabaseMetaData.typeNullable,
                                                    0,
                                                    DatabaseMetaData.typeSearchable,
                                                    0,
                                                    0,
                                                    0,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    0,
                                                    10),
                                            Arrays.asList(
                                                    "TEXT",
                                                    Types.VARCHAR,
                                                    0,
                                                    null,
                                                    null,
                                                    null,
                                                    DatabaseMetaData.typeNullable,
                                                    1,
                                                    DatabaseMetaData.typeSearchable,
                                                    1,
                                                    0,
                                                    0,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    0,
                                                    10)))
                            + " order by DATA_TYPE";
            getTypeInfo = conn.prepareStatement(sql);
        }

        getTypeInfo.clearParameters();
        return getTypeInfo.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getUDTs(java.lang.String, java.lang.String, java.lang.String,
     *     int[])
     */
    public ResultSet getUDTs(String c, String s, String t, int[] types) throws SQLException {
        if (getUDTs == null) {
            getUDTs =
                    conn.prepareStatement(
                            "select  null as TYPE_CAT, null as TYPE_SCHEM, "
                                    + "null as TYPE_NAME,  null as CLASS_NAME,  null as DATA_TYPE, null as REMARKS, "
                                    + "null as BASE_TYPE "
                                    + "limit 0;");
        }

        getUDTs.clearParameters();
        return getUDTs.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getVersionColumns(java.lang.String, java.lang.String,
     *     java.lang.String)
     */
    public ResultSet getVersionColumns(String c, String s, String t) throws SQLException {
        if (getVersionColumns == null) {
            getVersionColumns =
                    conn.prepareStatement(
                            "select null as SCOPE, null as COLUMN_NAME, "
                                    + "null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, "
                                    + "null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }
        return getVersionColumns.executeQuery();
    }

    /**
     * @deprecated Not exactly sure what this function does, as it is not implementing any
     *     interface, and is not used anywhere in the code. Deprecated since 3.43.0.0.
     */
    @Deprecated
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException("not implemented by SQLite JDBC driver");
    }

    /** Not implemented yet. */
    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not yet implemented by SQLite JDBC driver");
    }

    /** Not implemented yet. */
    public ResultSet getFunctionColumns(String a, String b, String c, String d)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("Not yet implemented by SQLite JDBC driver");
    }

    // inner classes

    /** Pattern used to extract column order for an unnamed primary key. */
    protected static final Pattern PK_UNNAMED_PATTERN =
            Pattern.compile(
                    ".*PRIMARY\\s+KEY\\s*\\((.*?)\\).*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Pattern used to extract a named primary key. */
    protected static final Pattern PK_NAMED_PATTERN =
            Pattern.compile(
                    ".*CONSTRAINT\\s*(.*?)\\s*PRIMARY\\s+KEY\\s*\\((.*?)\\).*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Parses the sqlite_schema table for a table's primary key */
    class PrimaryKeyFinder {
        /** The table name. */
        String table;

        /** The primary key name. */
        String pkName = null;

        /** The column(s) for the primary key. */
        String[] pkColumns = null;

        /**
         * Constructor.
         *
         * @param table The table for which to get find a primary key.
         * @throws SQLException
         */
        public PrimaryKeyFinder(String table) throws SQLException {
            this.table = table;

            // specific handling for sqlite_schema and synonyms, so that
            // getExportedKeys/getPrimaryKeys return an empty ResultSet instead of throwing an
            // exception
            if ("sqlite_schema".equals(table) || "sqlite_master".equals(table)) return;

            if (table == null || table.trim().length() == 0) {
                throw new SQLException("Invalid table name: '" + this.table + "'");
            }

            try (Statement stat = conn.createStatement();
                    // read create SQL script for table
                    ResultSet rs =
                            stat.executeQuery(
                                    "select sql from sqlite_schema where"
                                            + " lower(name) = lower('"
                                            + escape(table)
                                            + "') and type in ('table', 'view')")) {

                if (!rs.next()) throw new SQLException("Table not found: '" + table + "'");

                Matcher matcher = PK_NAMED_PATTERN.matcher(rs.getString(1));
                if (matcher.find()) {
                    pkName = unquoteIdentifier(escape(matcher.group(1)));
                    pkColumns = matcher.group(2).split(",");
                } else {
                    matcher = PK_UNNAMED_PATTERN.matcher(rs.getString(1));
                    if (matcher.find()) {
                        pkColumns = matcher.group(1).split(",");
                    }
                }

                if (pkColumns == null) {
                    try (ResultSet rs2 =
                            stat.executeQuery("pragma table_info('" + escape(table) + "');")) {
                        while (rs2.next()) {
                            if (rs2.getBoolean(6)) pkColumns = new String[] {rs2.getString(2)};
                        }
                    }
                }

                if (pkColumns != null) {
                    for (int i = 0; i < pkColumns.length; i++) {
                        pkColumns[i] = unquoteIdentifier(pkColumns[i]);
                    }
                }
            }
        }

        /** @return The primary key name if any. */
        public String getName() {
            return pkName;
        }

        /** @return Array of primary key column(s) if any. */
        public String[] getColumns() {
            return pkColumns;
        }
    }

    class ImportedKeyFinder {

        /** Pattern used to extract a named primary key. */
        private final Pattern FK_NAMED_PATTERN =
                Pattern.compile(
                        "CONSTRAINT\\s*\"?([A-Za-z_][A-Za-z\\d_]*)?\"?\\s*FOREIGN\\s+KEY\\s*\\((.*?)\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        private final String fkTableName;
        private final List<ForeignKey> fkList = new ArrayList<>();

        public ImportedKeyFinder(String table) throws SQLException {

            if (table == null || table.trim().length() == 0) {
                throw new SQLException("Invalid table name: '" + table + "'");
            }

            this.fkTableName = table;

            List<String> fkNames = getForeignKeyNames(this.fkTableName);

            try (Statement stat = conn.createStatement();
                    ResultSet rs =
                            stat.executeQuery(
                                    "pragma foreign_key_list('"
                                            + escape(this.fkTableName.toLowerCase())
                                            + "')")) {

                int prevFkId = -1;
                int count = 0;
                ForeignKey fk = null;
                while (rs.next()) {
                    int fkId = rs.getInt(1);
                    String pkTableName = rs.getString(3);
                    String fkColName = rs.getString(4);
                    String pkColName = rs.getString(5);
                    String onUpdate = rs.getString(6);
                    String onDelete = rs.getString(7);
                    String match = rs.getString(8);

                    String fkName = null;
                    if (fkNames.size() > count) fkName = fkNames.get(count);

                    if (fkId != prevFkId) {
                        fk =
                                new ForeignKey(
                                        fkName,
                                        pkTableName,
                                        fkTableName,
                                        onUpdate,
                                        onDelete,
                                        match);
                        fkList.add(fk);
                        prevFkId = fkId;
                        count++;
                    }
                    if (fk != null) {
                        fk.addColumnMapping(fkColName, pkColName);
                    }
                }
            }
        }

        private List<String> getForeignKeyNames(String tbl) throws SQLException {
            List<String> fkNames = new ArrayList<>();
            if (tbl == null) {
                return fkNames;
            }
            try (Statement stat2 = conn.createStatement();
                    ResultSet rs =
                            stat2.executeQuery(
                                    "select sql from sqlite_schema where"
                                            + " lower(name) = lower('"
                                            + escape(tbl)
                                            + "')")) {

                if (rs.next()) {
                    Matcher matcher = FK_NAMED_PATTERN.matcher(rs.getString(1));

                    while (matcher.find()) {
                        fkNames.add(matcher.group(1));
                    }
                }
            }
            Collections.reverse(fkNames);
            return fkNames;
        }

        public String getFkTableName() {
            return fkTableName;
        }

        public List<ForeignKey> getFkList() {
            return fkList;
        }

        class ForeignKey {

            private final String fkName;
            private final String pkTableName;
            private final String fkTableName;
            private final List<String> fkColNames = new ArrayList<>();
            private final List<String> pkColNames = new ArrayList<>();
            private final String onUpdate;
            private final String onDelete;
            private final String match;

            ForeignKey(
                    String fkName,
                    String pkTableName,
                    String fkTableName,
                    String onUpdate,
                    String onDelete,
                    String match) {
                this.fkName = fkName;
                this.pkTableName = pkTableName;
                this.fkTableName = fkTableName;
                this.onUpdate = onUpdate;
                this.onDelete = onDelete;
                this.match = match;
            }

            public String getFkName() {
                return fkName;
            }

            void addColumnMapping(String fkColName, String pkColName) {
                fkColNames.add(fkColName);
                pkColNames.add(pkColName);
            }

            public String[] getColumnMapping(int colSeq) {
                return new String[] {fkColNames.get(colSeq), pkColNames.get(colSeq)};
            }

            public int getColumnMappingCount() {
                return fkColNames.size();
            }

            public String getPkTableName() {
                return pkTableName;
            }

            public String getFkTableName() {
                return fkTableName;
            }

            public String getOnUpdate() {
                return onUpdate;
            }

            public String getOnDelete() {
                return onDelete;
            }

            public String getMatch() {
                return match;
            }

            @Override
            public String toString() {
                return "ForeignKey [fkName="
                        + fkName
                        + ", pkTableName="
                        + pkTableName
                        + ", fkTableName="
                        + fkTableName
                        + ", pkColNames="
                        + pkColNames
                        + ", fkColNames="
                        + fkColNames
                        + "]";
            }
        }
    }

    /** @see java.lang.Object#finalize() */
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * Follow rules in <a href="https://www.sqlite.org/lang_keywords.html">SQLite Keywords</a>
     *
     * @param name Identifier name
     * @return Unquoted identifier
     */
    private String unquoteIdentifier(String name) {
        if (name == null) return name;
        name = name.trim();
        if (name.length() > 2
                && ((name.startsWith("`") && name.endsWith("`"))
                        || (name.startsWith("\"") && name.endsWith("\""))
                        || (name.startsWith("[") && name.endsWith("]")))) {
            // unquote to be consistent with column names returned by getColumns()
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    /**
     * Class-wrapper around the logger object to avoid build-time initialization of the logging
     * framework in native-image
     */
    private static class LogHolder {
        private static final Logger logger = LoggerFactory.getLogger(JDBC3DatabaseMetaData.class);
    }
}
