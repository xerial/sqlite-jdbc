module org.xerial.sqlitejdbc {

    requires transitive java.sql;
    requires transitive java.sql.rowset;

    exports org.sqlite;
    exports org.sqlite.core;
    exports org.sqlite.date;
    exports org.sqlite.javax;
    exports org.sqlite.jdbc3;
    exports org.sqlite.jdbc4;
    exports org.sqlite.util;

    provides java.sql.Driver with org.sqlite.JDBC;

}
