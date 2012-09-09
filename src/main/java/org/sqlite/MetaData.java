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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MetaData implements DatabaseMetaData
{
    /**
     * Pattern used to extract column order for an unnamed primary key.
     */
    protected final static Pattern PK_UNNAMED =
            Pattern.compile(".* primary +key *\\((.*?,+.*?)\\).*", Pattern.CASE_INSENSITIVE);

    /**
    * Pattern used to extract a named primary key.
    */
    protected final static Pattern PK_NAMED =
        Pattern.compile(".* constraint +(.*?) +primary +key *\\((.*?)\\).*", Pattern.CASE_INSENSITIVE);

    private Conn              conn;
    private PreparedStatement
            getTables             = null,   getTableTypes        = null,
            getTypeInfo           = null,   getCatalogs          = null,
            getSchemas            = null,   getUDTs              = null,
            getColumnsTblName     = null,   getSuperTypes        = null,
            getSuperTables        = null,   getTablePrivileges   = null,
            getIndexInfo          = null,   getProcedures        = null,
            getProcedureColumns   = null,   getAttributes        = null,
            getBestRowIdentifier  = null,   getVersionColumns    = null,
            getColumnPrivileges   = null;

    /**
     * Used by PrepStmt to save generating a new statement every call.
     */
    private PreparedStatement getGeneratedKeys = null;

    /**
     * Constructor that applies the Connection object.
     * @param conn Connection object.
     */
    MetaData(Conn conn) {
        this.conn = conn;
    }

    /**
     * @throws SQLException
     */
    void checkOpen() throws SQLException {
        if (conn == null) {
            throw new SQLException("connection closed");
        }
    }

    /**
     * @throws SQLException
     */
    synchronized void close() throws SQLException {
        if (conn == null) {
            return;
        }

        try {
            if (getTables != null) {
                getTables.close();
            }
            if (getTableTypes != null) {
                getTableTypes.close();
            }
            if (getTypeInfo != null) {
                getTypeInfo.close();
            }
            if (getCatalogs != null) {
                getCatalogs.close();
            }
            if (getSchemas != null) {
                getSchemas.close();
            }
            if (getUDTs != null) {
                getUDTs.close();
            }
            if (getColumnsTblName != null) {
                getColumnsTblName.close();
            }
            if (getSuperTypes != null) {
                getSuperTypes.close();
            }
            if (getSuperTables != null) {
                getSuperTables.close();
            }
            if (getTablePrivileges != null) {
                getTablePrivileges.close();
            }
            if (getIndexInfo != null) {
                getIndexInfo.close();
            }
            if (getProcedures != null) {
                getProcedures.close();
            }
            if (getProcedureColumns != null) {
                getProcedureColumns.close();
            }
            if (getAttributes != null) {
                getAttributes.close();
            }
            if (getBestRowIdentifier != null) {
                getBestRowIdentifier.close();
            }
            if (getVersionColumns != null) {
                getVersionColumns.close();
            }
            if (getColumnPrivileges != null) {
                getColumnPrivileges.close();
            }
            if (getGeneratedKeys != null) {
                getGeneratedKeys.close();
            }

            getTables = null;
            getTableTypes = null;
            getTypeInfo = null;
            getCatalogs = null;
            getSchemas = null;
            getUDTs = null;
            getColumnsTblName = null;
            getSuperTypes = null;
            getSuperTables = null;
            getTablePrivileges = null;
            getIndexInfo = null;
            getProcedures = null;
            getProcedureColumns = null;
            getAttributes = null;
            getBestRowIdentifier = null;
            getVersionColumns = null;
            getColumnPrivileges = null;
            getGeneratedKeys = null;
        }
        finally {
            conn = null;
        }
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
        return sqlStateSQL99;
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
        return false;
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
            getAttributes = conn.prepareStatement("select " + "null as TYPE_CAT, " + "null as TYPE_SCHEM, "
                    + "null as TYPE_NAME, " + "null as ATTR_NAME, " + "null as DATA_TYPE, "
                    + "null as ATTR_TYPE_NAME, " + "null as ATTR_SIZE, " + "null as DECIMAL_DIGITS, "
                    + "null as NUM_PREC_RADIX, " + "null as NULLABLE, " + "null as REMARKS, " + "null as ATTR_DEF, "
                    + "null as SQL_DATA_TYPE, " + "null as SQL_DATETIME_SUB, " + "null as CHAR_OCTET_LENGTH, "
                    + "null as ORDINAL_POSITION, " + "null as IS_NULLABLE, " + "null as SCOPE_CATALOG, "
                    + "null as SCOPE_SCHEMA, " + "null as SCOPE_TABLE, " + "null as SOURCE_DATA_TYPE limit 0;");
        }
        return getAttributes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getBestRowIdentifier(java.lang.String, java.lang.String,
     *      java.lang.String, int, boolean)
     */
    public ResultSet getBestRowIdentifier(String c, String s, String t, int scope, boolean n) throws SQLException {
        if (getBestRowIdentifier == null) {
            getBestRowIdentifier = conn.prepareStatement("select " + "null as SCOPE, " + "null as COLUMN_NAME, "
                    + "null as DATA_TYPE, " + "null as TYPE_NAME, " + "null as COLUMN_SIZE, "
                    + "null as BUFFER_LENGTH, " + "null as DECIMAL_DIGITS, " + "null as PSEUDO_COLUMN limit 0;");
        }
        return getBestRowIdentifier.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getColumnPrivileges(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getColumnPrivileges(String c, String s, String t, String colPat) throws SQLException {
        if (getColumnPrivileges == null) {
            getColumnPrivileges = conn.prepareStatement("select " + "null as TABLE_CAT, " + "null as TABLE_SCHEM, "
                    + "null as TABLE_NAME, " + "null as COLUMN_NAME, " + "null as GRANTOR, " + "null as GRANTEE, "
                    + "null as PRIVILEGE, " + "null as IS_GRANTABLE limit 0;");
        }
        return getColumnPrivileges.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getColumns(String c, String s, String tbl, String colPat) throws SQLException {
        Statement stat = conn.createStatement();
        ResultSet rs;
        String sql;

        checkOpen();

        if (getColumnsTblName == null) {
            getColumnsTblName = conn.prepareStatement("select tbl_name from sqlite_master where tbl_name like ?;");
        }

        // determine exact table name
        getColumnsTblName.setString(1, tbl);
        rs = getColumnsTblName.executeQuery();
        if (!rs.next()) {
            return rs;
        }
        tbl = rs.getString(1);
        rs.close();

        sql = "select " + "null as TABLE_CAT, " + "null as TABLE_SCHEM, " + "'" + escape(tbl) + "' as TABLE_NAME, "
                + "cn as COLUMN_NAME, " + "ct as DATA_TYPE, " + "tn as TYPE_NAME, " + "2000000000 as COLUMN_SIZE, "
                + "2000000000 as BUFFER_LENGTH, " + "10   as DECIMAL_DIGITS, " + "10   as NUM_PREC_RADIX, "
                + "colnullable as NULLABLE, " + "null as REMARKS, " + "colDefault as COLUMN_DEF, "
                + "0    as SQL_DATA_TYPE, " + "0    as SQL_DATETIME_SUB, " + "2000000000 as CHAR_OCTET_LENGTH, "
                + "ordpos as ORDINAL_POSITION, " + "(case colnullable when 0 then 'NO' when 1 then 'YES' else '' end)"
                + "    as IS_NULLABLE, " + "null as SCOPE_CATLOG, " + "null as SCOPE_SCHEMA, "
                + "null as SCOPE_TABLE, " + "null as SOURCE_DATA_TYPE from (";

        // the command "pragma table_info('tablename')" does not embed
        // like a normal select statement so we must extract the information
        // and then build a resultset from unioned select statements
        rs = stat.executeQuery("pragma table_info ('" + escape(tbl) + "');");

        boolean colFound = false;
        for (int i = 0; rs.next(); i++) {
            String colName = rs.getString(2);
            String colType = rs.getString(3);
            String colNotNull = rs.getString(4);
            String colDefault = rs.getString(5);

            int colNullable = 2;
            if (colNotNull != null) {
                colNullable = colNotNull.equals("0") ? 1 : 0;
            }
            if (colFound) {
                sql += " union all ";
            }
            colFound = true;

            //            colType = colType == null ? "TEXT" : colType.toUpperCase();
            //            int colJavaType = -1;
            //            if (colType.equals("INT") || colType.equals("INTEGER"))
            //                colJavaType = Types.INTEGER;
            //            else if (colType.equals("TEXT"))
            //                colJavaType = Types.VARCHAR;
            //            else if (colType.equals("FLOAT"))
            //                colJavaType = Types.FLOAT;
            //            else
            //                colJavaType = Types.VARCHAR;

            /*
             * improved column types
             * ref http://www.sqlite.org/datatype3.html - 2.1 Determination Of Column
            Affinity
             * plus some degree of artistic-license applied
             */
            colType = colType == null ? "TEXT" : colType.toUpperCase();
            int colJavaType = -1;
            // rule #1 + boolean
            if (colType.matches(".*(INT|BOOL).*")) {
                colJavaType = Types.INTEGER;
            }
            else if (colType.matches(".*(CHAR|CLOB|TEXT|BLOB).*")) {
                colJavaType = Types.VARCHAR;
            }
            else if (colType.matches(".*(REAL|FLOA|DOUB|DEC|NUM).*")) {
                colJavaType = Types.FLOAT;
            }
            else {
                // catch-all
                colJavaType = Types.VARCHAR;
            }

            sql += "select " + i + " as ordpos, "
                    + colNullable + " as colnullable, '"
                    + colJavaType + "' as ct, '"
                    + escape(colName) + "' as cn, '"
                    + escape(colType) + "' as tn, "
                    + quote(colDefault == null ? null : escape(colDefault)) + " as colDefault";

            if (colPat != null) {
                sql += " where upper(cn) like upper('" + escape(colPat) + "')";
            }
        }
        sql += colFound ? ");" : "select null as ordpos, null as colnullable, null as cn, null as tn, null as colDefault) limit 0;";
        rs.close();

        return stat.executeQuery(sql);
    }

    /**
     * @see java.sql.DatabaseMetaData#getCrossReference(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public ResultSet getCrossReference(String pc, String ps, String pt, String fc, String fs, String ft)
            throws SQLException {

        if (pt == null) {
            return getExportedKeys(fc, fs, ft);
        }
        if (ft == null) {
            return getImportedKeys(pc, ps, pt);
        }

        StringBuilder query = new StringBuilder();
        query.append(String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ", quote(pc),
                quote(ps), quote(pt))
                + "'' as PKCOLUMN_NAME, "
                + String.format("%s as FKTABLE_CAT, %s as FKTABLE_SCHEM,  %s as FKTABLE_NAME, ", quote(fc), quote(fs),
                        quote(ft))
                + "'' as FKCOLUMN_NAME, -1 as KEY_SEQ, 3 as UPDATE_RULE, "
                + "3 as DELETE_RULE, '' as FK_NAME, '' as PK_NAME, "
                + Integer.toString(importedKeyInitiallyDeferred)
                + " as DEFERRABILITY limit 0;");
        return conn.createStatement().executeQuery(query.toString());
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemas()
     */
    public ResultSet getSchemas() throws SQLException {
        if (getSchemas == null) {
            getSchemas = conn.prepareStatement("select " + "null as TABLE_SCHEM, " + "null as TABLE_CATALOG "
                    + "limit 0;");
        }
        getSchemas.clearParameters();
        return getSchemas.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogs()
     */
    public ResultSet getCatalogs() throws SQLException {
        if (getCatalogs == null) {
            getCatalogs = conn.prepareStatement("select null as TABLE_CAT limit 0;");
        }
        getCatalogs.clearParameters();
        return getCatalogs.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getPrimaryKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getPrimaryKeys(String c, String s, String table) throws SQLException {
        String[] columnNames = null;
        String pkName = null;
        StringBuilder sql = new StringBuilder(512);
        sql.append("select null as TABLE_CAT, null as TABLE_SCHEM, '")
           .append(escape(table))
           .append("' as TABLE_NAME, cn as COLUMN_NAME, ks as KEY_SEQ, pk as PK_NAME from (");

        Statement stat = conn.createStatement();
        // read create SQL script for table
        ResultSet rs = stat.executeQuery("select sql from sqlite_master where" +
            " upper(name) = upper('" + escape(table) + "')");
        rs.next();

        Matcher matcher = PK_NAMED.matcher(rs.getString(1));
        if (matcher.find()){
            pkName = '\'' + escape(matcher.group(1)) + '\'';
            columnNames = matcher.group(2).split(",");
        }
        else {
            matcher = PK_UNNAMED.matcher(rs.getString(1));
            if (matcher.find()){
                columnNames = matcher.group(1).split(",");
            }
        }
        rs.close();

        if (columnNames != null) {
            for (int i = 0; i < columnNames.length; i++) {
                if (i > 0) sql.append(" union ");
                sql.append("select ").append(pkName).append(" as pk, '")
                   .append(escape(columnNames[i].trim())).append("' as cn, ")
                   .append(i).append(" as ks");
            }

            return stat.executeQuery(sql.append(") order by cn;").toString());
        }

        rs = stat.executeQuery("pragma table_info('" + escape(table) + "');");
        int i;
        for (i = 0; rs.next(); i++) {
            String colName = rs.getString(2);

            if (!rs.getBoolean(6)) {
                i--;
                continue;
            }
            if (i > 0) {
                sql.append(" union all ");
            }

            sql.append("select null as pk, 0 as ks, '")
               .append(escape(colName)).append("' as cn");
        }
        sql.append(i == 0 ? "select null as cn, null as pk, 0 as ks) order by cn limit 0;" :
                            ") order by cn;");
        rs.close();

        return stat.executeQuery(sql.toString());
    }

    /**
     * Adds SQL string quotes to the given string.
     * @param tableName The string to quote.
     * @return The quoted string.
     */
    private static String quote(String tableName) {
        if (tableName == null) {
            return "null";
        }
        else {
            return String.format("'%s'", tableName);
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getExportedKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {

        StringBuilder exportedKeysQuery = new StringBuilder();
        exportedKeysQuery.append(String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ",
                quote(catalog), quote(schema), quote(table))
                + String.format("pcn as PKCOLUMN_NAME, %s as FKTABLE_CAT, %s as FKTABLE_SCHEM, ", quote(catalog),
                        quote(schema))
                + "fkn as FKTABLE_NAME, fcn as FKCOLUMN_NAME, "
                + "ks as KEY_SEQ, "
                + "ur as UPDATE_RULE, "
                + "dr as DELETE_RULE, "
                + "'' as FK_NAME, "
                + "'' as PK_NAME, "
                + Integer.toString(importedKeyInitiallyDeferred) + " as DEFERRABILITY from (");

        // retrieve table list
        String tableListQuery = String.format("select name from sqlite_master where type = 'table'");
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(tableListQuery);
        ArrayList<String> tableList = new ArrayList<String>();
        while (rs.next()) {
            tableList.add(rs.getString(1));
        }
        rs.close();

        // find imported keys for each table
        int count = 0;
        for (String targetTable : tableList) {
            String foreignKeyQuery = String.format("pragma foreign_key_list('%s');", escape(targetTable));

            try {
                ResultSet fk = stat.executeQuery(foreignKeyQuery);
                for (; fk.next();) {
                    int keySeq = fk.getInt(2) + 1;
                    String PKTabName = fk.getString(3);
                    String FKColName = fk.getString(4);
                    String PKColName = fk.getString(5);
                    String updateRule = fk.getString(6);
                    String deleteRule = fk.getString(7);

                    if (PKTabName == null || !PKTabName.equals(table)) {
                        continue;
                    }

                    if (count > 0) {
                        exportedKeysQuery.append(" union all ");
                    }

                    exportedKeysQuery.append("select " + Integer.toString(keySeq) + " as ks," + "'"
                            + escape(targetTable) + "' as fkn," + "'" + escape(FKColName) + "' as fcn," + "'"
                            + escape(PKColName) + "' as pcn," + String.format("case '%s' ", escape(updateRule))
                            + String.format("when 'NO ACTION' then %d ", importedKeyNoAction)
                            + String.format("when 'CASCADE' then %d ", importedKeyCascade)
                            + String.format("when 'RESTRICT' then %d  ", importedKeyRestrict)
                            + String.format("when 'SET NULL' then %d  ", importedKeySetNull)
                            + String.format("when 'SET DEFAULT' then %d  ", importedKeySetDefault) + "end as ur,"
                            + String.format("case '%s' ", escape(deleteRule))
                            + String.format("when 'NO ACTION' then %d ", importedKeyNoAction)
                            + String.format("when 'CASCADE' then %d ", importedKeyCascade)
                            + String.format("when 'RESTRICT' then %d  ", importedKeyRestrict)
                            + String.format("when 'SET NULL' then %d  ", importedKeySetNull)
                            + String.format("when 'SET DEFAULT' then %d  ", importedKeySetDefault) + "end as dr");

                    count++;
                }

                exportedKeysQuery.append(");");
                fk.close();
            }
            catch (SQLException e) {
                // continue
            }
        }

        String sql = (count > 0) ? exportedKeysQuery.toString() : (String.format(
                "select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, %s as PKTABLE_NAME, ", quote(catalog), quote(schema),
                quote(table))
                + "'' as PKCOLUMN_NAME, "
                + String.format("%s as FKTABLE_CAT, %s as FKTABLE_SCHEM, ", quote(catalog), quote(schema))
                + "'' as FKTABLE_NAME, "
                + "'' as FKCOLUMN_NAME, "
                + "-1 as KEY_SEQ, "
                + "3 as UPDATE_RULE, "
                + "3 as DELETE_RULE, " + "'' as FK_NAME, " + "'' as PK_NAME, " + "5 as DEFERRABILITY limit 0;");
        return stat.executeQuery(sql);
    }

    /**
     * @see java.sql.DatabaseMetaData#getImportedKeys(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        String sql;
        ResultSet rs = null;
        Statement stat = conn.createStatement();

        sql = String.format("select %s as PKTABLE_CAT, %s as PKTABLE_SCHEM, ", quote(catalog), quote(schema))
                + String.format(
                        "ptn as PKTABLE_NAME, pcn as PKCOLUMN_NAME, %s as FKTABLE_CAT, %s as FKTABLE_SCHEM, %s as FKTABLE_NAME, ",
                        quote(catalog), quote(schema), quote(table)) + "fcn as FKCOLUMN_NAME, " + "ks as KEY_SEQ, "
                + "ur as UPDATE_RULE, " + "dr as DELETE_RULE, " + "'' as FK_NAME, " + "'' as PK_NAME, "
                + Integer.toString(importedKeyInitiallyDeferred) + " as DEFERRABILITY from (";

        // Use a try catch block to avoid "query does not return ResultSet" error
        try {
            rs = stat.executeQuery("pragma foreign_key_list('" + escape(table) + "');");
            int i;
            for (i = 0; rs.next(); i++) {
                int keySeq = rs.getInt(2) + 1;
                String PKTabName = rs.getString(3);
                String FKColName = rs.getString(4);
                String PKColName = rs.getString(5);
                String updateRule = rs.getString(6);
                String deleteRule = rs.getString(7);

                if (i > 0) {
                    sql += " union all ";
                }

                sql += String.format("select %d as ks,", keySeq)
                        + String.format("'%s' as ptn, '%s' as fcn, '%s' as pcn,", escape(PKTabName), escape(FKColName),
                                escape(PKColName)) + String.format("case '%s' ", escape(updateRule))
                        + String.format("when 'NO ACTION' then %d ", importedKeyNoAction)
                        + String.format("when 'CASCADE' then %d ", importedKeyCascade)
                        + String.format("when 'RESTRICT' then %d  ", importedKeyRestrict)
                        + String.format("when 'SET NULL' then %d  ", importedKeySetNull)
                        + String.format("when 'SET DEFAULT' then %d  ", importedKeySetDefault) + "end as ur,"
                        + String.format("case '%s' ", escape(deleteRule))
                        + String.format("when 'NO ACTION' then %d ", importedKeyNoAction)
                        + String.format("when 'CASCADE' then %d ", importedKeyCascade)
                        + String.format("when 'RESTRICT' then %d  ", importedKeyRestrict)
                        + String.format("when 'SET NULL' then %d  ", importedKeySetNull)
                        + String.format("when 'SET DEFAULT' then %d  ", importedKeySetDefault) + "end as dr";
            }
            sql += ");";
            rs.close();
        }
        catch (SQLException e) {
            sql += "select -1 as ks, '' as ptn, '' as fcn, '' as pcn, " + importedKeyNoAction + " as ur, "
                    + importedKeyNoAction + " as dr) limit 0;";
        }

        return stat.executeQuery(sql);
    }

    /**
     * @see java.sql.DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String,
     *      java.lang.String, boolean, boolean)
     */
    public ResultSet getIndexInfo(String c, String s, String t, boolean u, boolean approximate) throws SQLException {
        String sql;
        ResultSet rs = null;
        Statement stat = conn.createStatement();

        sql = "select " + "null as TABLE_CAT, " + "null as TABLE_SCHEM, " + "'" + escape(t) + "' as TABLE_NAME, "
                + "un as NON_UNIQUE, " + "null as INDEX_QUALIFIER, " + "n as INDEX_NAME, "
                + Integer.toString(tableIndexOther) + " as TYPE, " + "op as ORDINAL_POSITION, " + "cn as COLUMN_NAME, "
                + "null as ASC_OR_DESC, " + "0 as CARDINALITY, " + "0 as PAGES, " + "null as FILTER_CONDITION from (";

        // Use a try catch block to avoid "query does not return ResultSet" error
        try {
            ArrayList<ArrayList> indexList = new ArrayList<ArrayList>();

            rs = stat.executeQuery("pragma index_list('" + escape(t) + "');");
            while (rs.next()) {
                indexList.add(new ArrayList());
                indexList.get(indexList.size() - 1).add(rs.getString(2));
                indexList.get(indexList.size() - 1).add(rs.getInt(3));
            }
            rs.close();

            int i = 0;
            Iterator indexIterator = indexList.iterator();
            ArrayList currentIndex;
            while (indexIterator.hasNext()) {
                currentIndex = (ArrayList) indexIterator.next();
                String indexName = currentIndex.get(0).toString();
                int unique = (Integer) currentIndex.get(1);

                rs = stat.executeQuery("pragma index_info('" + escape(indexName) + "');");
                for (; rs.next(); i++) {

                    int ordinalPosition = rs.getInt(1) + 1;
                    String colName = rs.getString(3);

                    if (i > 0) {
                        sql += " union all ";
                    }

                    sql += "select " + Integer.toString(1 - unique) + " as un," + "'" + escape(indexName) + "' as n,"
                            + Integer.toString(ordinalPosition) + " as op," + "'" + escape(colName) + "' as cn";
                    i++;
                }
                rs.close();
            }
            sql += ");";
        }
        catch (SQLException e) {
            sql += "select null as un, null as n, null as op, null as cn) limit 0;";
        }

        return stat.executeQuery(sql);
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureColumns(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ResultSet getProcedureColumns(String c, String s, String p, String colPat) throws SQLException {
        if (getProcedures == null) {
            getProcedureColumns = conn.prepareStatement("select " + "null as PROCEDURE_CAT, "
                    + "null as PROCEDURE_SCHEM, " + "null as PROCEDURE_NAME, " + "null as COLUMN_NAME, "
                    + "null as COLUMN_TYPE, " + "null as DATA_TYPE, " + "null as TYPE_NAME, " + "null as PRECISION, "
                    + "null as LENGTH, " + "null as SCALE, " + "null as RADIX, " + "null as NULLABLE, "
                    + "null as REMARKS limit 0;");
        }
        return getProcedureColumns.executeQuery();

    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedures(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getProcedures(String c, String s, String p) throws SQLException {
        if (getProcedures == null) {
            getProcedures = conn.prepareStatement("select " + "null as PROCEDURE_CAT, " + "null as PROCEDURE_SCHEM, "
                    + "null as PROCEDURE_NAME, " + "null as UNDEF1, " + "null as UNDEF2, " + "null as UNDEF3, "
                    + "null as REMARKS, " + "null as PROCEDURE_TYPE limit 0;");
        }
        return getProcedures.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTables(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getSuperTables(String c, String s, String t) throws SQLException {
        if (getSuperTables == null) {
            getSuperTables = conn.prepareStatement("select " + "null as TABLE_CAT, " + "null as TABLE_SCHEM, "
                    + "null as TABLE_NAME, " + "null as SUPERTABLE_NAME limit 0;");
        }
        return getSuperTables.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTypes(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getSuperTypes(String c, String s, String t) throws SQLException {
        if (getSuperTypes == null) {
            getSuperTypes = conn.prepareStatement("select " + "null as TYPE_CAT, " + "null as TYPE_SCHEM, "
                    + "null as TYPE_NAME, " + "null as SUPERTYPE_CAT, " + "null as SUPERTYPE_SCHEM, "
                    + "null as SUPERTYPE_NAME limit 0;");
        }
        return getSuperTypes.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTablePrivileges(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public ResultSet getTablePrivileges(String c, String s, String t) throws SQLException {
        if (getTablePrivileges == null) {
            getTablePrivileges = conn.prepareStatement("select " + "null as TABLE_CAT, " + "null as TABLE_SCHEM, "
                    + "null as TABLE_NAME, " + "null as GRANTOR, " + "null as GRANTEE, " + "null as PRIVILEGE, "
                    + "null as IS_GRANTABLE limit 0;");
        }
        return getTablePrivileges.executeQuery();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String[])
     */
    public synchronized ResultSet getTables(String c, String s, String t, String[] types) throws SQLException {
        checkOpen();

        t = (t == null || "".equals(t)) ? "%" : t.toUpperCase();

        String sql = "select" + " null as TABLE_CAT," + " null as TABLE_SCHEM," + " name as TABLE_NAME,"
                + " upper(type) as TABLE_TYPE," + " null as REMARKS," + " null as TYPE_CAT," + " null as TYPE_SCHEM,"
                + " null as TYPE_NAME," + " null as SELF_REFERENCING_COL_NAME," + " null as REF_GENERATION"
                + " from (select name, type from sqlite_master union all"
                + "       select name, type from sqlite_temp_master)" + " where TABLE_NAME like '" + escape(t) + "'";

        if (types != null) {
            sql += " and TABLE_TYPE in (";
            for (int i = 0; i < types.length; i++) {
                if (i > 0) {
                    sql += ", ";
                }
                sql += "'" + types[i].toUpperCase() + "'";
            }
            sql += ")";
        }

        sql += ";";

        return conn.createStatement().executeQuery(sql);
    }

    /**
     * @see java.sql.DatabaseMetaData#getTableTypes()
     */
    public ResultSet getTableTypes() throws SQLException {
        checkOpen();
        if (getTableTypes == null) {
            getTableTypes = conn.prepareStatement("select 'TABLE' as TABLE_TYPE"
                    + " union select 'VIEW' as TABLE_TYPE;");
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
                    + typeNullable
                    + " as NULLABLE, "
                    + "1 as CASE_SENSITIVE, "
                    + typeSearchable
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
            getUDTs = conn.prepareStatement("select " + "null as TYPE_CAT, " + "null as TYPE_SCHEM, "
                    + "null as TYPE_NAME, " + "null as CLASS_NAME, " + "null as DATA_TYPE, " + "null as REMARKS, "
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
            getVersionColumns = conn.prepareStatement("select " + "null as SCOPE, " + "null as COLUMN_NAME, "
                    + "null as DATA_TYPE, " + "null as TYPE_NAME, " + "null as COLUMN_SIZE, "
                    + "null as BUFFER_LENGTH, " + "null as DECIMAL_DIGITS, " + "null as PSEUDO_COLUMN limit 0;");
        }
        return getVersionColumns.executeQuery();
    }

    /**
     * @return Generated row id of the last INSERT command.
     * @throws SQLException
     */
    ResultSet getGeneratedKeys() throws SQLException {
        if (getGeneratedKeys == null) {
            getGeneratedKeys = conn.prepareStatement("select last_insert_rowid();");
        }
        return getGeneratedKeys.executeQuery();
    }

    /**
     * Applies SQL escapes for special characters in a given string.
     * @param val The string to escape.
     * @return The SQL escaped string.
     */
    private String escape(final String val) {
        // TODO: this function is ugly, pass this work off to SQLite, then we
        //       don't have to worry about Unicode 4, other characters needing
        //       escaping, etc.
        int len = val.length();
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            if (val.charAt(i) == '\'') {
                buf.append('\'');
            }
            buf.append(val.charAt(i));
        }
        return buf.toString();
    }

    /** Not implemented yet. */
    public Struct createStruct(String t, Object[] attr) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }

    /** Not implemented yet. */
    public ResultSet getFunctionColumns(String a, String b, String c, String d) throws SQLException {
        throw new SQLException("Not yet implemented by SQLite JDBC driver");
    }
}
