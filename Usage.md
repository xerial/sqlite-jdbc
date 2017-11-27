## How to Specify Database Files ##

Here is an example to establishing a connection to a database file `C:\work\mydatabase.db` (in Windows)

```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:C:/work/mydatabase.db");
```

Opening a UNIX (Linux, Mac OS X, etc.) file `/home/leo/work/mydatabase.db`
```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/leo/work/mydatabase.db");
```

## How to Use Memory Databases ##

SQLite supports on-memory database management, which does not create any database files. To use a memory database in your Java code, get the database connection as follows:


```java
Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
```

And also, you can create memory database as follows:
```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:");
```

## How to use Online Backup and Restore Feature ##

Take a backup of the whole database to `backup.db` file:


```java

// Create a memory database
Connection conn = DriverManager.getConnection("jdbc:sqlite:");
Statement stmt = conn.createStatement();
// Do some updates
stmt.executeUpdate("create table sample(id, name)");
stmt.executeUpdate("insert into sample values(1, \"leo\")");
stmt.executeUpdate("insert into sample values(2, \"yui\")");
// Dump the database contents to a file
stmt.executeUpdate("backup to backup.db");
Restore the database from a backup file:
// Create a memory database
Connection conn = DriverManager.getConnection("jdbc:sqlite:");
// Restore the database from a backup file
Statement stat = conn.createStatement();
stat.executeUpdate("restore from backup.db");

```

## Creating BLOB data ##

1. Create a table with a column of blob type: `create table T (id integer, data blob)`
1. Create a prepared statement with `?` symbol: `insert into T values(1, ?)`
1. Prepare a blob data in byte array (e.g., `byte[] data = ...`)
1. `preparedStatement.setBytes(1, data)`
1. `preparedStatement.execute()...`

## Reading Database Files in classpaths or network (read-only) ##

To load db files that can be found from the class loader (e.g., db 
files inside a jar file in the classpath), 
use `jdbc:sqlite::resource:` prefix. 

For example, here is an example to access an SQLite DB file, `sample.db` 
in a Java package `org.yourdomain`: 


```java

Connection conn = DriverManager.getConnection("jdbc:sqlite::resource:org/yourdomain/sample.db"); 

```

In addition, external DB resources can be used as follows: 


```java

Connection conn = DriverManager.getConnection("jdbc:sqlite::resource:http://www.xerial.org/svn/project/XerialJ/trunk/sqlite-jdbc/src/test/java/org/sqlite/sample.db"); 

```

To access db files inside some specific jar file (in local or remote), 
use the [JAR URL](http://java.sun.com/j2se/1.5.0/docs/api/java/net/JarURLConnection.html):


```java

Connection conn = DriverManager.getConnection("jdbc:sqlite::resource:jar:http://www.xerial.org/svn/project/XerialJ/trunk/sqlite-jdbc/src/test/resources/testdb.jar!/sample.db"); 

```

DB files will be extracted to a temporary folder specified in `System.getProperty("java.io.tmpdir")`.

## Configure Connections #


```java

SQLiteConfig config = new SQLiteConfig();
// config.setReadOnly(true);   
config.setSharedCache(true);
config.recursiveTriggers(true);
// ... other configuration can be set via SQLiteConfig object
Connection conn = DriverManager.getConnection("jdbc:sqlite:sample.db", config.toProperties());
```

## How to Use Encrypted Databases ##

*__Important: xerial/sqlite-jdbc does not support encryption out of the box, you need a special .dll/.so__*

SQLite support encryption of the database via special drivers and a key. To use an encrypted database you need a driver which supports encrypted database via `pragma key` or `pragma hexkey`, e.g. SQLite SSE or SQLCipher. You need to specify those drivers via directly referencing the .dll/.so through:

```
-Dorg.sqlite.lib.path=.
-Dorg.sqlite.lib.name=sqlite_cryption_support.dll
```

Now the only need to specify the password is via:
```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:db.sqlite", "", "password");
```
### Binary Passphrase
If you need to provide the password in binary form, you have to specify how the provided .dll/.so needs it. There are two different modes available:

#### SSE:
The binary password is provided via `pragma hexkey='AE...'`

#### SQLCipher:
The binary password is provided via `pragma key="x'AE...'"`

You set the mode at the connectionstring level:
```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:db.sqlite?hexkey_mode=sse", "", "AE...");
```
