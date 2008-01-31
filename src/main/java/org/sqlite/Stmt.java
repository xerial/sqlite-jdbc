/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
class Stmt extends RS implements Statement, Codes
{
    private ArrayList batch = null;

    Stmt(Conn conn)
    {
        super(conn);
    }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException
    {
        if (pointer == 0)
            throw new SQLException("SQLite JDBC internal error: pointer == 0 on exec.");
        if (isRS())
            throw new SQLException("SQLite JDBC internal error: isRS() on exec.");

        boolean rc = false;
        try
        {
            rc = db.execute(this, null);
        }
        finally
        {
            resultsWaiting = rc;
        }

        return db.column_count(pointer) != 0;
    }

    // PUBLIC INTERFACE /////////////////////////////////////////////

    public Statement getStatement()
    {
        return this;
    }

    /**
     * More lax than JDBC spec, a Statement can be reused after close(). This is
     * to support Stmt and RS sharing a heap object.
     */
    public void close() throws SQLException
    {
        if (pointer == 0)
            return;
        clearRS();
        colsMeta = null;
        meta = null;
        batch = null;
        int resp = db.finalize(this);
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE)
            db.throwex();
    }

    /**
     * The JVM does not ensure finalize() is called, so a Map in the DB class
     * keeps track of statements for finalization.
     */
    protected void finalize() throws SQLException
    {
        close();
    }

    public int getUpdateCount() throws SQLException
    {
        checkOpen();
        if (pointer == 0 || resultsWaiting)
            return -1;
        return db.changes();
    }

    public boolean execute(String sql) throws SQLException
    {
        checkOpen();
        close();
        this.sql = sql;
        db.prepare(this);
        return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        checkOpen();
        close();
        this.sql = sql;
        db.prepare(this);
        if (!exec())
        {
            close();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException
    {
        checkOpen();
        close();
        this.sql = sql;
        int changes = 0;
        try
        {
            db.prepare(this);
            changes = db.executeUpdate(this, null);
        }
        finally
        {
            close();
        }
        return changes;
    }

    public void addBatch(String sql) throws SQLException
    {
        checkOpen();
        if (batch == null)
            batch = new ArrayList();
        batch.add(sql);
    }

    public void clearBatch() throws SQLException
    {
        checkOpen();
        if (batch != null)
            batch.clear();
    }

    public int[] executeBatch() throws SQLException
    {
        // TODO: optimise
        checkOpen();
        close();
        if (batch == null)
            return new int[] {};

        int[] changes = new int[batch.size()];

        synchronized (db)
        {
            try
            {
                for (int i = 0; i < changes.length; i++)
                {
                    try
                    {
                        sql = (String) batch.get(i);
                        db.prepare(this);
                        changes[i] = db.executeUpdate(this, null);
                    }
                    catch (SQLException e)
                    {
                        throw new BatchUpdateException("batch entry " + i + ": " + e.getMessage(), changes);
                    }
                    finally
                    {
                        db.finalize(this);
                    }
                }
            }
            finally
            {
                batch.clear();
            }
        }

        return changes;
    }

    // public boolean isClosed() throws SQLException
    // {
    // return false;
    // }
    //
    // public boolean isPoolable() throws SQLException
    // {
    // return false;
    // }

    // public void setPoolable(boolean poolable) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public boolean isWrapperFor(Class< ? > iface) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    // }
    //
    // public <T> T unwrap(Class<T> iface) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public int getHoldability() throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public Reader getNCharacterStream(int columnIndex) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public Reader getNCharacterStream(String columnLabel) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public NClob getNClob(int columnIndex) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public NClob getNClob(String columnLabel) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public String getNString(int columnIndex) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public String getNString(String columnLabel) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public RowId getRowId(int columnIndex) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public RowId getRowId(String columnLabel) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public SQLXML getSQLXML(int columnIndex) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public SQLXML getSQLXML(String columnLabel) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateAsciiStream(int columnIndex, InputStream x) throws
    // SQLException
    // {
    // throw new SQLException("not yet implemented");
    // // TODO Auto-generated method stub
    //
    // }
    //
    // public void updateAsciiStream(String columnLabel, InputStream x) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateAsciiStream(int columnIndex, InputStream x, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateAsciiStream(String columnLabel, InputStream x, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBinaryStream(int columnIndex, InputStream x) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBinaryStream(String columnLabel, InputStream x) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBinaryStream(int columnIndex, InputStream x, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBinaryStream(String columnLabel, InputStream x, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBlob(int columnIndex, InputStream inputStream) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBlob(String columnLabel, InputStream inputStream)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBlob(int columnIndex, InputStream inputStream, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateBlob(String columnLabel, InputStream inputStream, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateCharacterStream(int columnIndex, Reader x) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateCharacterStream(String columnLabel, Reader reader)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateCharacterStream(int columnIndex, Reader x, long length)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateCharacterStream(String columnLabel, Reader reader, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateClob(int columnIndex, Reader reader) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateClob(String columnLabel, Reader reader) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateClob(int columnIndex, Reader reader, long length)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateClob(String columnLabel, Reader reader, long length)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNCharacterStream(int columnIndex, Reader x) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNCharacterStream(String columnLabel, Reader reader)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNCharacterStream(int columnIndex, Reader x, long
    // length) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNCharacterStream(String columnLabel, Reader reader,
    // long length) throws SQLException
    // {
    // throw new SQLException("not yet implemented");
    // // TODO Auto-generated method stub
    //
    // }
    //
    // public void updateNClob(int columnIndex, NClob clob) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNClob(String columnLabel, NClob clob) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNClob(int columnIndex, Reader reader) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNClob(String columnLabel, Reader reader) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNClob(int columnIndex, Reader reader, long length)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNClob(String columnLabel, Reader reader, long length)
    // throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNString(int columnIndex, String string) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateNString(String columnLabel, String string) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateRowId(int columnIndex, RowId x) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateRowId(String columnLabel, RowId x) throws SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }
    //
    // public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws
    // SQLException
    // {
    // // TODO Auto-generated method stub
    // throw new SQLException("not yet implemented");
    //
    // }

}
