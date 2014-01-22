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

package org.sqlite.core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sqlite.SQLiteConnection;

public abstract class CoreDatabaseMetaData
{
    protected SQLiteConnection              conn;
    protected PreparedStatement
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
     * Used to save generating a new statement every call.
     */
    protected PreparedStatement getGeneratedKeys = null;

    /**
     * Reference count.
     */
    public int refCount = 1;

    /**
     * Constructor that applies the Connection object.
     * @param conn Connection object.
     */
    protected CoreDatabaseMetaData(SQLiteConnection conn) {
        this.conn = conn;
    }

    public abstract ResultSet getGeneratedKeys() throws SQLException;

    /**
     * @throws SQLException
     */
    protected void checkOpen() throws SQLException {
        if (conn == null) {
            throw new SQLException("connection closed");
        }
    }

    /**
     * @throws SQLException
     */
    public synchronized void close() throws SQLException {
        if (conn == null || refCount > 0) {
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
     * Adds SQL string quotes to the given string.
     * @param tableName The string to quote.
     * @return The quoted string.
     */
    protected static String quote(String tableName) {
        if (tableName == null) {
            return "null";
        }
        else {
            return String.format("'%s'", tableName);
        }
    }

    /**
     * Applies SQL escapes for special characters in a given string.
     * @param val The string to escape.
     * @return The SQL escaped string.
     */
    protected String escape(final String val) {
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
    public class PrimaryKeyFinder {
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
