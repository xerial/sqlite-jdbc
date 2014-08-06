package org.sqlite.jdbc4;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import org.sqlite.SQLiteConnection;
import org.sqlite.jdbc3.JDBC3DatabaseMetaData;

public class JDBC4DatabaseMetaData extends JDBC3DatabaseMetaData implements DatabaseMetaData
{
    public JDBC4DatabaseMetaData(SQLiteConnection conn) {
        super(conn);
    }

    // JDBC 4
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public ResultSet getSchemas(String catalog, String schemaPattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public ResultSet getFunctions(String catalog, String schemaPattern,
            String functionNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        // TODO
        return null;
    }
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        // TODO
        return false;
    }
}
