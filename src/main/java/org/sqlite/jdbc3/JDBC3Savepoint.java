package org.sqlite.jdbc3;

import java.sql.SQLException;
import java.sql.Savepoint;

public class JDBC3Savepoint implements Savepoint {

	final int id;

	final String name;

	JDBC3Savepoint(int id) {
		this.id = id;
		this.name = null;
	}

	JDBC3Savepoint(String name) {
		this.id = 0;
		this.name = name;
	}

	public int getSavepointId() throws SQLException {
		return id;
	}

	public String getSavepointName() throws SQLException {
		return name == null ? String.format("SQLITE_SAVEPOINT_%s", id) : name;
	}
}