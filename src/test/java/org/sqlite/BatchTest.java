package org.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BatchTest 
{
	private Connection connection;
	
	@Before
	public void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:");
        
        Statement stmt = null;
        try {
        	stmt = connection.createStatement();
        	stmt.executeUpdate("create table test (id integer primary key, stuff text);");
        }
        finally {
        	close(stmt);
        }
	}
	
	@After
	public void tearDown() throws Exception {
		connection.close();
	}
	
	@Test
	public void clearParametersShouldNotDiscardBatch() throws Exception {
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("insert into test(id, stuff) values (?, ?)");
			
			for (int i = 0; i< 2; i++) {
				stmt.clearParameters();
				
				stmt.setInt(1, i);
				stmt.setString(2, "test" + i);
				stmt.addBatch();
			}
			stmt.executeBatch();
			
			assertRowCount();
		}
		finally {
			close(stmt);
		}
	}

	private void assertRowCount() throws Exception {
		Statement select = null;
		ResultSet results = null;
		try {
			select = connection.createStatement();
			select.execute("select count(*) from test");
			results = select.getResultSet();
			results.next();
			int rowCount = results.getInt(1);
			Assert.assertEquals(2, rowCount);
		}
		finally {
			close(results);
			close(select);
		}
	}
	
	private void close(ResultSet results) throws SQLException {
    	if (results != null) {
    		results.close();
    	}
	}

	private void close(Statement stmt) throws SQLException {
		if (stmt != null) {
			stmt.close();
		}
	}
}
