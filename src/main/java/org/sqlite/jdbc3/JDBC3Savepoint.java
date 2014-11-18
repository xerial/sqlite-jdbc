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

	JDBC3Savepoint(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getSavepointId() throws SQLException {
		return id;
	}

	public String getSavepointName() throws SQLException {
		return name == null ? String.format("SQLITE_SAVEPOINT_%s", id) : name;
	}
}