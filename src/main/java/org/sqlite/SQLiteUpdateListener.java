package org.sqlite;

/** https://www.sqlite.org/c3ref/update_hook.html */
public interface SQLiteUpdateListener {

    public enum Type {
        INSERT,
        DELETE,
        UPDATE
    }

    void onUpdate(Type type, String database, String table, long rowId);
}
