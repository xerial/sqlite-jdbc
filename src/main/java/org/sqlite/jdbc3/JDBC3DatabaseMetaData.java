package org.sqlite.jdbc3;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sqlite.SQLiteConnection;
import org.sqlite.core.Codes;
import org.sqlite.core.CoreStatement;
import org.sqlite.util.StringUtils;

public abstract class JDBC3DatabaseMetaData extends org.sqlite.core.CoreDatabaseMetaData {

    protected JDBC3DatabaseMetaData(SQLiteConnection conn) {
        super(conn);
    }

    /**
     * @see java.sql.DatabaseMetaData#getConnection()
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
     */
    public int getDatabaseMajorVersion() {
        return 3;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
     */
    public int getDatabaseMinorVersion() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverMajorVersion()
     */
    public int getDriverMajorVersion() {
        return 1;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverMinorVersion()
     */
    public int getDriverMinorVersion() {
        return 1;
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMajorVersion()
     */
    public int getJDBCMajorVersion() {
        return 2;
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMinorVersion()
     */
    public int getJDBCMinorVersion() {
        return 1;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDefaultTransactionIsolation()
     */
    public int getDefaultTransactionIsolation() {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxBinaryLiteralLength()
     */
    public int getMaxBinaryLiteralLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxCatalogNameLength()
     */
    public int getMaxCatalogNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxCharLiteralLength()
     */
    public int getMaxCharLiteralLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnNameLength()
     */
    public int getMaxColumnNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnsInGroupBy()
     */
    public int getMaxColumnsInGroupBy() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnsInIndex()
     */
    public int getMaxColumnsInIndex() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnsInOrderBy()
     */
    public int getMaxColumnsInOrderBy() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnsInSelect()
     */
    public int getMaxColumnsInSelect() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxColumnsInTable()
     */
    public int getMaxColumnsInTable() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxConnections()
     */
    public int getMaxConnections() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxCursorNameLength()
     */
    public int getMaxCursorNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxIndexLength()
     */
    public int getMaxIndexLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxProcedureNameLength()
     */
    public int getMaxProcedureNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxRowSize()
     */
    public int getMaxRowSize() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxSchemaNameLength()
     */
    public int getMaxSchemaNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxStatementLength()
     */
    public int getMaxStatementLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxStatements()
     */
    public int getMaxStatements() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxTableNameLength()
     */
    public int getMaxTableNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxTablesInSelect()
     */
    public int getMaxTablesInSelect() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxUserNameLength()
     */
    public int getMaxUserNameLength() {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getResultSetHoldability()
     */
    public int getResultSetHoldability() {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSQLStateType()
     */
    public int getSQLStateType() {
        return DatabaseMetaData.sqlStateSQL99;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public String getDatabaseProductName() {
        return "SQLite";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseProductVersion()
     */
    public String getDatabaseProductVersion() throws SQLException {
        return conn.libversion();
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverName()
     */
    public String getDriverName() {
        return "SQLiteJDBC";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverVersion()
     */
    public String getDriverVersion() {
        return conn.getDriverVersion();
    }

    /**
     * @see java.sql.DatabaseMetaData#getExtraNameCharacters()
     */
    public String getExtraNameCharacters() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogSeparator()
     */
    public String getCatalogSeparator() {
        return ".";
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogTerm()
     */
    public String getCatalogTerm() {
        return "catalog";
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemaTerm()
     */
    public String getSchemaTerm() {
        return "schema";
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureTerm()
     */
    public String getProcedureTerm() {
        return "not_implemented";
    }

    /**
     * @see java.sql.DatabaseMetaData#getSearchStringEscape()
     */
    public String getSearchStringEscape() {
        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#getIdentifierQuoteString()
     */
    public String getIdentifierQuoteString() {
        return " ";
    }

    /**
     * @see java.sql.DatabaseMetaData#getSQLKeywords()
     */
    public String getSQLKeywords() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getNumericFunctions()
     */
    public String getNumericFunctions() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getStringFunctions()
     */
    public String getStringFunctions() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getSystemFunctions()
     */
    public String getSystemFunctions() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getTimeDateFunctions()
     */
    public String getTimeDateFunctions() {
        return "";
    }

    /**
     * @see java.sql.DatabaseMetaData#getURL()
     */
    public String getURL() {
        return conn.url();
    }

    /**
     * @see java.sql.DatabaseMetaData#getUserName()
     */
    public String getUserName() {
        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#allProceduresAreCallable()
     */
    public boolean allProceduresAreCallable() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#allTablesAreSelectable()
     */
    public boolean allTablesAreSelectable() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()
     */
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#dataDefinitionIgnoredInTransactions()
     */
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#doesMaxRowSizeIncludeBlobs()
     */
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#deletesAreDetected(int)
     */
    public boolean deletesAreDetected(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#insertsAreDetected(int)
     */
    public boolean insertsAreDetected(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#isCatalogAtStart()
     */
    public boolean isCatalogAtStart() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#locatorsUpdateCopy()
     */
    public boolean locatorsUpdateCopy() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#nullPlusNonNullIsNull()
     */
    public boolean nullPlusNonNullIsNull() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtEnd()
     */
    public boolean nullsAreSortedAtEnd() {
        return !nullsAreSortedAtStart();
    }

    /**
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtStart()
     */
    public boolean nullsAreSortedAtStart() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#nullsAreSortedHigh()
     */
    public boolean nullsAreSortedHigh() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#nullsAreSortedLow()
     */
    public boolean nullsAreSortedLow() {
        return !nullsAreSortedHigh();
    }

    /**
     * @see java.sql.DatabaseMetaData#othersDeletesAreVisible(int)
     */
    public boolean othersDeletesAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#othersInsertsAreVisible(int)
     */
    public boolean othersInsertsAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#othersUpdatesAreVisible(int)
     */
    public boolean othersUpdatesAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#ownDeletesAreVisible(int)
     */
    public boolean ownDeletesAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#ownInsertsAreVisible(int)
     */
    public boolean ownInsertsAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#ownUpdatesAreVisible(int)
     */
    public boolean ownUpdatesAreVisible(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesLowerCaseIdentifiers()
     */
    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesLowerCaseQuotedIdentifiers()
     */
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesMixedCaseIdentifiers()
     */
    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesMixedCaseQuotedIdentifiers()
     */
    public boolean storesMixedCaseQuotedIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesUpperCaseIdentifiers()
     */
    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#storesUpperCaseQuotedIdentifiers()
     */
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithAddColumn()
     */
    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithDropColumn()
     */
    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92EntryLevelSQL()
     */
    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92FullSQL()
     */
    public boolean supportsANSI92FullSQL() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92IntermediateSQL()
     */
    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsBatchUpdates()
     */
    public boolean supportsBatchUpdates() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInDataManipulation()
     */
    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInIndexDefinitions()
     */
    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions()
     */
    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInProcedureCalls()
     */
    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInTableDefinitions()
     */
    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsColumnAliasing()
     */
    public boolean supportsColumnAliasing() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsConvert()
     */
    public boolean supportsConvert() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsConvert(int, int)
     */
    public boolean supportsConvert(int fromType, int toType) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCorrelatedSubqueries()
     */
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsDataDefinitionAndDataManipulationTransactions()
     */
    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsDataManipulationTransactionsOnly()
     */
    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsDifferentTableCorrelationNames()
     */
    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsExpressionsInOrderBy()
     */
    public boolean supportsExpressionsInOrderBy() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMinimumSQLGrammar()
     */
    public boolean supportsMinimumSQLGrammar() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCoreSQLGrammar()
     */
    public boolean supportsCoreSQLGrammar() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsExtendedSQLGrammar()
     */
    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsLimitedOuterJoins()
     */
    public boolean supportsLimitedOuterJoins() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsFullOuterJoins()
     */
    public boolean supportsFullOuterJoins() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
     */
    public boolean supportsGetGeneratedKeys() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGroupBy()
     */
    public boolean supportsGroupBy() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGroupByBeyondSelect()
     */
    public boolean supportsGroupByBeyondSelect() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGroupByUnrelated()
     */
    public boolean supportsGroupByUnrelated() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsIntegrityEnhancementFacility()
     */
    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsLikeEscapeClause()
     */
    public boolean supportsLikeEscapeClause() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMixedCaseIdentifiers()
     */
    public boolean supportsMixedCaseIdentifiers() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMixedCaseQuotedIdentifiers()
     */
    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleOpenResults()
     */
    public boolean supportsMultipleOpenResults() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleResultSets()
     */
    public boolean supportsMultipleResultSets() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleTransactions()
     */
    public boolean supportsMultipleTransactions() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsNamedParameters()
     */
    public boolean supportsNamedParameters() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsNonNullableColumns()
     */
    public boolean supportsNonNullableColumns() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossCommit()
     */
    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossRollback()
     */
    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossCommit()
     */
    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossRollback()
     */
    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOrderByUnrelated()
     */
    public boolean supportsOrderByUnrelated() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOuterJoins()
     */
    public boolean supportsOuterJoins() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsPositionedDelete()
     */
    public boolean supportsPositionedDelete() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsPositionedUpdate()
     */
    public boolean supportsPositionedUpdate() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetConcurrency(int, int)
     */
    public boolean supportsResultSetConcurrency(int t, int c) {
        return t == ResultSet.TYPE_FORWARD_ONLY && c == ResultSet.CONCUR_READ_ONLY;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetHoldability(int)
     */
    public boolean supportsResultSetHoldability(int h) {
        return h == ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetType(int)
     */
    public boolean supportsResultSetType(int t) {
        return t == ResultSet.TYPE_FORWARD_ONLY;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSavepoints()
     */
    public boolean supportsSavepoints() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInDataManipulation()
     */
    public boolean supportsSchemasInDataManipulation() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInIndexDefinitions()
     */
    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInPrivilegeDefinitions()
     */
    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInProcedureCalls()
     */
    public boolean supportsSchemasInProcedureCalls() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInTableDefinitions()
     */
    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSelectForUpdate()
     */
    public boolean supportsSelectForUpdate() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStatementPooling()
     */
    public boolean supportsStatementPooling() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStoredProcedures()
     */
    public boolean supportsStoredProcedures() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInComparisons()
     */
    public boolean supportsSubqueriesInComparisons() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInExists()
     */
    public boolean supportsSubqueriesInExists() {
        return true;
    } // TODO: check

    /**
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInIns()
     */
    public boolean supportsSubqueriesInIns() {
        return true;
    } // TODO: check

    /**
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInQuantifieds()
     */
    public boolean supportsSubqueriesInQuantifieds() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsTableCorrelationNames()
     */
    public boolean supportsTableCorrelationNames() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsTransactionIsolationLevel(int)
     */
    public boolean supportsTransactionIsolationLevel(int level) {
        return level == Connection.TRANSACTION_SERIALIZABLE;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsTransactions()
     */
    public boolean supportsTransactions() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsUnion()
     */
    public boolean supportsUnion() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsUnionAll()
     */
    public boolean supportsUnionAll() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#updatesAreDetected(int)
     */
    public boolean updatesAreDetected(int type) {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#usesLocalFilePerTable()
     */
    public boolean usesLocalFilePerTable() {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#usesLocalFiles()
     */
    public boolean usesLocalFiles() {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#isReadOnly()
     */
    public boolean isReadOnly() throws SQLException {
        return conn.isReadOnly();
    }

    /**
     * @see java.sql.DatabaseMetaData#getAttributes(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getAttributes(String c, String s, String t, String a) throws SQLException {
        if (getAttributes == null) {
            getAttributes = conn.prepareStatement("select null as TYPE_CAT, null as TYPE_SCHEM, " +
                    "null as TYPE_NAME, null as ATTR_NAME, null as DATA_TYPE, " +
                    "null as ATTR_TYPE_NAME, null as ATTR_SIZE, null as DECIMAL_DIGITS, " +
                    "null as NUM_PREC_RADIX, null as NULLABLE, null as REMARKS, null as ATTR_DEF, " +
                    "null as SQL_DATA_TYPE, null as SQL_DATETIME_SUB, null as CHAR_OCTET_LENGTH, " +
                    "null as ORDINAL_POSITION, null as IS_NULLABLE, null as SCOPE_CATALOG, " +
                    "null as SCOPE_SCHEMA, null as SCOPE_TABLE, null as SOURCE_DATA_TYPE limit 0;");
        }

        return getAttributes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getBestRowIdentifier(java.lang.String, java.lang.String,
     *      java.lang.String, int, boolean)
     */
    public ResultSet getBestRowIdentifier(String c, String s, String t, int scope, boolean n) throws SQLException {
        if (getBestRowIdentifier == null) {
            getBestRowIdentifier = conn.prepareStatement("select null as SCOPE, null as COLUMN_NAME, " +
                    "null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, " +
                    "null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }

        return getBestRowIdentifier.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getColumnPrivileges(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getColumnPrivileges(String c, String s, String t, String colPat) throws SQLException {
        if (getColumnPrivileges == null) {
            getColumnPrivileges = conn.prepareStatement("select null as TABLE_CAT, null as TABLE_SCHEM, " +
                    "null as TABLE_NAME, null as COLUMN_NAME, null as GRANTOR, null as GRANTEE, " +
                    "null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }

        return getColumnPrivileges.executeQuery();
    }

    // Column type patterns
    protected static final Pattern TYPE_INTEGER = Pattern.compile(".*(INT|BOOL).*");
    protected static final Pattern TYPE_VARCHAR = Pattern.compile(".*(CHAR|CLOB|TEXT|BLOB).*");
    protected static final Pattern TYPE_FLOAT = Pattern.compile(".*(REAL|FLOA|DOUB|DEC|NUM).*");

    /**
     * @see java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getColumns(String c, String s, String tblNamePattern, String colNamePattern) throws SQLException {

        // get the list of tables matching the pattern (getTables)
        // create a Matrix Cursor for each of the tables
        // create a merge cursor from all the Matrix Cursors
        // and return the columname and type from:
        //    "PRAGMA table_info(tablename)"
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
        //        TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
        //        COLUMN_SIZE int => column size.
        //        BUFFER_LENGTH is not used.
        //        DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable.
        //        NUM_PREC_RADIX int => Radix (typically either 10 or 2)
        //        NULLABLE int => is NULL allowed.
        //        columnNoNulls - might not allow NULL values
        //        columnNullable - definitely allows NULL values
        //        columnNullableUnknown - nullability unknown
        //        REMARKS String => comment describing column (may be null)
        //        COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
        //        SQL_DATA_TYPE int => unused
        //        SQL_DATETIME_SUB int => unused
        //        CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
        //        ORDINAL_POSITION int => index of column in table (starting at 1)
        //        IS_NULLABLE String => ISO rules are used to determine the nullability for a column.
        //        YES --- if the parameter can include NULLs
        //        NO --- if the parameter cannot include NULLs
        //        empty string --- if the nullability for the parameter is unknown
        //        SCOPE_CATLOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
        //        SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
        //        SCOPE_TABLE String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF)
        //        SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
        //        IS_AUTOINCREMENT String => Indicates whether this column is auto incremented
        //        YES --- if the column is auto incremented
        //        NO --- if the column is not auto incremented
        //        empty string --- if it cannot be determined whether the column is auto incremented parameter is unknown
        checkOpen();

        StringBuilder sql = new StringBuilder(700);
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, tblname as TABLE_NAME, ")
           .append("cn as COLUMN_NAME, ct as DATA_TYPE, tn as TYPE_NAME, 2000000000 as COLUMN_SIZE, ")
           .append("2000000000 as BUFFER_LENGTH, 10   as DECIMAL_DIGITS, 10   as NUM_PREC_RADIX, ")
           .append("colnullable as NULLABLE, null as REMARKS, colDefault as COLUMN_DEF, ")
           .append("0    as SQL_DATA_TYPE, 0    as SQL_DATETIME_SUB, 2000000000 as CHAR_OCTET_LENGTH, ")
           .append("ordpos as ORDINAL_POSITION, (case colnullable when 0 then 'NO' when 1 then 'YES' else '' end)")
           .append("    as IS_NULLABLE, null as SCOPE_CATLOG, null as SCOPE_SCHEMA, ")
           .append("null as SCOPE_TABLE, null as SOURCE_DATA_TYPE from (");

        boolean colFound = false;

        ResultSet rs = null;
        try {
            // Get all tables implied by the input
            final String[] types = new String[] {"TABLE", "VIEW"};
            rs = getTables(c, s, tblNamePattern, types);
            while (rs.next()) {
                String tableName = rs.getString(3);

                Statement colstat = conn.createStatement();
                ResultSet rscol = null;
                try {
                    // For each table, get the column info and build into overall SQL
                    String pragmaStatement = "PRAGMA table_info('"+ tableName + "')";
                    rscol = colstat.executeQuery(pragmaStatement);

                    for (int i = 0; rscol.next(); i++) {
                        String colName = rscol.getString(2);
                        String colType = rscol.getString(3);
                        String colNotNull = rscol.getString(4);
                        String colDefault = rscol.getString(5);

                        int colNullable = 2;
                        if (colNotNull != null) {
                            colNullable = colNotNull.equals("0") ? 1 : 0;
                        }

                        if (colFound) {
                            sql.append(" union all ");
                        }
                        colFound = true;

                        /*
                         * improved column types
                         * ref http://www.sqlite.org/datatype3.html - 2.1 Determination Of Column Affinity
                         * plus some degree of artistic-license applied
                         */
                        colType = colType == null ? "TEXT" : colType.toUpperCase();
                        int colJavaType = -1;
                        // rule #1 + boolean
                        if (TYPE_INTEGER.matcher(colType).find()) {
                            colJavaType = Types.INTEGER;
                        }
                        else if (TYPE_VARCHAR.matcher(colType).find()) {
                            colJavaType = Types.VARCHAR;
                        }
                        else if (TYPE_FLOAT.matcher(colType).find()) {
                            colJavaType = Types.FLOAT;
                        }
                        else {
                            // catch-all
                            colJavaType = Types.VARCHAR;
                        }

                        sql.append("select ").append(i).append(" as ordpos, ")
                           .append(colNullable).append(" as colnullable,")
                           .append("'").append(colJavaType).append("' as ct, ")
                           .append("'").append(tableName).append("' as tblname, ")
                           .append("'").append(escape(colName)).append("' as cn, ")
                           .append("'").append(escape(colType)).append("' as tn, ")
                           .append(quote(colDefault == null ? null : escape(colDefault))).append(" as colDefault");

                        if (colNamePattern != null) {
                            sql.append(" where upper(cn) like upper('").append(escape(colNamePattern)).append("')");
                        }
                    }
                } finally {
                    if (rscol != null) {
                        try {
                            rscol.close();
                        } catch (SQLException e) {}
                    }
                    if (colstat != null) {
                        try {
                            colstat.close();
                        } catch(SQLException e) {}
                    }
                }
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (colFound) {
            sql.append(") order by TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION;");
        }
        else {
            sql.append("select null as ordpos, null as colnullable, null as ct, null as tblname, null as cn, null as tn, null as colDefault) limit 0;");
        }

        Statement stat = conn.createStatement();
        return ((CoreStatement)stat).executeQuery(sql.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getCrossReference(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public ResultSet getCrossReference(String pc, String ps, String pt, String fc, String fs, String ft) throws SQLException {
        if (pt == null) {
            return getExportedKeys(fc, fs, ft);
        }

        if (ft == null) {
            return getImportedKeys(pc, ps, pt);
        }

        StringBuilder query = new StringBuilder();
        query.append("select ").append(quote(pc)).append(" as PKTABLE_CAT, ")
            .append(quote(ps)).append(" as PKTABLE_SCHEM, ").append(quote(pt)).append(" as PKTABLE_NAME, ")
            .append("'' as PKCOLUMN_NAME, ").append(quote(fc)).append(" as FKTABLE_CAT, ")
            .append(quote(fs)).append(" as FKTABLE_SCHEM, ").append(quote(ft)).append(" as FKTABLE_NAME, ")
            .append("'' as FKCOLUMN_NAME, -1 as KEY_SEQ, 3 as UPDATE_RULE, 3 as DELETE_RULE, '' as FK_NAME, '' as PK_NAME, ")
            .append(Integer.toString(DatabaseMetaData.importedKeyInitiallyDeferred)).append(" as DEFERRABILITY limit 0 ");

        return ((CoreStatement)conn.createStatement()).executeQuery(query.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemas()
     */
    public ResultSet getSchemas() throws SQLException {
        if (getSchemas == null) {
            getSchemas = conn.prepareStatement("select null as TABLE_SCHEM, null as TABLE_CATALOG limit 0;");
        }

        return getSchemas.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogs()
     */
    public ResultSet getCatalogs() throws SQLException {
        if (getCatalogs == null) {
            getCatalogs = conn.prepareStatement("select null as TABLE_CAT limit 0;");
        }

        return getCatalogs.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getPrimaryKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
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

            return ((CoreStatement)stat).executeQuery(sql.toString(), true);
        }

        String pkName = pkFinder.getName();

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sql.append(" union ");
            sql.append("select ").append(pkName).append(" as pk, '")
               .append(escape(columns[i].trim())).append("' as cn, ")
               .append(i).append(" as ks");
        }

        return ((CoreStatement)stat).executeQuery(sql.append(") order by cn;").toString(), true);
    }

    private final static Map<String, Integer> RULE_MAP = new HashMap<String, Integer>();

    static {
        RULE_MAP.put("NO ACTION", DatabaseMetaData.importedKeyNoAction);
        RULE_MAP.put("CASCADE", DatabaseMetaData.importedKeyCascade);
        RULE_MAP.put("RESTRICT", DatabaseMetaData.importedKeyRestrict);
        RULE_MAP.put("SET NULL", DatabaseMetaData.importedKeySetNull);
        RULE_MAP.put("SET DEFAULT", DatabaseMetaData.importedKeySetDefault);
    }

    /**
     * Pattern used to extract a named primary key.
     */
     protected final static Pattern FK_NAMED_PATTERN =
        Pattern.compile(".*\\sCONSTRAINT\\s+(.*?)\\s*FOREIGN\\s+KEY\\s*\\((.*?)\\).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

     /**
     * @see java.sql.DatabaseMetaData#getExportedKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        PrimaryKeyFinder pkFinder = new PrimaryKeyFinder(table);
        String[] pkColumns = pkFinder.getColumns();
        Statement stat = conn.createStatement();

        catalog = (catalog != null) ? quote(catalog) : null;
        schema = (schema != null) ? quote(schema) : null;

        StringBuilder exportedKeysQuery = new StringBuilder(512);

        int count = 0;
        if (pkColumns != null) {
            // retrieve table list
            ResultSet rs = stat.executeQuery("select name from sqlite_master where type = 'table'");
            ArrayList<String> tableList = new ArrayList<String>();

            while (rs.next()) {
                tableList.add(rs.getString(1));
            }

            rs.close();
    
            ResultSet fk = null;
            String target = table.toLowerCase();
            // find imported keys for each table
            for (String tbl : tableList) {
                try {
                    fk = stat.executeQuery("pragma foreign_key_list('" + escape(tbl) + "')");
                } catch (SQLException e) {
                    if (e.getErrorCode() == Codes.SQLITE_DONE) 
                        continue; // expected if table has no foreign keys

                    throw e;
                }

                Statement stat2 = null;
                try {
                    stat2 = conn.createStatement();

                    while(fk.next()) {
                        int keySeq = fk.getInt(2) + 1;
                        String PKTabName = fk.getString(3).toLowerCase();

                        if (PKTabName == null || !PKTabName.equals(target)) {
                            continue;
                        }

                        String PKColName = fk.getString(5);
                        PKColName = (PKColName == null) ? pkColumns[0] : PKColName.toLowerCase();

                        exportedKeysQuery
                            .append(count > 0 ? " union all select " : "select ")
                            .append(Integer.toString(keySeq)).append(" as ks, lower('")
                            .append(escape(tbl)).append("') as fkt, lower('")
                            .append(escape(fk.getString(4))).append("') as fcn, '")
                            .append(escape(PKColName)).append("' as pcn, ")
                            .append(RULE_MAP.get(fk.getString(6))).append(" as ur, ")
                            .append(RULE_MAP.get(fk.getString(7))).append(" as dr, ");

                        rs = stat2.executeQuery("select sql from sqlite_master where" +
                            " lower(name) = lower('" + escape(tbl) + "')");

                        if (rs.next()) {
                            Matcher matcher = FK_NAMED_PATTERN.matcher(rs.getString(1));

                            if (matcher.find()){
                                exportedKeysQuery.append("'").append(escape(matcher.group(1).toLowerCase())).append("' as fkn");
                            }
                            else {
                                exportedKeysQuery.append("'' as fkn");
                            }
                        }

                        rs.close();
                        count++;
                    }
                }
                finally {
                    try{
                        if (rs != null) rs.close();
                    }catch(SQLException e) {}
                    try{
                        if (stat2 != null) stat2.close();
                    }catch(SQLException e) {}
                    try{
                        if (fk != null) fk.close();
                    }catch(SQLException e) {}
                }
            }
        }

        boolean hasImportedKey = (count > 0);
        StringBuilder sql = new StringBuilder(512);
        sql.append("select ")
            .append(catalog).append(" as PKTABLE_CAT, ")
            .append(schema).append(" as PKTABLE_SCHEM, ")
            .append(quote(table)).append(" as PKTABLE_NAME, ")
            .append(hasImportedKey ? "pcn" : "''").append(" as PKCOLUMN_NAME, ")
            .append(catalog).append(" as FKTABLE_CAT, ")
            .append(schema).append(" as FKTABLE_SCHEM, ")
            .append(hasImportedKey ? "fkt" : "''").append(" as FKTABLE_NAME, ")
            .append(hasImportedKey ? "fcn" : "''").append(" as FKCOLUMN_NAME, ")
            .append(hasImportedKey ? "ks" : "-1").append(" as KEY_SEQ, ")
            .append(hasImportedKey ? "ur" : "3").append(" as UPDATE_RULE, ")
            .append(hasImportedKey ? "dr" : "3").append(" as DELETE_RULE, ")
            .append(hasImportedKey ? "fkn" : "''").append(" as FK_NAME, ")
            .append(pkFinder.getName() != null ? pkFinder.getName() : "''").append(" as PK_NAME, ")
            .append(Integer.toString(DatabaseMetaData.importedKeyInitiallyDeferred)) // FIXME: Check for pragma foreign_keys = true ?
            .append(" as DEFERRABILITY ");

        if (hasImportedKey) {
            sql.append("from (").append(exportedKeysQuery).append(") order by fkt");
        }
        else {
            sql.append("limit 0");
        }

        return ((CoreStatement)stat).executeQuery(sql.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getImportedKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        ResultSet rs = null;
        Statement stat = conn.createStatement();
        StringBuilder sql = new StringBuilder(700);

        sql.append("select ").append(quote(catalog)).append(" as PKTABLE_CAT, ")
            .append(quote(schema)).append(" as PKTABLE_SCHEM, ")
            .append("ptn as PKTABLE_NAME, pcn as PKCOLUMN_NAME, ")
            .append(quote(catalog)).append(" as FKTABLE_CAT, ")
            .append(quote(schema)).append(" as FKTABLE_SCHEM, ")
            .append(quote(table)).append(" as FKTABLE_NAME, ") 
            .append("fcn as FKCOLUMN_NAME, ks as KEY_SEQ, ur as UPDATE_RULE, dr as DELETE_RULE, '' as FK_NAME, '' as PK_NAME, ")
            .append(Integer.toString(DatabaseMetaData.importedKeyInitiallyDeferred)).append(" as DEFERRABILITY from (");

        // Use a try catch block to avoid "query does not return ResultSet" error
        try {
            rs = stat.executeQuery("pragma foreign_key_list('" + escape(table) + "');");
        }
        catch (SQLException e) {
            sql.append("select -1 as ks, '' as ptn, '' as fcn, '' as pcn, ")
                .append(DatabaseMetaData.importedKeyNoAction).append(" as ur, ")
                .append(DatabaseMetaData.importedKeyNoAction).append(" as dr) limit 0;");

            return ((CoreStatement)stat).executeQuery(sql.toString(), true);
        }

        for (int i = 0; rs.next(); i++) {
            int keySeq = rs.getInt(2) + 1;
            String PKTabName = rs.getString(3);
            String FKColName = rs.getString(4);
            String PKColName = rs.getString(5);

            if (PKColName == null) {
                PKColName = new PrimaryKeyFinder(PKTabName).getColumns()[0];
            }

            String updateRule = rs.getString(6);
            String deleteRule = rs.getString(7);

            if (i > 0) {
                sql.append(" union all ");
            }

            sql.append("select ").append(keySeq).append(" as ks,")
                .append("'").append(escape(PKTabName)).append("' as ptn, '")
                .append(escape(FKColName)).append("' as fcn, '")
                .append(escape(PKColName)).append("' as pcn,")
                .append("case '").append(escape(updateRule)).append("'")
                .append(" when 'NO ACTION' then ").append(DatabaseMetaData.importedKeyNoAction)
                .append(" when 'CASCADE' then ").append(DatabaseMetaData.importedKeyCascade)
                .append(" when 'RESTRICT' then ").append(DatabaseMetaData.importedKeyRestrict)
                .append(" when 'SET NULL' then ").append(DatabaseMetaData.importedKeySetNull)
                .append(" when 'SET DEFAULT' then ").append(DatabaseMetaData.importedKeySetDefault).append(" end as ur, ")
                .append("case '").append(escape(deleteRule)).append("'")
                .append(" when 'NO ACTION' then ").append(DatabaseMetaData.importedKeyNoAction)
                .append(" when 'CASCADE' then ").append(DatabaseMetaData.importedKeyCascade)
                .append(" when 'RESTRICT' then ").append(DatabaseMetaData.importedKeyRestrict)
                .append(" when 'SET NULL' then ").append(DatabaseMetaData.importedKeySetNull)
                .append(" when 'SET DEFAULT' then ").append(DatabaseMetaData.importedKeySetDefault).append(" end as dr");
        }
        rs.close();

        return ((CoreStatement)stat).executeQuery(sql.append(");").toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String,
     *      java.lang.String, boolean, boolean)
     */
    public ResultSet getIndexInfo(String c, String s, String table, boolean u, boolean approximate) throws SQLException {
        ResultSet rs = null;
        Statement stat = conn.createStatement();
        StringBuilder sql = new StringBuilder(500);

        // define the column header
        // this is from the JDBC spec, it is part of the driver protocol
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, '")
                .append(escape(table)).append("' as TABLE_NAME, un as NON_UNIQUE, null as INDEX_QUALIFIER, n as INDEX_NAME, ")
                .append(Integer.toString(DatabaseMetaData.tableIndexOther)).append(" as TYPE, op as ORDINAL_POSITION, ")
                .append("cn as COLUMN_NAME, null as ASC_OR_DESC, 0 as CARDINALITY, 0 as PAGES, null as FILTER_CONDITION from (");

        // this always returns a result set now, previously threw exception
        rs = stat.executeQuery("pragma index_list('" + escape(table) + "');");

        ArrayList<ArrayList<Object>> indexList = new ArrayList<ArrayList<Object>>();
        while (rs.next()) {
            indexList.add(new ArrayList<Object>());
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

            int i = 0;
            Iterator<ArrayList<Object>> indexIterator = indexList.iterator();
            ArrayList<Object> currentIndex;

            ArrayList<String> unionAll = new ArrayList<String>();

            while (indexIterator.hasNext()) {
                currentIndex = indexIterator.next();
                String indexName = currentIndex.get(0).toString();
                rs = stat.executeQuery("pragma index_info('" + escape(indexName) + "');");

                while (rs.next()) {

                    StringBuilder sqlRow = new StringBuilder();

                    sqlRow.append("select ").append(Integer.toString(1 - (Integer) currentIndex.get(1))).append(" as un,'")
                            .append(escape(indexName)).append("' as n,")
                            .append(Integer.toString(rs.getInt(1) + 1)).append(" as op,'")
                            .append(escape(rs.getString(3))).append("' as cn");

                    unionAll.add(sqlRow.toString());
                }

                rs.close();
            }

            String sqlBlock = StringUtils.join(unionAll, " union all ");

            return ((CoreStatement) stat).executeQuery(sql.append(sqlBlock).append(");").toString(), true);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureColumns(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getProcedureColumns(String c, String s, String p, String colPat) throws SQLException {
        if (getProcedures == null) {
            getProcedureColumns = conn.prepareStatement("select null as PROCEDURE_CAT, " +
                    "null as PROCEDURE_SCHEM, null as PROCEDURE_NAME, null as COLUMN_NAME, " +
                    "null as COLUMN_TYPE, null as DATA_TYPE, null as TYPE_NAME, null as PRECISION, " +
                    "null as LENGTH, null as SCALE, null as RADIX, null as NULLABLE, " +
                    "null as REMARKS limit 0;");
        }
        return getProcedureColumns.executeQuery();

    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedures(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getProcedures(String c, String s, String p) throws SQLException {
        if (getProcedures == null) {
            getProcedures = conn.prepareStatement("select null as PROCEDURE_CAT, null as PROCEDURE_SCHEM, " +
                    "null as PROCEDURE_NAME, null as UNDEF1, null as UNDEF2, null as UNDEF3, " +
                    "null as REMARKS, null as PROCEDURE_TYPE limit 0;");
        }
        return getProcedures.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTables(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getSuperTables(String c, String s, String t) throws SQLException {
        if (getSuperTables == null) {
            getSuperTables = conn.prepareStatement("select null as TABLE_CAT, null as TABLE_SCHEM, " +
                    "null as TABLE_NAME, null as SUPERTABLE_NAME limit 0;");
        }
        return getSuperTables.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTypes(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getSuperTypes(String c, String s, String t) throws SQLException {
        if (getSuperTypes == null) {
            getSuperTypes = conn.prepareStatement("select null as TYPE_CAT, null as TYPE_SCHEM, " +
                    "null as TYPE_NAME, null as SUPERTYPE_CAT, null as SUPERTYPE_SCHEM, " +
                    "null as SUPERTYPE_NAME limit 0;");
        }
        return getSuperTypes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTablePrivileges(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getTablePrivileges(String c, String s, String t) throws SQLException {
        if (getTablePrivileges == null) {
            getTablePrivileges = conn.prepareStatement("select  null as TABLE_CAT, "
                    + "null as TABLE_SCHEM, null as TABLE_NAME, null as GRANTOR, null "
                    + "GRANTEE,  null as PRIVILEGE, null as IS_GRANTABLE limit 0;");
        }
        return getTablePrivileges.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String[])
     */
    public synchronized ResultSet getTables(String c, String s, String tblNamePattern, String types[]) throws SQLException {
        checkOpen();

        tblNamePattern = (tblNamePattern == null || "".equals(tblNamePattern)) ? "%" : escape(tblNamePattern);

        StringBuilder sql = new StringBuilder();
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, name as TABLE_NAME,")
           .append(" upper(type) as TABLE_TYPE, null as REMARKS, null as TYPE_CAT, null as TYPE_SCHEM,")
           .append(" null as TYPE_NAME, null as SELF_REFERENCING_COL_NAME, null as REF_GENERATION")
           .append(" from (select name, type from sqlite_master union all select name, type from sqlite_temp_master)")
           .append(" where TABLE_NAME like '").append(tblNamePattern).append("' and TABLE_TYPE in (");

        if (types == null || types.length == 0) {
            sql.append("'TABLE','VIEW'");
        }
        else {
            sql.append("'").append(types[0].toUpperCase()).append("'");

            for (int i = 1; i < types.length; i++) {
                sql.append(",'").append(types[i].toUpperCase()).append("'");
            }
        }

        sql.append(") order by TABLE_TYPE, TABLE_NAME;");

        return ((CoreStatement)conn.createStatement()).executeQuery(sql.toString(), true);
    }

    /**
     * @see java.sql.DatabaseMetaData#getTableTypes()
     */
    public ResultSet getTableTypes() throws SQLException {
        checkOpen();
        if (getTableTypes == null) {
            getTableTypes = conn.prepareStatement("select 'TABLE' as TABLE_TYPE "
                    + "union select 'VIEW' as TABLE_TYPE;");
        }
        getTableTypes.clearParameters();
        return getTableTypes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTypeInfo()
     */
    public ResultSet getTypeInfo() throws SQLException {
        if (getTypeInfo == null) {
            getTypeInfo = conn.prepareStatement("select " + "tn as TYPE_NAME, " + "dt as DATA_TYPE, "
                    + "0 as PRECISION, " + "null as LITERAL_PREFIX, " + "null as LITERAL_SUFFIX, "
                    + "null as CREATE_PARAMS, "
                    + DatabaseMetaData.typeNullable
                    + " as NULLABLE, "
                    + "1 as CASE_SENSITIVE, "
                    + DatabaseMetaData.typeSearchable
                    + " as SEARCHABLE, "
                    + "0 as UNSIGNED_ATTRIBUTE, "
                    + "0 as FIXED_PREC_SCALE, "
                    + "0 as AUTO_INCREMENT, "
                    + "null as LOCAL_TYPE_NAME, "
                    + "0 as MINIMUM_SCALE, "
                    + "0 as MAXIMUM_SCALE, "
                    + "0 as SQL_DATA_TYPE, "
                    + "0 as SQL_DATETIME_SUB, "
                    + "10 as NUM_PREC_RADIX from ("
                    + "    select 'BLOB' as tn, "
                    + Types.BLOB
                    + " as dt union"
                    + "    select 'NULL' as tn, "
                    + Types.NULL
                    + " as dt union"
                    + "    select 'REAL' as tn, "
                    + Types.REAL
                    + " as dt union"
                    + "    select 'TEXT' as tn, "
                    + Types.VARCHAR
                    + " as dt union"
                    + "    select 'INTEGER' as tn, "
                    + Types.INTEGER + " as dt" + ") order by TYPE_NAME;");
        }

        getTypeInfo.clearParameters();
        return getTypeInfo.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getUDTs(java.lang.String, java.lang.String, java.lang.String,
     *      int[])
     */
    public ResultSet getUDTs(String c, String s, String t, int[] types) throws SQLException {
        if (getUDTs == null) {
            getUDTs = conn.prepareStatement("select  null as TYPE_CAT, null as TYPE_SCHEM, "
                    + "null as TYPE_NAME,  null as CLASS_NAME,  null as DATA_TYPE, null as REMARKS, "
                    + "null as BASE_TYPE " + "limit 0;");
        }

        getUDTs.clearParameters();
        return getUDTs.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getVersionColumns(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getVersionColumns(String c, String s, String t) throws SQLException {
        if (getVersionColumns == null) {
            getVersionColumns = conn.prepareStatement("select null as SCOPE, null as COLUMN_NAME, "
                    + "null as DATA_TYPE, null as TYPE_NAME, null as COLUMN_SIZE, "
                    + "null as BUFFER_LENGTH, null as DECIMAL_DIGITS, null as PSEUDO_COLUMN limit 0;");
        }
        return getVersionColumns.executeQuery();
    }

    /**
     * @return Generated row id of the last INSERT command.
     * @throws SQLException
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        if (getGeneratedKeys == null) {
            getGeneratedKeys = conn.prepareStatement("select last_insert_rowid();");
        }

        return getGeneratedKeys.executeQuery();
    }

    /** Not implemented yet. */
    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }

    /** Not implemented yet. */
    public ResultSet getFunctionColumns(String a, String b, String c, String d) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }

    // inner classes

    /**
     * Pattern used to extract column order for an unnamed primary key.
     */
    protected final static Pattern PK_UNNAMED_PATTERN =
        Pattern.compile(".*\\sPRIMARY\\s+KEY\\s+\\((.*?,+.*?)\\).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Pattern used to extract a named primary key.
     */
     protected final static Pattern PK_NAMED_PATTERN =
         Pattern.compile(".*\\sCONSTRAINT\\s+(.*?)\\s+PRIMARY\\s+KEY\\s+\\((.*?)\\).*",
             Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Parses the sqlite_master table for a table's primary key
     */
    class PrimaryKeyFinder {
        /** The table name. */
        String table;

        /** The primary key name. */
        String pkName = null;

        /** The column(s) for the primary key. */
        String pkColumns[] = null;

        /**
         * Constructor.
         * @param table The table for which to get find a primary key.
         * @throws SQLException
         */
        public PrimaryKeyFinder(String table) throws SQLException {
            this.table = table;

            if (table == null || table.trim().length() == 0) {
                throw new SQLException("Invalid table name: '" + this.table + "'");
            }

            Statement stat = null;
            ResultSet rs = null;

            try {
                stat = conn.createStatement();
                // read create SQL script for table
                rs = stat.executeQuery("select sql from sqlite_master where" +
                    " lower(name) = lower('" + escape(table) + "') and type = 'table'");

                if (!rs.next())
                    throw new SQLException("Table not found: '" + table + "'");

                Matcher matcher = PK_NAMED_PATTERN.matcher(rs.getString(1));
                if (matcher.find()){
                    pkName = '\'' + escape(matcher.group(1).toLowerCase()) + '\'';
                    pkColumns = matcher.group(2).split(",");
                }
                else {
                    matcher = PK_UNNAMED_PATTERN.matcher(rs.getString(1));
                    if (matcher.find()){
                        pkColumns = matcher.group(1).split(",");
                    }
                }

                if (pkColumns == null) {
                    rs = stat.executeQuery("pragma table_info('" + escape(table) + "');");
                    while(rs.next()) {
                        if (rs.getBoolean(6))
                            pkColumns = new String[]{rs.getString(2)};
                    }
                }

                if (pkColumns != null)
                    for (int i = 0; i < pkColumns.length; i++) {
                        pkColumns[i] = pkColumns[i].toLowerCase().trim();
                    }
            }
            finally {
                try {
                    if (rs != null) rs.close();
                } catch (Exception e) {}
                try {
                    if (stat != null) stat.close();
                } catch (Exception e) {}
            }
        }

        /**
         * @return The primary key name if any.
         */
        public String getName() {
            return pkName;
        }

        /**
         * @return Array of primary key column(s) if any.
         */
        public String[] getColumns() {
            return pkColumns;
        }
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        close();
    }
}
