package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.core.DB;
import org.sqlite.core.NativeDBHelper;

public class AuthorizerTest {
    private Connection conn;
    private Statement stat;

    /** Denies any read or write of tables prefixed with "sys_". */
    private static final Authorizer DENY_SYS =
            new Authorizer() {
                @Override
                protected int authorize(
                        int action, String arg1, String arg2, String db, String triggerOrView) {
                    switch (action) {
                        case SQLITE_READ:
                        case SQLITE_UPDATE:
                        case SQLITE_INSERT:
                        case SQLITE_DELETE:
                            if (arg1 != null && arg1.startsWith("sys_")) return SQLITE_DENY;
                    }
                    return SQLITE_OK;
                }
            };

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate("create table sys_secret (v text)");
        stat.executeUpdate("insert into sys_secret values ('top secret')");
        stat.executeUpdate("create table pub (v text)");
        stat.executeUpdate("insert into pub values ('hello')");
        stat.executeUpdate("create view pub_view as select v from sys_secret");
        stat.executeUpdate(
                "create trigger pub_trg after insert on pub begin"
                        + " insert into sys_secret values (new.v); end");
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    private static void assertDenied(Statement stat, String sql) {
        assertThatThrownBy(() -> stat.execute(sql))
                .isInstanceOf(SQLiteException.class)
                .extracting(e -> ((SQLiteException) e).getResultCode())
                .isEqualTo(SQLiteErrorCode.SQLITE_AUTH);
    }

    @Test
    public void deniesDirectAndIndirectAccess() throws Exception {
        Authorizer.setAuthorizer(conn, DENY_SYS);

        // normal access still works
        try (ResultSet rs = stat.executeQuery("select v from pub")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("hello");
        }

        // direct
        assertDenied(stat, "select * from sys_secret");
        assertDenied(stat, "update sys_secret set v = 'x'");
        // subquery
        assertDenied(stat, "select (select v from sys_secret) from pub");
        // view
        assertDenied(stat, "select * from pub_view");
        // trigger: inserting into pub writes sys_secret
        assertDenied(stat, "insert into pub values ('leak')");
    }

    @Test
    public void reportsTriggerOrViewName() throws Exception {
        final StringBuilder seen = new StringBuilder();
        Authorizer.setAuthorizer(
                conn,
                new Authorizer() {
                    @Override
                    protected int authorize(
                            int action, String arg1, String arg2, String db, String trg) {
                        if (action == SQLITE_READ && "sys_secret".equals(arg1)) {
                            seen.append(db).append(':').append(trg);
                        }
                        return SQLITE_OK;
                    }
                });
        stat.executeQuery("select * from pub_view").close();
        assertThat(seen.toString()).startsWith("main:pub_view");
    }

    @Test
    public void ignoreReturnsNull() throws Exception {
        Authorizer.setAuthorizer(
                conn,
                new Authorizer() {
                    @Override
                    protected int authorize(int action, String a1, String a2, String d, String t) {
                        return action == SQLITE_READ && "sys_secret".equals(a1)
                                ? SQLITE_IGNORE
                                : SQLITE_OK;
                    }
                });
        try (ResultSet rs = stat.executeQuery("select v from sys_secret")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isNull();
        }
    }

    @Test
    public void exceptionDenies() throws Exception {
        Authorizer.setAuthorizer(
                conn,
                new Authorizer() {
                    @Override
                    protected int authorize(int action, String a1, String a2, String d, String t) {
                        throw new RuntimeException("boom");
                    }
                });
        assertDenied(stat, "select * from pub");
        // connection still usable after clearing
        Authorizer.clearAuthorizer(conn);
        stat.executeQuery("select * from pub").close();
    }

    @Test
    public void unicodeNamesAreDecodedCorrectly() throws Exception {
        final String table = "sys_\uD83D\uDE00_t\u00E9";
        stat.executeUpdate("create table \"" + table + "\" (v)");
        final List<String> seen = new ArrayList<>();
        Authorizer.setAuthorizer(
                conn,
                new Authorizer() {
                    @Override
                    protected int authorize(int action, String a1, String a2, String d, String t) {
                        if (action == SQLITE_READ) seen.add(a1);
                        return DENY_SYS.authorize(action, a1, a2, d, t);
                    }
                });
        assertDenied(stat, "select * from \"" + table + "\"");
        assertThat(seen).contains(table);
    }

    @Test
    public void existingPreparedStatementsAreRechecked() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("select v from sys_secret")) {
            ps.executeQuery().close();

            // installing the authorizer expires ps; it is re-prepared and denied on next execute
            Authorizer.setAuthorizer(conn, DENY_SYS);
            assertThatThrownBy(ps::executeQuery)
                    .isInstanceOf(SQLiteException.class)
                    .extracting(e -> ((SQLiteException) e).getResultCode())
                    .isEqualTo(SQLiteErrorCode.SQLITE_AUTH);
        }
        Authorizer.clearAuthorizer(conn);
        try (PreparedStatement ps = conn.prepareStatement("select v from sys_secret")) {
            ps.executeQuery().close();
        }
    }

    @Test
    public void runningStatementFinishesUnderOldPolicy() throws Exception {
        stat.executeUpdate("insert into sys_secret values ('second')");
        try (PreparedStatement ps = conn.prepareStatement("select v from sys_secret");
                ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            Authorizer.setAuthorizer(conn, DENY_SYS);
            // already running cursor is allowed to complete
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("second");
        }
    }

    @Test
    public void clearAndReset() throws Exception {
        SQLiteConnection sqliteConnection = (SQLiteConnection) conn;
        final DB database = sqliteConnection.getDatabase();

        Authorizer.setAuthorizer(conn, DENY_SYS);
        assertThat(NativeDBHelper.getAuthorizer(database)).isNotEqualTo(0);
        assertDenied(stat, "select * from sys_secret");

        Authorizer.clearAuthorizer(conn);
        assertThat(NativeDBHelper.getAuthorizer(database)).isEqualTo(0);
        stat.executeQuery("select * from sys_secret").close();
        Authorizer.clearAuthorizer(conn);

        Authorizer.setAuthorizer(conn, DENY_SYS);
        Authorizer.setAuthorizer(conn, null);
        assertThat(NativeDBHelper.getAuthorizer(database)).isEqualTo(0);
        stat.executeQuery("select * from sys_secret").close();

        Authorizer.setAuthorizer(conn, DENY_SYS);
        assertThat(NativeDBHelper.getAuthorizer(database)).isNotEqualTo(0);
        conn.close();
        assertThat(NativeDBHelper.getAuthorizer(database)).isEqualTo(0);
        assertThatThrownBy(() -> Authorizer.setAuthorizer(conn, DENY_SYS))
                .isInstanceOf(SQLException.class);
    }
}
