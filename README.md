# SQLite JDBC Driver
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/xerial/sqlite-jdbc/CI/master)](https://github.com/xerial/sqlite-jdbc/actions/workflows/test.yml?query=branch%3Amaster)
[![Join the chat at https://gitter.im/xerial/sqlite-jdbc](https://badges.gitter.im/xerial/sqlite-jdbc.svg)](https://gitter.im/xerial/sqlite-jdbc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.xerial/sqlite-jdbc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.xerial/sqlite-jdbc/)
[![javadoc](https://javadoc.io/badge2/org.xerial/sqlite-jdbc/javadoc.svg)](https://javadoc.io/doc/org.xerial/sqlite-jdbc)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/org.xerial/sqlite-jdbc?color=blue&label=maven%20snapshot&server=https%3A%2F%2Foss.sonatype.org%2F)](https://oss.sonatype.org/content/repositories/snapshots/org/xerial/sqlite-jdbc/)

SQLite JDBC is a library for accessing and creating [SQLite](http://sqlite.org) database files in Java.

Our SQLiteJDBC library requires no configuration since native libraries for major OSs, including Windows, macOS, Linux etc., are assembled into a single JAR (Java Archive) file.

# Usage

:arrow_right: More usage examples and configuration are available in [USAGE.md](USAGE.md)

SQLite JDBC is a library for accessing SQLite databases through the JDBC API. For the general usage of JDBC, see [JDBC Tutorial](http://docs.oracle.com/javase/tutorial/jdbc/index.html) or [Oracle JDBC Documentation](http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136101.html).

1. [Download](#download) `sqlite-jdbc-(VERSION).jar`
then append this jar file into your classpath.
2. Open a SQLite database connection from your code. (see the example below)

## Example usage
Assuming `sqlite-jdbc-(VERSION).jar` is placed in the current directory.

```shell
> javac Sample.java
> java -classpath ".;sqlite-jdbc-(VERSION).jar" Sample   # in Windows
or
> java -classpath ".:sqlite-jdbc-(VERSION).jar" Sample   # in macOS or Linux
name = leo
id = 1
name = yui
id = 2
```    

**Sample.java**

```java
    import java.sql.Connection;
    import java.sql.DriverManager;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Statement;

    public class Sample
    {
      public static void main(String[] args)
      {
        Connection connection = null;
        try
        {
          // create a database connection
          connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
          Statement statement = connection.createStatement();
          statement.setQueryTimeout(30);  // set timeout to 30 sec.

          statement.executeUpdate("drop table if exists person");
          statement.executeUpdate("create table person (id integer, name string)");
          statement.executeUpdate("insert into person values(1, 'leo')");
          statement.executeUpdate("insert into person values(2, 'yui')");
          ResultSet rs = statement.executeQuery("select * from person");
          while(rs.next())
          {
            // read the result set
            System.out.println("name = " + rs.getString("name"));
            System.out.println("id = " + rs.getInt("id"));
          }
        }
        catch(SQLException e)
        {
          // if the error message is "out of memory",
          // it probably means no database file is found
          System.err.println(e.getMessage());
        }
        finally
        {
          try
          {
            if(connection != null)
              connection.close();
          }
          catch(SQLException e)
          {
            // connection close failed.
            System.err.println(e.getMessage());
          }
        }
      }
    }
```

# How does SQLiteJDBC work?
Our SQLite JDBC driver package (i.e., `sqlite-jdbc-(VERSION).jar`) contains three
types of native SQLite libraries (`sqlite-jdbc.dll`, `sqlite-jdbc.jnilib`, `sqlite-jdbc.so`),
each of them is compiled for Windows, macOS and Linux. An appropriate native library
file is automatically extracted into your OS's temporary folder, when your program
loads `org.sqlite.JDBC` driver.

## Supported Operating Systems
Since sqlite-jdbc-3.6.19, the natively compiled SQLite engines will be used for
the following operating systems:

|              | x86 | x86_64 | armv5 | armv6 | armv7 | arm64 | ppc64 |
|--------------|-----|--------|-------|-------|-------|-------|-------|
| Windows      | ✔   | ✔      |       |       | ✔     | ✔     |       |
| macOS        |     | ✔      |       |       |       | ✔     |       |
| Linux (libc) | ✔   | ✔      | ✔     | ✔     | ✔     | ✔     | ✔     |
| Linux (musl) | ✔   | ✔      |       |       |       | ✔     |       |
| Android      | ✔   | ✔      | ✔     |       |       | ✔     |       |
| FreeBSD      | ✔   | ✔      |       |       |       | ✔     |       |


In the other OSs not listed above, the pure-java SQLite is used. (Applies to versions before 3.7.15)

If you want to use the native library for your OS, [build the source from scratch](./CONTRIBUTING.md).

# Download

Download from [Maven Central](https://search.maven.org/artifact/org.xerial/sqlite-jdbc) or from the [releases](https://github.com/xerial/sqlite-jdbc/releases) page.

```xml
<dependencies>
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>(version)</version>
    </dependency>
</dependencies>
```

Snapshots of the development version are available in [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/xerial/sqlite-jdbc/).

## Project versioning explained
The project's version follows the version of the SQLite library that is bundled in the jar, with an extra digit to denote the project's increment.

For example, if the SQLite version is `3.39.2`, the project version will be `3.39.2.x`, where `x` starts at 0, and increments with every release that is not changing the SQLite version.

If the SQLite version is updated to `3.40.0`, the project version will be updated to `3.40.0.0`.

## Hint for maven-shade-plugin

You may need to add shade plugin transformer to solve `No suitable driver found for jdbc:sqlite:` issue.

```xml
<transformer
	implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
	<resource>META-INF/services/java.sql.Driver</resource>
</transformer>
```

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>(version)</version>
</dependency>
```

# How can I help?

We are always looking for:
- **Reviewers** for issues or PRs, you can check https://github.com/xerial/sqlite-jdbc/labels/review%20wanted
- **Contributors** to submit PRs, you can check https://github.com/xerial/sqlite-jdbc/labels/help%20wanted and https://github.com/xerial/sqlite-jdbc/labels/good%20first%20issue

Please read our [contribution](./CONTRIBUTING.md) guide.
