package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** These tests are designed to stress Statements on memory databases. */
public class DBMetaDataTest
{
    private Connection       conn;
    private Statement        stat;
    private DatabaseMetaData meta;

    @Before
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        stat = conn.createStatement();
        stat.executeUpdate("create table test (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create view testView as select * from test;");
        meta = conn.getMetaData();
    }

    @After
    public void close() throws SQLException {
        meta = null;
        stat.close();
        conn.close();
    }

    @Test
    public void getTables() throws SQLException {
        ResultSet rs = meta.getTables(null, null, null, null);
        assertNotNull(rs);

        stat.getGeneratedKeys().close();
        stat.close();

        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test"); // 3
        assertEquals(rs.getString("TABLE_TYPE"), "TABLE"); // 4
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertEquals(rs.getString("TABLE_TYPE"), "VIEW");
        rs.close();

        rs = meta.getTables(null, null, "bob", null);
        assertFalse(rs.next());
        rs.close();
        rs = meta.getTables(null, null, "test", null);
        assertTrue(rs.next());
        assertFalse(rs.next());
        rs.close();
        rs = meta.getTables(null, null, "test%", null);
        assertTrue(rs.next());
        assertTrue(rs.next());
        rs.close();

        rs = meta.getTables(null, null, null, new String[] { "table" });
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertFalse(rs.next());
        rs.close();

        rs = meta.getTables(null, null, null, new String[] { "view" });
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void getTableTypes() throws SQLException {
        ResultSet rs = meta.getTableTypes();
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_TYPE"), "GLOBAL TEMPORARY");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_TYPE"), "SYSTEM TABLE");        
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_TYPE"), "TABLE");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_TYPE"), "VIEW");
        assertFalse(rs.next());
    }

    @Test
    public void getTypeInfo() throws SQLException {
        ResultSet rs = meta.getTypeInfo();
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(rs.getString("TYPE_NAME"), "BLOB");
        assertTrue(rs.next());
        assertEquals(rs.getString("TYPE_NAME"), "INTEGER");
        assertTrue(rs.next());
        assertEquals(rs.getString("TYPE_NAME"), "NULL");
        assertTrue(rs.next());
        assertEquals(rs.getString("TYPE_NAME"), "REAL");
        assertTrue(rs.next());
        assertEquals(rs.getString("TYPE_NAME"), "TEXT");
        assertFalse(rs.next());
    }

    @Test
    public void getColumns() throws SQLException {
        ResultSet rs = meta.getColumns(null, null, "test", "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertEquals(rs.getString("IS_NULLABLE"), "YES");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getInt("DATA_TYPE"), Types.INTEGER);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "test", "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertEquals(rs.getInt("DATA_TYPE"), Types.FLOAT);
        assertEquals(rs.getString("IS_NULLABLE"), "YES");
        assertEquals(rs.getString("COLUMN_DEF"), "0.0");
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "test", "sn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        assertEquals(rs.getString("IS_NULLABLE"), "NO");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "test", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "test", "%n");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "test%", "%");
        // TABLE "test"
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        // VIEW "testView"
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "%", "%");
        // TABLE "test"
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "test");
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        // VIEW "testView"
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "testView");
        assertEquals(rs.getString("COLUMN_NAME"), "id");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "fn");
        assertTrue(rs.next());
        assertEquals(rs.getString("COLUMN_NAME"), "sn");
        assertFalse(rs.next());

        rs = meta.getColumns(null, null, "doesnotexist", "%");
        assertFalse(rs.next());
        assertEquals(24, rs.getMetaData().getColumnCount());
    }

    @Test
    public void numberOfgetImportedKeysCols() throws SQLException {

        stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
        stat.executeUpdate("create table child1 (id1 integer, id2 integer, foreign key(id1) references parent(id1), foreign key(id2) references parent(id2))");
        stat.executeUpdate("create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

        ResultSet importedKeys = meta.getImportedKeys(null, null, "child1");

        //child1: 1st fk (simple)
        assertTrue(importedKeys.next());
        assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("id2", importedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(importedKeys.getString("PK_NAME"));
        assertNotNull(importedKeys.getString("FK_NAME"));
        assertEquals("child1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("id2", importedKeys.getString("FKCOLUMN_NAME"));

        //child1: 2nd fk (simple)
        assertTrue(importedKeys.next());
        assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("id1", importedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(importedKeys.getString("PK_NAME"));
        assertNotNull(importedKeys.getString("FK_NAME"));
        assertEquals("child1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("id1", importedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(importedKeys.next());

        importedKeys = meta.getImportedKeys(null, null, "child2");

        //child2: 1st fk (composite)
        assertTrue(importedKeys.next());
        assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("id2", importedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(importedKeys.getString("PK_NAME"));
        assertNotNull(importedKeys.getString("FK_NAME"));
        assertEquals("child2", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("id2", importedKeys.getString("FKCOLUMN_NAME"));

        assertTrue(importedKeys.next());
        assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("id1", importedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(importedKeys.getString("PK_NAME"));
        assertNotNull(importedKeys.getString("FK_NAME"));
        assertEquals("child2", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("id1", importedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(importedKeys.next());

        importedKeys.close();
    }

    @Test
    public void numberOfgetExportedKeysCols() throws SQLException {

        stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
        stat.executeUpdate("create table child1 (id1 integer, id2 integer,\r\n foreign\tkey(id1) references parent(id1), foreign key(id2) references parent(id2))");
        stat.executeUpdate("create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

        ResultSet exportedKeys = meta.getExportedKeys(null, null, "parent");

        //1st fk (simple) - child1
        assertTrue(exportedKeys.next());
        assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("id2", exportedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(exportedKeys.getString("PK_NAME"));
        assertNotNull(exportedKeys.getString("FK_NAME"));
        assertEquals("child1", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("id2", exportedKeys.getString("FKCOLUMN_NAME"));

        //2nd fk (simple) - child1
        assertTrue(exportedKeys.next());
        assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("id1", exportedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(exportedKeys.getString("PK_NAME"));
        assertNotNull(exportedKeys.getString("FK_NAME"));
        assertEquals("child1", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("id1", exportedKeys.getString("FKCOLUMN_NAME"));

        //3rd fk (composite) - child2
        assertTrue(exportedKeys.next());
        assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("id2", exportedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(exportedKeys.getString("PK_NAME"));
        assertNotNull(exportedKeys.getString("FK_NAME"));
        assertEquals("child2", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("id2", exportedKeys.getString("FKCOLUMN_NAME"));

        assertTrue(exportedKeys.next());
        assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("id1", exportedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(exportedKeys.getString("PK_NAME"));
        assertNotNull(exportedKeys.getString("FK_NAME"));
        assertEquals("child2", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("id1", exportedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(exportedKeys.next());

        exportedKeys.close();
    }

    @Test
    public void getExportedKeysColsForNamedKeys() throws SQLException {

    	ResultSet exportedKeys;

    	// 1. Check for named primary keys
    	// SQL is deliberately in uppercase, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate("CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");

		exportedKeys = meta.getExportedKeys(null, null, "PARENT1");

        assertTrue(exportedKeys.next());
        assertEquals("PARENT1", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID1", exportedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT", exportedKeys.getString("PK_NAME"));
        assertEquals("", exportedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID1", exportedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(exportedKeys.next());

        exportedKeys.close();
        
    	// 2. Check for named foreign keys
    	// SQL is deliberately in mixed case, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE Parent2 (Id1 INTEGER, DATA1 INTEGER, PRIMARY KEY (Id1))");
        stat.executeUpdate("CREATE TABLE Child2 (Id1 INTEGER, DATA2 INTEGER, CONSTRAINT FK_Child2 FOREIGN KEY(Id1) REFERENCES Parent2(Id1))");

		exportedKeys = meta.getExportedKeys(null, null, "Parent2");

        assertTrue(exportedKeys.next());
        assertEquals("Parent2", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("Id1", exportedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("", exportedKeys.getString("PK_NAME"));
        assertEquals("FK_Child2", exportedKeys.getString("FK_NAME"));
        assertEquals("Child2", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("Id1", exportedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(exportedKeys.next());

        exportedKeys.close();
    }

    @Test
    public void getImportedKeysColsForNamedKeys() throws SQLException {

    	ResultSet importedKeys;

    	// 1. Check for named primary keys
    	// SQL is deliberately in uppercase, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate("CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");

		importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertTrue(importedKeys.next());
        assertEquals("PARENT1", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID1", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT", importedKeys.getString("PK_NAME"));
        assertEquals("", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID1", importedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(importedKeys.next());

        importedKeys.close();
        
    	// 2. Check for named foreign keys
    	// SQL is deliberately in mixed case, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE Parent2 (Id1 INTEGER, DATA1 INTEGER, PRIMARY KEY (Id1))");
        stat.executeUpdate("CREATE TABLE Child2 (Id1 INTEGER, DATA2 INTEGER, "
        		+ "CONSTRAINT FK_Child2 FOREIGN KEY(Id1) REFERENCES Parent2(Id1))");

		importedKeys = meta.getImportedKeys(null, null, "Child2");

        assertTrue(importedKeys.next());
        assertEquals("Parent2", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("Id1", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("", importedKeys.getString("PK_NAME"));
        assertEquals("FK_Child2", importedKeys.getString("FK_NAME"));
        assertEquals("Child2", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("Id1", importedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(importedKeys.next());

        importedKeys.close();
    }
    
    @Test
    public void getImportedKeysColsForMixedCaseDefinition() throws SQLException {

    	ResultSet importedKeys;

    	// SQL is deliberately in mixed-case, to make sure case-sensitivity is maintained
        stat.executeUpdate("CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT PRIMARY KEY (ID1))");
        stat.executeUpdate("CREATE TABLE CHILD1 (ID1 INTEGER, DATA2 INTEGER, "
        		+ "CONSTRAINT FK_Parent1 FOREIGN KEY(ID1) REFERENCES Parent1(Id1))");

		importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertTrue(importedKeys.next());
        assertEquals("Parent1", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("Id1", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT", importedKeys.getString("PK_NAME"));
        assertEquals("FK_Parent1", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID1", importedKeys.getString("FKCOLUMN_NAME"));

        assertFalse(importedKeys.next());

        importedKeys.close();    
    }
    
    @Test
    public void getImportedKeysColsForMultipleImports() throws SQLException {

    	ResultSet importedKeys;

    	stat.executeUpdate("CREATE TABLE PARENT1 (ID1 INTEGER, DATA1 INTEGER, CONSTRAINT PK_PARENT1 PRIMARY KEY (ID1))");
    	stat.executeUpdate("CREATE TABLE PARENT2 (ID2 INTEGER, DATA2 INTEGER, CONSTRAINT PK_PARENT2 PRIMARY KEY (ID2))");
    	stat.executeUpdate("CREATE TABLE CHILD1 (ID1 INTEGER, ID2 INTEGER, "
    			+ "CONSTRAINT FK_PARENT1 FOREIGN KEY(ID1) REFERENCES PARENT1(ID1), "
    			+ "CONSTRAINT FK_PARENT2 FOREIGN KEY(ID2) REFERENCES PARENT2(ID2))");

		importedKeys = meta.getImportedKeys(null, null, "CHILD1");

        assertTrue(importedKeys.next());
        assertEquals("PARENT1", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID1", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT1", importedKeys.getString("PK_NAME"));
        assertEquals("FK_PARENT1", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID1", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertTrue(importedKeys.next());
        assertEquals("PARENT2", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID2", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT2", importedKeys.getString("PK_NAME"));
        assertEquals("FK_PARENT2", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID2", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertFalse(importedKeys.next());

        importedKeys.close();    
        
        // Unnamed foreign keys and unnamed primary keys
    	stat.executeUpdate("CREATE TABLE PARENT3 (ID3 INTEGER, DATA3 INTEGER, PRIMARY KEY (ID3))");
    	stat.executeUpdate("CREATE TABLE PARENT4 (ID4 INTEGER, DATA4 INTEGER, CONSTRAINT PK_PARENT4 PRIMARY KEY (ID4))");
    	stat.executeUpdate("CREATE TABLE CHILD2 (ID3 INTEGER, ID4 INTEGER, "
    			+ "FOREIGN KEY(ID3) REFERENCES PARENT3(ID3), "
    			+ "CONSTRAINT FK_PARENT4 FOREIGN KEY(ID4) REFERENCES PARENT4(ID4))");

		importedKeys = meta.getImportedKeys(null, null, "CHILD2");

        assertTrue(importedKeys.next());
        assertEquals("PARENT3", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID3", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("", importedKeys.getString("PK_NAME"));
        assertEquals("", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD2", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID3", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertTrue(importedKeys.next());
        assertEquals("PARENT4", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID4", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT4", importedKeys.getString("PK_NAME"));
        assertEquals("FK_PARENT4", importedKeys.getString("FK_NAME"));
        assertEquals("CHILD2", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID4", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertFalse(importedKeys.next());

        importedKeys.close();   
    }
    
    @Test
    public void getImportedKeysCols2() throws SQLException {

    	stat.executeUpdate("CREATE TABLE Authors (Id INTEGER NOT NULL, Name VARCHAR(20) NOT NULL, "
    			+ "CONSTRAINT PK_Authors PRIMARY KEY (Id)," + 
    			"  CONSTRAINT CHECK_UPPERCASE_Name CHECK (Name=UPPER(Name)))");
    	stat.executeUpdate("CREATE TABLE Books (Id INTEGER NOT NULL, Title VARCHAR(255) NOT NULL, PreviousEditionId INTEGER,"
    			+ "CONSTRAINT PK_Books PRIMARY KEY (Id), "
    			+ "CONSTRAINT FK_PreviousEdition FOREIGN KEY(PreviousEditionId) REFERENCES Books (Id))");
    	stat.executeUpdate("CREATE TABLE BookAuthors (BookId INTEGER NOT NULL, AuthorId INTEGER NOT NULL, "
    			+ "CONSTRAINT FK_Y_Book FOREIGN KEY (BookId) REFERENCES Books (Id), "
    			+ "CONSTRAINT FK_Z_Author FOREIGN KEY (AuthorId) REFERENCES Authors (Id)) ");

    	ResultSet importedKeys;
    	
		importedKeys = meta.getImportedKeys(null, null, "BookAuthors");

        assertTrue(importedKeys.next());
        assertEquals("Authors", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("Id", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_Authors", importedKeys.getString("PK_NAME"));
        assertEquals("FK_Z_Author", importedKeys.getString("FK_NAME"));
        assertEquals("BookAuthors", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("AuthorId", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertTrue(importedKeys.next());
        assertEquals("Books", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("Id", importedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_Books", importedKeys.getString("PK_NAME"));
        assertEquals("FK_Y_Book", importedKeys.getString("FK_NAME"));
        assertEquals("BookAuthors", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("BookId", importedKeys.getString("FKCOLUMN_NAME"));
        
        assertFalse(importedKeys.next());

        importedKeys.close();  
	

        ResultSet exportedKeys;

		exportedKeys = meta.getExportedKeys(null, null, "Authors");

		assertTrue(exportedKeys.next());
		assertEquals("Authors", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("Id", exportedKeys.getString("PKCOLUMN_NAME"));
		assertEquals("PK_Authors", exportedKeys.getString("PK_NAME"));
		assertEquals("FK_Z_Author", exportedKeys.getString("FK_NAME"));
		assertEquals("BookAuthors", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("AuthorId", exportedKeys.getString("FKCOLUMN_NAME"));

		assertFalse(exportedKeys.next());

		exportedKeys.close(); 

		exportedKeys = meta.getExportedKeys(null, null, "Books");

		assertTrue(exportedKeys.next());
		assertEquals("Books", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("Id", exportedKeys.getString("PKCOLUMN_NAME"));
		assertEquals("PK_Books", exportedKeys.getString("PK_NAME"));
		assertEquals("FK_Y_Book", exportedKeys.getString("FK_NAME"));
		assertEquals("BookAuthors", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("BookId", exportedKeys.getString("FKCOLUMN_NAME"));
		
		assertTrue(exportedKeys.next());
		assertEquals("Books", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("Id", exportedKeys.getString("PKCOLUMN_NAME"));
		assertEquals("PK_Books", exportedKeys.getString("PK_NAME"));
		assertEquals("FK_PreviousEdition", exportedKeys.getString("FK_NAME")); // ???
		assertEquals("Books", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("PreviousEditionId", exportedKeys.getString("FKCOLUMN_NAME"));
		
		assertFalse(exportedKeys.next());

		exportedKeys.close();  
    }
    
    @Test
    public void getExportedKeysColsForMultipleImports() throws SQLException {

    	ResultSet exportedKeys;

    	stat.executeUpdate("CREATE TABLE PARENT1 (ID1 INTEGER, ID2 INTEGER, CONSTRAINT PK_PARENT1 PRIMARY KEY (ID1))");
    	stat.executeUpdate("CREATE TABLE CHILD1 (ID1 INTEGER, CONSTRAINT FK_PARENT1 FOREIGN KEY(ID1) REFERENCES PARENT1(ID1))");
    	stat.executeUpdate("CREATE TABLE CHILD2 (ID2 INTEGER, CONSTRAINT FK_PARENT2 FOREIGN KEY(ID2) REFERENCES PARENT1(ID2))");

		exportedKeys = meta.getExportedKeys(null, null, "PARENT1");

        assertTrue(exportedKeys.next());
        assertEquals("PARENT1", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID1", exportedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("PK_PARENT1", exportedKeys.getString("PK_NAME"));
        assertEquals("FK_PARENT1", exportedKeys.getString("FK_NAME"));
        assertEquals("CHILD1", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID1", exportedKeys.getString("FKCOLUMN_NAME"));
        
        assertTrue(exportedKeys.next());
        assertEquals("PARENT1", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("ID2", exportedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("", exportedKeys.getString("PK_NAME"));
        assertEquals("FK_PARENT2", exportedKeys.getString("FK_NAME"));
        assertEquals("CHILD2", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("ID2", exportedKeys.getString("FKCOLUMN_NAME"));
        
        assertFalse(exportedKeys.next());

        exportedKeys.close();    
    }
    
    @Test
    public void columnOrderOfgetTables() throws SQLException {
    	
    	stat.executeUpdate("CREATE TABLE TABLE1 (ID1 INTEGER PRIMARY KEY AUTOINCREMENT, ID2 INTEGER)");
    	stat.executeUpdate("CREATE TABLE TABLE2 (ID2 INTEGER, DATA2 VARCHAR(20))");
    	stat.executeUpdate("CREATE TEMP TABLE TABLE3 (ID3 INTEGER, DATA3 VARCHAR(20))");
    	stat.executeUpdate("CREATE VIEW VIEW1 (V1, V2) AS SELECT ID1, ID2 FROM TABLE1");
    	
        ResultSet rsTables = meta.getTables(null, null, null, new String[] {"TABLE", "VIEW", "GLOBAL TEMPORARY", "SYSTEM TABLE"});
        
        assertTrue(rsTables.next());
        
        // Check order of columns
        ResultSetMetaData rsmeta = rsTables.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 10);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TABLE_NAME");
        assertEquals(rsmeta.getColumnName(4), "TABLE_TYPE");
        assertEquals(rsmeta.getColumnName(5), "REMARKS");
        assertEquals(rsmeta.getColumnName(6), "TYPE_CAT");
        assertEquals(rsmeta.getColumnName(7), "TYPE_SCHEM");
        assertEquals(rsmeta.getColumnName(8), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(9), "SELF_REFERENCING_COL_NAME");
        assertEquals(rsmeta.getColumnName(10), "REF_GENERATION");
        
        assertEquals("TABLE3", rsTables.getString("TABLE_NAME"));
        assertEquals("GLOBAL TEMPORARY", rsTables.getString("TABLE_TYPE"));
        
        assertTrue(rsTables.next());
        assertEquals("sqlite_sequence", rsTables.getString("TABLE_NAME"));
        assertEquals("SYSTEM TABLE", rsTables.getString("TABLE_TYPE"));
        
        assertTrue(rsTables.next());
        assertEquals("TABLE1", rsTables.getString("TABLE_NAME"));
        assertEquals("TABLE", rsTables.getString("TABLE_TYPE"));

        assertTrue(rsTables.next());
        assertEquals("TABLE2", rsTables.getString("TABLE_NAME"));
        assertEquals("TABLE", rsTables.getString("TABLE_TYPE"));

        assertTrue(rsTables.next());
        assertTrue(rsTables.next());
        assertEquals("VIEW1", rsTables.getString("TABLE_NAME"));
        assertEquals("VIEW", rsTables.getString("TABLE_TYPE"));
                
        rsTables.close();    
    }

    @Test
    public void columnOrderOfgetTableTypes() throws SQLException {
        ResultSet rs = meta.getTableTypes();
        assertTrue(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 1);
        assertEquals(rsmeta.getColumnName(1), "TABLE_TYPE");
    }

    @Test
    public void columnOrderOfgetTypeInfo() throws SQLException {
        ResultSet rs = meta.getTypeInfo();
        assertTrue(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 18);
        assertEquals(rsmeta.getColumnName(1), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(2), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(3), "PRECISION");
        assertEquals(rsmeta.getColumnName(4), "LITERAL_PREFIX");
        assertEquals(rsmeta.getColumnName(5), "LITERAL_SUFFIX");
        assertEquals(rsmeta.getColumnName(6), "CREATE_PARAMS");
        assertEquals(rsmeta.getColumnName(7), "NULLABLE");
        assertEquals(rsmeta.getColumnName(8), "CASE_SENSITIVE");
        assertEquals(rsmeta.getColumnName(9), "SEARCHABLE");
        assertEquals(rsmeta.getColumnName(10), "UNSIGNED_ATTRIBUTE");
        assertEquals(rsmeta.getColumnName(11), "FIXED_PREC_SCALE");
        assertEquals(rsmeta.getColumnName(12), "AUTO_INCREMENT");
        assertEquals(rsmeta.getColumnName(13), "LOCAL_TYPE_NAME");
        assertEquals(rsmeta.getColumnName(14), "MINIMUM_SCALE");
        assertEquals(rsmeta.getColumnName(15), "MAXIMUM_SCALE");
        assertEquals(rsmeta.getColumnName(16), "SQL_DATA_TYPE");
        assertEquals(rsmeta.getColumnName(17), "SQL_DATETIME_SUB");
        assertEquals(rsmeta.getColumnName(18), "NUM_PREC_RADIX");
    }

    @Test
    public void columnOrderOfgetColumns() throws SQLException {
        ResultSet rs = meta.getColumns(null, null, "test", null);
        assertTrue(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 24);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TABLE_NAME");
        assertEquals(rsmeta.getColumnName(4), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(5), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(6), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(7), "COLUMN_SIZE");
        assertEquals(rsmeta.getColumnName(8), "BUFFER_LENGTH");
        assertEquals(rsmeta.getColumnName(9), "DECIMAL_DIGITS");
        assertEquals(rsmeta.getColumnName(10), "NUM_PREC_RADIX");
        assertEquals(rsmeta.getColumnName(11), "NULLABLE");
        assertEquals(rsmeta.getColumnName(12), "REMARKS");
        assertEquals(rsmeta.getColumnName(13), "COLUMN_DEF");
        assertEquals(rsmeta.getColumnName(14), "SQL_DATA_TYPE");
        assertEquals(rsmeta.getColumnName(15), "SQL_DATETIME_SUB");
        assertEquals(rsmeta.getColumnName(16), "CHAR_OCTET_LENGTH");
        assertEquals(rsmeta.getColumnName(17), "ORDINAL_POSITION");
        assertEquals(rsmeta.getColumnName(18), "IS_NULLABLE");
        // should be SCOPE_CATALOG, but misspelt in the standard
        assertEquals(rsmeta.getColumnName(19), "SCOPE_CATLOG");
        assertEquals(rsmeta.getColumnName(20), "SCOPE_SCHEMA");
        assertEquals(rsmeta.getColumnName(21), "SCOPE_TABLE");
        assertEquals(rsmeta.getColumnName(22), "SOURCE_DATA_TYPE");
        assertEquals(rsmeta.getColumnName(23), "IS_AUTOINCREMENT");
        assertEquals(rsmeta.getColumnName(24), "IS_GENERATEDCOLUMN");
        assertEquals(rs.getString("COLUMN_NAME").toUpperCase(), "ID");
        assertEquals(rs.getInt("ORDINAL_POSITION"), 1);
    }

    // the following functions always return an empty resultset, so
    // do not bother testing their parameters, only the column types

    @Test
    public void columnOrderOfgetProcedures() throws SQLException {
        ResultSet rs = meta.getProcedures(null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 8);
        assertEquals(rsmeta.getColumnName(1), "PROCEDURE_CAT");
        assertEquals(rsmeta.getColumnName(2), "PROCEDURE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "PROCEDURE_NAME");
        // currently (Java 1.5), cols 4,5,6 are undefined
        assertEquals(rsmeta.getColumnName(7), "REMARKS");
        assertEquals(rsmeta.getColumnName(8), "PROCEDURE_TYPE");
    }

    @Test
    public void columnOrderOfgetProcedurColumns() throws SQLException {
        ResultSet rs = meta.getProcedureColumns(null, null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 13);
        assertEquals(rsmeta.getColumnName(1), "PROCEDURE_CAT");
        assertEquals(rsmeta.getColumnName(2), "PROCEDURE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "PROCEDURE_NAME");
        assertEquals(rsmeta.getColumnName(4), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(5), "COLUMN_TYPE");
        assertEquals(rsmeta.getColumnName(6), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(7), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(8), "PRECISION");
        assertEquals(rsmeta.getColumnName(9), "LENGTH");
        assertEquals(rsmeta.getColumnName(10), "SCALE");
        assertEquals(rsmeta.getColumnName(11), "RADIX");
        assertEquals(rsmeta.getColumnName(12), "NULLABLE");
        assertEquals(rsmeta.getColumnName(13), "REMARKS");
    }

    @Test
    public void columnOrderOfgetSchemas() throws SQLException {
        ResultSet rs = meta.getSchemas();
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 2);
        assertEquals(rsmeta.getColumnName(1), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(2), "TABLE_CATALOG");
    }

    @Test
    public void columnOrderOfgetCatalogs() throws SQLException {
        ResultSet rs = meta.getCatalogs();
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 1);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
    }

    @Test
    public void columnOrderOfgetColumnPrivileges() throws SQLException {
        ResultSet rs = meta.getColumnPrivileges(null, null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 8);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TABLE_NAME");
        assertEquals(rsmeta.getColumnName(4), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(5), "GRANTOR");
        assertEquals(rsmeta.getColumnName(6), "GRANTEE");
        assertEquals(rsmeta.getColumnName(7), "PRIVILEGE");
        assertEquals(rsmeta.getColumnName(8), "IS_GRANTABLE");
    }

    @Test
    public void columnOrderOfgetTablePrivileges() throws SQLException {
        ResultSet rs = meta.getTablePrivileges(null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 7);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TABLE_NAME");
        assertEquals(rsmeta.getColumnName(4), "GRANTOR");
        assertEquals(rsmeta.getColumnName(5), "GRANTEE");
        assertEquals(rsmeta.getColumnName(6), "PRIVILEGE");
        assertEquals(rsmeta.getColumnName(7), "IS_GRANTABLE");
    }

    @Test
    public void columnOrderOfgetBestRowIdentifier() throws SQLException {
        ResultSet rs = meta.getBestRowIdentifier(null, null, null, 0, false);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 8);
        assertEquals(rsmeta.getColumnName(1), "SCOPE");
        assertEquals(rsmeta.getColumnName(2), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(3), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(4), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(5), "COLUMN_SIZE");
        assertEquals(rsmeta.getColumnName(6), "BUFFER_LENGTH");
        assertEquals(rsmeta.getColumnName(7), "DECIMAL_DIGITS");
        assertEquals(rsmeta.getColumnName(8), "PSEUDO_COLUMN");
    }

    @Test
    public void columnOrderOfgetVersionColumns() throws SQLException {
        ResultSet rs = meta.getVersionColumns(null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 8);
        assertEquals(rsmeta.getColumnName(1), "SCOPE");
        assertEquals(rsmeta.getColumnName(2), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(3), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(4), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(5), "COLUMN_SIZE");
        assertEquals(rsmeta.getColumnName(6), "BUFFER_LENGTH");
        assertEquals(rsmeta.getColumnName(7), "DECIMAL_DIGITS");
        assertEquals(rsmeta.getColumnName(8), "PSEUDO_COLUMN");
    }

    @Test
    public void viewIngetPrimaryKeys() throws SQLException {
        ResultSet rs;

        stat.executeUpdate("create table t1 (c1, c2, c3);");
        stat.executeUpdate("create view view_nopk (v1, v2) as select c1, c3 from t1;");

        rs = meta.getPrimaryKeys(null, null, "view_nopk");
        assertFalse(rs.next());
    }
    
    @Test
    public void moreOfgetColumns() throws SQLException {
        ResultSet rs;

        stat.executeUpdate("create table tabcols1 (col1, col2);");
        // mixed-case table, column and primary key names
        stat.executeUpdate("CREATE TABLE TabCols2 (Col1, Col2);");
        // quoted table, column and primary key names
        stat.executeUpdate("CREATE TABLE `TabCols3` (`Col1`, `Col2`);");
        
        rs = meta.getColumns(null, null, "tabcols1", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "tabcols1");
        assertEquals(rs.getString("COLUMN_NAME"), "col1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertEquals(rs.getString("IS_NULLABLE"), "YES");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "tabcols1");
        assertEquals(rs.getString("COLUMN_NAME"), "col2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
        rs = meta.getColumns(null, null, "TabCols2", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TabCols2");
        assertEquals(rs.getString("COLUMN_NAME"), "Col1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertEquals(rs.getString("IS_NULLABLE"), "YES");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TabCols2");
        assertEquals(rs.getString("COLUMN_NAME"), "Col2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
        rs = meta.getColumns(null, null, "TabCols3", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TabCols3");
        assertEquals(rs.getString("COLUMN_NAME"), "Col1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertEquals(rs.getString("IS_NULLABLE"), "YES");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TabCols3");
        assertEquals(rs.getString("COLUMN_NAME"), "Col2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
    }

    @Test
    public void autoincrement() throws SQLException {
        ResultSet rs;

        // no autoincrement no rowid
        stat.executeUpdate("CREATE TABLE TAB1 (COL1 INTEGER NOT NULL PRIMARY KEY, COL2) WITHOUT ROWID;");
        // no autoincrement
        stat.executeUpdate("CREATE TABLE TAB2 (COL1 INTEGER NOT NULL PRIMARY KEY, COL2);");
        // autoincrement
        stat.executeUpdate("CREATE TABLE TAB3 (COL1 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, COL2);");
        
        rs = meta.getColumns(null, null, "TAB1", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB1");
        assertEquals(rs.getString("COLUMN_NAME"), "COL1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.INTEGER);
        assertEquals(rs.getString("IS_NULLABLE"), "NO");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB1");
        assertEquals(rs.getString("COLUMN_NAME"), "COL2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
        rs = meta.getColumns(null, null, "TAB2", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB2");
        assertEquals(rs.getString("COLUMN_NAME"), "COL1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.INTEGER);
        assertEquals(rs.getString("IS_NULLABLE"), "NO");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "NO");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB2");
        assertEquals(rs.getString("COLUMN_NAME"), "COL2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
        rs = meta.getColumns(null, null, "TAB3", "%");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB3");
        assertEquals(rs.getString("COLUMN_NAME"), "COL1");
        assertEquals(rs.getInt("DATA_TYPE"), Types.INTEGER);
        assertEquals(rs.getString("IS_NULLABLE"), "NO");
        assertEquals(rs.getString("COLUMN_DEF"), null);
        assertEquals(rs.getString("IS_AUTOINCREMENT"), "YES");
        assertTrue(rs.next());
        assertEquals(rs.getString("TABLE_NAME"), "TAB3");
        assertEquals(rs.getString("COLUMN_NAME"), "COL2");
        assertEquals(rs.getInt("DATA_TYPE"), Types.VARCHAR);
        assertFalse(rs.next());
        
    }

    @Test
    public void columnOrderOfgetPrimaryKeys() throws Exception {
        ResultSet rs;
        ResultSetMetaData rsmeta;

        stat.executeUpdate("create table nopk (c1, c2, c3, c4);");
        stat.executeUpdate("create table pk1 (col1 primary key, col2, col3);");
        stat.executeUpdate("create table pk2 (col1, col2 primary key, col3);");
        stat.executeUpdate("create table pk3 (col1, col2, col3, col4, primary key (col3, col2  ));");
        // extra spaces and mixed case are intentional, do not remove!
        stat.executeUpdate("create table pk4 (col1, col2, col3, col4, " +
                "\r\nCONSTraint\r\nnamed  primary\r\n\t\t key   (col3, col2  ));");
        // mixed-case table, column and primary key names - GitHub issue #219
        stat.executeUpdate("CREATE TABLE Pk5 (Col1, Col2, Col3, Col4, CONSTRAINT NamedPk PRIMARY KEY (Col3, Col2));");
        // quoted table, column and primary key names - GitHub issue #219
        stat.executeUpdate("CREATE TABLE `Pk6` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk` PRIMARY KEY (`Col3`, `Col2`));");
        // spaces before and after "primary key" - GitHub issue #236
        stat.executeUpdate("CREATE TABLE pk7 (col1, col2, col3, col4 VARCHAR(10),PRIMARY KEY (col1, col2, col3));");
        stat.executeUpdate("CREATE TABLE pk8 (col1, col2, col3, col4 VARCHAR(10), PRIMARY KEY(col1, col2, col3));");
        stat.executeUpdate("CREATE TABLE pk9 (col1, col2, col3, col4 VARCHAR(10),PRIMARY KEY(col1, col2, col3));");
        stat.executeUpdate("CREATE TABLE `Pk10` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk`PRIMARY KEY (`Col3`, `Col2`));");
        stat.executeUpdate("CREATE TABLE `Pk11` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT `NamedPk` PRIMARY KEY(`Col3`, `Col2`));");
        stat.executeUpdate("CREATE TABLE `Pk12` (`Col1`, `Col2`, `Col3`, `Col4`, CONSTRAINT`NamedPk`PRIMARY KEY(`Col3`,`Col2`));");
        stat.executeUpdate("CREATE TABLE \"Pk13\" (\"Col1\", \"Col2\", \"Col3\", \"Col4\", CONSTRAINT \"NamedPk\" PRIMARY KEY(\"Col3\",\"Col2\"));");
        
        rs = meta.getPrimaryKeys(null, null, "nopk");
        assertFalse(rs.next());
        rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 6);
        assertEquals(rsmeta.getColumnName(1), "TABLE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TABLE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TABLE_NAME");
        assertEquals(rsmeta.getColumnName(4), "COLUMN_NAME");
        assertEquals(rsmeta.getColumnName(5), "KEY_SEQ");
        assertEquals(rsmeta.getColumnName(6), "PK_NAME");
        rs.close();
        
        assertPrimaryKey(meta, "pk1", null, "col1");
        assertPrimaryKey(meta, "pk2", null, "col2");
        assertPrimaryKey(meta, "pk3", null, "col3", "col2");
        assertPrimaryKey(meta, "pk4", "named", "col3", "col2");
        assertPrimaryKey(meta, "Pk5", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk6", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "pk7", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "pk8", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "pk9", null, "col1", "col2", "col3");
        assertPrimaryKey(meta, "Pk10", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk11", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk12", "NamedPk", "Col3", "Col2");
        assertPrimaryKey(meta, "Pk13", "NamedPk", "Col3", "Col2");
    }
    
    private void assertPrimaryKey(DatabaseMetaData meta, String tableName, String pkName, String... pkColumns) throws Exception {  
    	final Map<String, Integer> colSeq = new HashMap<String, Integer>();
    	for (int i = 0; i < pkColumns.length; i++) {
    		colSeq.put(pkColumns[i], i+1);
    	}
    	Arrays.sort(pkColumns);
    	
	    final ResultSet rs = meta.getPrimaryKeys(null, null, tableName);
	    assertTrue(rs.next());
	    for (int i = 0; i < pkColumns.length; i++) {
	    	assertEquals("DatabaseMetaData.getPrimaryKeys: TABLE_CAT", null, rs.getString("TABLE_CAT"));
	    	assertEquals("DatabaseMetaData.getPrimaryKeys: TABLE_SCHEM", null, rs.getString("TABLE_SCHEM"));
		    assertEquals("DatabaseMetaData.getPrimaryKeys: TABLE_NAME", tableName, rs.getString("TABLE_NAME"));
		    assertEquals("DatabaseMetaData.getPrimaryKeys: COLUMN_NAME", pkColumns[i], rs.getString("COLUMN_NAME"));
		    assertEquals("DatabaseMetaData.getPrimaryKeys: PK_NAME", pkName, rs.getString("PK_NAME"));
		    assertEquals("DatabaseMetaData.getPrimaryKeys: KEY_SEQ", colSeq.get(pkColumns[i]).intValue(), rs.getInt("KEY_SEQ"));
		    if (i < pkColumns.length - 1) assertTrue(rs.next());
	    }
	    rs.close();
    }

    @Test
    public void columnOrderOfgetImportedKeys() throws SQLException {

        stat.executeUpdate("create table person (id integer)");
        stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet importedKeys = meta.getImportedKeys("default", "global", "address");
        assertTrue(importedKeys.next());
        assertEquals("default", importedKeys.getString("PKTABLE_CAT"));
        assertEquals("global", importedKeys.getString("PKTABLE_SCHEM"));
        assertEquals("default", importedKeys.getString("FKTABLE_CAT"));
        assertEquals("person", importedKeys.getString("PKTABLE_NAME"));
        assertEquals("id", importedKeys.getString("PKCOLUMN_NAME"));
        assertNotNull(importedKeys.getString("PK_NAME"));
        assertNotNull(importedKeys.getString("FK_NAME"));
        assertEquals("address", importedKeys.getString("FKTABLE_NAME"));
        assertEquals("pid", importedKeys.getString("FKCOLUMN_NAME"));
        importedKeys.close();

        importedKeys = meta.getImportedKeys(null, null, "person");
        assertTrue(!importedKeys.next());
        importedKeys.close();
    }

    @Test
    public void columnOrderOfgetExportedKeys() throws SQLException {

        stat.executeUpdate("create table person (id integer primary key)");
        stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet exportedKeys = meta.getExportedKeys("default", "global", "person");
        assertTrue(exportedKeys.next());
        assertEquals("default", exportedKeys.getString("PKTABLE_CAT"));
        assertEquals("global", exportedKeys.getString("PKTABLE_SCHEM"));
        assertEquals("default", exportedKeys.getString("FKTABLE_CAT"));
        assertEquals("global", exportedKeys.getString("FKTABLE_SCHEM"));
        assertNotNull(exportedKeys.getString("PK_NAME"));
        assertNotNull(exportedKeys.getString("FK_NAME"));

        assertEquals("person", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("id", exportedKeys.getString("PKCOLUMN_NAME"));
        assertEquals("address", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("pid", exportedKeys.getString("FKCOLUMN_NAME"));

        exportedKeys.close();

        exportedKeys = meta.getExportedKeys(null, null, "address");
        assertFalse(exportedKeys.next());
        exportedKeys.close();

        // With explicit primary column defined.
        stat.executeUpdate("create table REFERRED (ID integer primary key not null)");
        stat.executeUpdate("create table REFERRING (ID integer, RID integer, constraint fk\r\n foreign\tkey\r\n(RID) references REFERRED(id))");

        exportedKeys = meta.getExportedKeys(null, null, "referred");
        assertEquals("REFERRED", exportedKeys.getString("PKTABLE_NAME"));
        assertEquals("REFERRING", exportedKeys.getString("FKTABLE_NAME"));
        assertEquals("fk", exportedKeys.getString("FK_NAME"));
        exportedKeys.close();
    }

    @Test
    public void columnOrderOfgetCrossReference() throws SQLException {
        stat.executeUpdate("create table person (id integer)");
        stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

        ResultSet cr = meta.getCrossReference(null, null, "person", null, null, "address");
        //assertTrue(cr.next());

    }

    /* TODO
    
    @Test public void columnOrderOfgetTypeInfo() throws SQLException {
    @Test public void columnOrderOfgetIndexInfo() throws SQLException {
    @Test public void columnOrderOfgetSuperTypes() throws SQLException {
    @Test public void columnOrderOfgetSuperTables() throws SQLException {
    @Test public void columnOrderOfgetAttributes() throws SQLException {*/

    @Test
    public void columnOrderOfgetUDTs() throws SQLException {
        ResultSet rs = meta.getUDTs(null, null, null, null);
        assertFalse(rs.next());
        ResultSetMetaData rsmeta = rs.getMetaData();
        assertEquals(rsmeta.getColumnCount(), 7);
        assertEquals(rsmeta.getColumnName(1), "TYPE_CAT");
        assertEquals(rsmeta.getColumnName(2), "TYPE_SCHEM");
        assertEquals(rsmeta.getColumnName(3), "TYPE_NAME");
        assertEquals(rsmeta.getColumnName(4), "CLASS_NAME");
        assertEquals(rsmeta.getColumnName(5), "DATA_TYPE");
        assertEquals(rsmeta.getColumnName(6), "REMARKS");
        assertEquals(rsmeta.getColumnName(7), "BASE_TYPE");
    }

    @Test
    public void getIndexInfoOnTest() throws SQLException {
        ResultSet rs = meta.getIndexInfo(null,null,"test",false,false);

        assertNotNull(rs);
    }

    @Test
    public void getIndexInfoIndexedSingle() throws SQLException {
        stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn);");

        ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertNotNull(rs);
        assertNotNull(rsmd);
    }


    @Test
    public void getIndexInfoIndexedSingleExpr() throws SQLException {
        stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn, fn/2);");

        ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertNotNull(rs);
        assertNotNull(rsmd);
    }


    @Test
    public void getIndexInfoIndexedMulti() throws SQLException {
        stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
        stat.executeUpdate("create index testindex_idx on testindex (sn);");
        stat.executeUpdate("create index testindex_pk_idx on testindex (id);");

        ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);
        ResultSetMetaData rsmd = rs.getMetaData();

        assertNotNull(rs);
        assertNotNull(rsmd);
    }


    @Test
    public void version() throws Exception {
      File versionFile = new File("./VERSION");
      Properties version = new Properties();
      version.load(new FileReader(versionFile));
      String versionString = version.getProperty("version");
      int majorVersion = Integer.valueOf(versionString.split("\\.")[0]);
      int minorVersion = Integer.valueOf(versionString.split("\\.")[1]);
      
      assertTrue("major version check", majorVersion > 0);
      assertEquals("driver name","SQLite JDBC", meta.getDriverName());
      assertTrue("driver version", meta.getDriverVersion().startsWith(String.format("%d.%d", majorVersion, minorVersion)));
      assertEquals("driver major version", majorVersion, meta.getDriverMajorVersion());
      assertEquals("driver minor version", minorVersion, meta.getDriverMinorVersion());
      assertEquals("db name","SQLite", meta.getDatabaseProductName());
      assertEquals("db version", versionString, meta.getDatabaseProductVersion());
      assertEquals("db major version", majorVersion, meta.getDatabaseMajorVersion());
      assertEquals("db minor version", minorVersion, meta.getDatabaseMinorVersion());
      assertEquals("user name", null, meta.getUserName());
    }
    
}
