package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests SQLite tracing . */
public class TraceProfileTest
{
    private Connection    conn;
    private Statement     stat;

    @Before
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
    }

    @After
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void tracing() throws SQLException {

    	final List<String> t1stack = new ArrayList<String>();
    	final Trace t1 = new Trace() {
			@Override
			public void xTrace(String sql) {
				t1stack.add(sql);
			}
		};

		final List<String> t2stack = new ArrayList<String>();
    	final Trace t2 = new Trace() {
			@Override
			public void xTrace(String sql) {
				t2stack.add(sql);
			}
		};

		// set the t1 trace and capture a statement
        Trace.setTrace(conn, t1);
        stat.executeQuery("pragma journal_mode;").close();

        // execute without a trace
        Trace.clearTrace(conn);
        stat.executeQuery("pragma read_uncommitted;").close();

        // re-use the t1 trace to capture a statement
        Trace.setTrace(conn, t1);
        stat.executeQuery("pragma query_only;").close();

        // replace the t1 trace with the t2 trace
        Trace.setTrace(conn, t2);
        stat.executeQuery("pragma foreign_keys;").close();

        // replace the t2 trace with the t1 trace
        Trace.setTrace(conn, t1);
        stat.executeQuery("pragma recursive_triggers;").close();

        // replace the t1 trace with the t2 trace
        Trace.setTrace(conn, t2);
        stat.executeQuery("pragma schema_version;").close();

        Trace.clearTrace(conn);

        // validate the t1 trace stack
        assertEquals(3, t1stack.size());
        assertTrue(t1stack.contains("pragma journal_mode;"));
        assertTrue(t1stack.contains("pragma query_only;"));
        assertTrue(t1stack.contains("pragma recursive_triggers;"));
        assertFalse(t1stack.contains("pragma read_uncommitted;"));

        // validate the t2 trace stack
        assertEquals(2, t2stack.size());
        assertTrue(t2stack.contains("pragma foreign_keys;"));
        assertTrue(t2stack.contains("pragma schema_version;"));
    }

    @Test
    public void profiling() throws SQLException {

    	final Set<String> p1stack = new LinkedHashSet<String>();
    	final Profile p1 = new Profile() {
			@Override
			public void xProfile(String sql, long duration) {
				p1stack.add(sql);
			}
		};

		final Set<String> p2stack = new LinkedHashSet<String>();
    	final Profile p2 = new Profile() {
			@Override
			public void xProfile(String sql, long duration) {
				p2stack.add(sql);
			}
		};

		// set the p1 profile and capture a statement
		Profile.setProfile(conn, p1);
        stat.execute("create table mytest(id, name);");

        // execute without a profile
        Profile.clearProfile(conn);
        stat.execute("create table notprofiled(id, name);");

        // re-use the p1 profile to capture a statement
        Profile.setProfile(conn, p1);
        stat.execute("insert into mytest values(1, 'apples');");

        // replace the p1 profile with the p2 profile
        Profile.setProfile(conn, p2);
        stat.execute("insert into mytest values(2, 'oranges');");

        // replace the p2 profile with the p1 profile
        Profile.setProfile(conn, p1);
        stat.execute("insert into mytest values(3, 'bananas');");

        // replace the p1 profile with the p2 profile
        Profile.setProfile(conn, p2);
        stat.execute("insert into mytest values(4, 'pears');");

        Profile.clearProfile(conn);

        // validate the p1 profile stack
        assertEquals(5, p1stack.size());
        assertTrue(p1stack.contains("create table mytest(id, name);"));
        assertTrue(p1stack.contains("insert into mytest values(1, 'apples');"));
        assertTrue(p1stack.contains("insert into mytest values(3, 'bananas');"));
        assertFalse(p1stack.contains("create table notprofiled(id, name);"));

        // validate the p2 profile stack
        assertEquals(4, p2stack.size());
        assertTrue(p2stack.contains("insert into mytest values(2, 'oranges');"));
        assertTrue(p2stack.contains("insert into mytest values(4, 'pears');"));
    }

}
