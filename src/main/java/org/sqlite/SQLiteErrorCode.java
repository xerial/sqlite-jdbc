/*--------------------------------------------------------------------------
 *  Copyright 2009 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// sqlite-jdbc Project
//
// SQLiteErrorCode.java
// Since: Apr 21, 2009
//
// $URL$ 
// $Author$
//--------------------------------------
package org.sqlite;

/**
 * SQLite3 error code
 * 
 * @author leo
 * @see <a href="http://www.sqlite.org/c3ref/c_abort.html">http://www.sqlite.org/c3ref/c_abort.html</a>
 * 
 */
public enum SQLiteErrorCode {

    UNKNOWN_ERROR(-1, "unknown error"),
    SQLITE_OK(0, "Successful result"),
    /* beginning-of-error-codes */
    SQLITE_ERROR(1, "SQL error or missing database"),
    SQLITE_INTERNAL(2, "Internal logic error in SQLite"),
    SQLITE_PERM(3, " Access permission denied"),
    SQLITE_ABORT(4, " Callback routine requested an abort"),
    SQLITE_BUSY(5, " The database file is locked"),
    SQLITE_LOCKED(6, " A table in the database is locked"),
    SQLITE_NOMEM(7, " A malloc() failed"),
    SQLITE_READONLY(8, " Attempt to write a readonly database"),
    SQLITE_INTERRUPT(9, " Operation terminated by sqlite3_interrupt()"),
    SQLITE_IOERR(10, " Some kind of disk I/O error occurred"),
    SQLITE_CORRUPT(11, " The database disk image is malformed"),
    SQLITE_NOTFOUND(12, " NOT USED. Table or record not found"),
    SQLITE_FULL(13, " Insertion failed because database is full"),
    SQLITE_CANTOPEN(14, " Unable to open the database file"),
    SQLITE_PROTOCOL(15, " NOT USED. Database lock protocol error"),
    SQLITE_EMPTY(16, " Database is empty"),
    SQLITE_SCHEMA(17, " The database schema changed"),
    SQLITE_TOOBIG(18, " String or BLOB exceeds size limit"),
    SQLITE_CONSTRAINT(19, " Abort due to constraint violation"),
    SQLITE_MISMATCH(20, " Data type mismatch"),
    SQLITE_MISUSE(21, " Library used incorrectly"),
    SQLITE_NOLFS(22, " Uses OS features not supported on host"),
    SQLITE_AUTH(23, " Authorization denied"),
    SQLITE_FORMAT(24, " Auxiliary database format error"),
    SQLITE_RANGE(25, " 2nd parameter to sqlite3_bind out of range"),
    SQLITE_NOTADB(26, " File opened that is not a database file"),
    SQLITE_ROW(100, " sqlite3_step() has another row ready"),
    SQLITE_DONE(101, " sqlite3_step() has finished executing");

    public final int code;
    public final String message;

    /**
     * Constructor that applies error code and message.
     * @param code Error code.
     * @param message Message for the error.
     */
    private SQLiteErrorCode(int code, String message)
    {
        this.code = code;
        this.message = message;
    }

    /**
     * @param errorCode Error code.
     * @return Error message.
     */
    public static SQLiteErrorCode getErrorCode(int errorCode)
    {
        for (SQLiteErrorCode each : SQLiteErrorCode.values())
        {
            if (errorCode == each.code)
                return each;
        }
        return UNKNOWN_ERROR;
    }

    /**
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString()
    {
        return String.format("[%s] %s", this.name(), message);
    }
}
