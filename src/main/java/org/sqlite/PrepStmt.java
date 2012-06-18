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

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

final class PrepStmt extends Stmt implements PreparedStatement, ParameterMetaData, Codes
{
   private int columnCount;
   private int paramCount;

   PrepStmt(Conn conn, String sql) throws SQLException {
      super(conn);

      this.sql = sql;
      db.prepare(this);
      rs.colsMeta = db.column_names(pointer);
      columnCount = db.column_count(pointer);
      paramCount = db.bind_parameter_count(pointer);
      batch = new Object[paramCount];
      batchPos = 0;
   }

   /**
    * @see java.sql.PreparedStatement#clearParameters()
    */
   public void clearParameters() throws SQLException {
      checkOpen();
      db.reset(pointer);
      clearBatch();
   }

   /**
    * @see org.sqlite.Stmt#finalize()
    */
   @Override
   protected void finalize() throws SQLException {
       close();
   }

   /**
    * @see java.sql.PreparedStatement#execute()
    */
   public boolean execute() throws SQLException {
       checkOpen();
       rs.close();
       db.reset(pointer);
       resultsWaiting = db.execute(this, batch);
       return columnCount != 0;
   }

   /**
    * @see java.sql.PreparedStatement#executeQuery()
    */
   public ResultSet executeQuery() throws SQLException {
      checkOpen();
      if (columnCount == 0)
          throw new SQLException("query does not return results");
      rs.close();
      db.reset(pointer);
      resultsWaiting = db.execute(this, batch);
      return getResultSet();
   }

   /**
    * @see java.sql.PreparedStatement#executeUpdate()
    */
   public int executeUpdate() throws SQLException {
      checkOpen();
      if (columnCount != 0)
          throw new SQLException("query returns results");
      rs.close();
      db.reset(pointer);
      return db.executeUpdate(this, batch);
   }

   /**
    * @see org.sqlite.Stmt#executeBatch()
    */
   @Override
   public int[] executeBatch() throws SQLException {
       if (batchPos == 0)
           return new int[] {};
       try {
           return db.executeBatch(pointer, batchPos / paramCount, batch);
       }
       finally {
           clearBatch();
       }
   }

   /**
    * @see org.sqlite.Stmt#getUpdateCount()
    */
   @Override
   public int getUpdateCount() throws SQLException {
       checkOpen();
       if (pointer == 0 || resultsWaiting)
           return -1;
       return db.changes();
   }

   /**
    * @see java.sql.PreparedStatement#addBatch()
    */
   public void addBatch() throws SQLException {
      checkOpen();
      batchPos += paramCount;
      if (batchPos + paramCount > batch.length) {
          Object[] nb = new Object[batch.length * 2];
          System.arraycopy(batch, 0, nb, 0, batch.length);
          batch = nb;
      }
      System.arraycopy(batch, batchPos - paramCount, batch, batchPos, paramCount);
   }

   // ParameterMetaData FUNCTIONS //////////////////////////////////

   /**
    * @see java.sql.PreparedStatement#getParameterMetaData()
    */
   public ParameterMetaData getParameterMetaData() {
       return this;
   }

   /**
    * @see java.sql.ParameterMetaData#getParameterCount()
    */
   public int getParameterCount() throws SQLException {
       checkOpen();
       return paramCount;
   }

   /**
    * @see java.sql.ParameterMetaData#getParameterClassName(int)
    */
   public String getParameterClassName(int param) throws SQLException {
       checkOpen();
       return "java.lang.String";
   }

   /**
    * @see java.sql.ParameterMetaData#getParameterTypeName(int)
    */
   public String getParameterTypeName(int pos) {
       return "VARCHAR";
   }

   /**
    * @see java.sql.ParameterMetaData#getParameterType(int)
    */
   public int getParameterType(int pos) {
       return Types.VARCHAR;
   }

   /**
    * @see java.sql.ParameterMetaData#getParameterMode(int)
    */
   public int getParameterMode(int pos) {
       return parameterModeIn;
   }

   /**
    * @see java.sql.ParameterMetaData#getPrecision(int)
    */
   public int getPrecision(int pos) {
       return 0;
   }

   /**
    * @see java.sql.ParameterMetaData#getScale(int)
    */
   public int getScale(int pos) {
       return 0;
   }

   /**
    * @see java.sql.ParameterMetaData#isNullable(int)
    */
   public int isNullable(int pos) {
       return parameterNullable;
   }

   /**
    * @see java.sql.ParameterMetaData#isSigned(int)
    */
   public boolean isSigned(int pos) {
       return true;
   }

   /**
    * @return
    */
   public Statement getStatement() {
       return this;
   }

   // PARAMETER FUNCTIONS //////////////////////////////////////////

   /**
    * Assigns the Object value to the element at the specific position of Array batch.
    * @param pos
    * @param value
    * @throws SQLException
    */
   private void batch(int pos, Object value) throws SQLException {
      checkOpen();
      if (batch == null)
          batch = new Object[paramCount];
      batch[batchPos + pos - 1] = value;
   }

   /**
    * @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)
    */
   public void setBigDecimal(int pos, BigDecimal value) throws SQLException {
      batch(pos, value == null ? null : value.toString());
}

   /**
    * @see java.sql.PreparedStatement#setBoolean(int, boolean)
    */
   public void setBoolean(int pos, boolean value) throws SQLException {
      setInt(pos, value ? 1 : 0);
   }

   /**
    * @see java.sql.PreparedStatement#setByte(int, byte)
    */
   public void setByte(int pos, byte value) throws SQLException {
       setInt(pos, value);
   }

   /**
    * @see java.sql.PreparedStatement#setBytes(int, byte[])
    */
   public void setBytes(int pos, byte[] value) throws SQLException {
       batch(pos, value);
   }

   /**
    * @see java.sql.PreparedStatement#setDouble(int, double)
    */
   public void setDouble(int pos, double value) throws SQLException {
       batch(pos, new Double(value));
   }

   /**
    * @see java.sql.PreparedStatement#setFloat(int, float)
    */
   public void setFloat(int pos, float value) throws SQLException {
       batch(pos, new Float(value));
   }

   /**
    * @see java.sql.PreparedStatement#setInt(int, int)
    */
   public void setInt(int pos, int value) throws SQLException {
       batch(pos, new Integer(value));
   }

   /**
    * @see java.sql.PreparedStatement#setLong(int, long)
    */
   public void setLong(int pos, long value) throws SQLException {
       batch(pos, new Long(value));
   }

   /**
    * @see java.sql.PreparedStatement#setNull(int, int)
    */
   public void setNull(int pos, int u1) throws SQLException {
       setNull(pos, u1, null);
   }

   /**
    * @see java.sql.PreparedStatement#setNull(int, int, java.lang.String)
    */
   public void setNull(int pos, int u1, String u2) throws SQLException {
       batch(pos, null);
   }

   /**
    * @see java.sql.PreparedStatement#setObject(int, java.lang.Object)
    */
   public void setObject(int pos, Object value) throws SQLException {
      if (value == null)
          batch(pos, null);
      else if (value instanceof java.util.Date)
          batch(pos, new Long(((java.util.Date) value).getTime()));
      else if (value instanceof Date)
          batch(pos, new Long(((Date) value).getTime()));
      else if (value instanceof Time)
          batch(pos, new Long(((Time) value).getTime()));
      else if (value instanceof Timestamp)
          batch(pos, new Long(((Timestamp) value).getTime()));
      else if (value instanceof Long)
          batch(pos, value);
      else if (value instanceof Integer)
          batch(pos, value);
      else if (value instanceof Short)
          batch(pos, new Integer(((Short) value).intValue()));
      else if (value instanceof Float)
          batch(pos, value);
      else if (value instanceof Double)
          batch(pos, value);
      else if (value instanceof Boolean)
          setBoolean(pos, ((Boolean) value).booleanValue());
      else if (value instanceof byte[])
          batch(pos, value);
	else if (value instanceof BigDecimal)
		setBigDecimal(pos, (BigDecimal)value);
       else
           batch(pos, value.toString());
   }

   /**
    * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int)
    */
   public void setObject(int p, Object v, int t) throws SQLException {
       setObject(p, v);
   }

   /**
    * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int)
    */
   public void setObject(int p, Object v, int t, int s) throws SQLException {
       setObject(p, v);
   }

   /**
    * @see java.sql.PreparedStatement#setShort(int, short)
    */
   public void setShort(int pos, short value) throws SQLException {
       setInt(pos, value);
   }

   /**
    * @see java.sql.PreparedStatement#setString(int, java.lang.String)
    */
   public void setString(int pos, String value) throws SQLException {
       batch(pos, value);
   }

   /**
    * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)
    */
   public void setCharacterStream(int pos, Reader reader, int length) throws SQLException {
      try {
         // copy chars from reader to StringBuffer
         StringBuffer sb = new StringBuffer();
         char[] cbuf = new char[8192];
         int cnt;

         while ((cnt = reader.read(cbuf)) > 0) {
             sb.append(cbuf, 0, cnt);
         }

         // set as string
         setString(pos, sb.toString());
      }
      catch (IOException e) {
          throw new SQLException("Cannot read from character stream, exception message: " + e.getMessage());
      }
   }

   /**
    * @see java.sql.PreparedStatement#setDate(int, java.sql.Date)
    */
   public void setDate(int pos, Date x) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar)
    */
   public void setDate(int pos, Date x, Calendar cal) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#setTime(int, java.sql.Time)
    */
   public void setTime(int pos, Time x) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)
    */
   public void setTime(int pos, Time x, Calendar cal) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)
    */
   public void setTimestamp(int pos, Timestamp x) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)
    */
   public void setTimestamp(int pos, Timestamp x, Calendar cal) throws SQLException {
       setObject(pos, x);
   }

   /**
    * @see java.sql.PreparedStatement#getMetaData()
    */
   public ResultSetMetaData getMetaData() throws SQLException {
       checkOpen();
       return rs;
   }

   // UNUSED ///////////////////////////////////////////////////////

   /**
    * @see org.sqlite.Stmt#execute(java.lang.String)
    */
   @Override
   public boolean execute(String sql) throws SQLException {
       throw unused();
   }

   /**
    * @see org.sqlite.Stmt#executeUpdate(java.lang.String)
    */
   @Override
   public int executeUpdate(String sql) throws SQLException {
       throw unused();
   }

   /**
    * @see org.sqlite.Stmt#executeQuery(java.lang.String)
    */
   @Override
   public ResultSet executeQuery(String sql) throws SQLException {
       throw unused();
   }

   /**
    * @see org.sqlite.Stmt#addBatch(java.lang.String)
    */
   @Override
   public void addBatch(String sql) throws SQLException {
       throw unused();
   }

   /**
    * Creates an SQLiteException.
    * @return An SQLiteException.
    */
   private SQLException unused() {
       return new SQLException("not supported by PreparedStatment");
   }
}
