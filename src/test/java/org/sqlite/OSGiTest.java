package org.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.laeubisoft.osgi.junit5.framework.annotations.EmbeddedFramework;
import de.laeubisoft.osgi.junit5.framework.annotations.WithBundle;
import de.laeubisoft.osgi.junit5.framework.extension.FrameworkExtension;
import de.laeubisoft.osgi.junit5.framework.services.FrameworkEvents;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.launch.Framework;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.service.ServiceExtension;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

@WithBundle(value = "org.xerial.sqlite-jdbc", start = true, isolated = true)
@WithBundle(value = "org.osgi.service.jdbc")
@ExtendWith(ServiceExtension.class)
@ExtendWith(FrameworkExtension.class)
public class OSGiTest {

    @BeforeAll
    public static void beforeTest(
            @EmbeddedFramework Framework framework,
            @InjectService FrameworkEvents frameworkEvents) {
        FrameworkExtension.printBundles(framework, System.out::println);
        FrameworkExtension.printComponents(framework, System.out::println);
        frameworkEvents.assertErrorFree();
    }

    @InjectService(filter = "(osgi.jdbc.driver.class=org.sqlite.JDBC)")
    ServiceAware<DataSourceFactory> datasourceFactory;

    @BeforeEach
    public void checkService() {
        assertEquals(
                1,
                datasourceFactory.size(),
                "There should be exactly one DataSourceFactory for SQLite!");
    }

    @Test
    public void testCreateDriver() throws SQLException {
        Driver driver = getFactory().createDriver(null);
        assertClass(JDBC.class, driver);
    }

    @Test
    public void testCreateDataSource() throws SQLException {
        DataSource dataSource = getFactory().createDataSource(null);
        assertClass(SQLiteDataSource.class, dataSource);
    }

    @Test
    public void testCreateConnectionPoolDataSource() throws SQLException {
        ConnectionPoolDataSource dataSource = getFactory().createConnectionPoolDataSource(null);
        assertClass(SQLiteConnectionPoolDataSource.class, dataSource);
    }

    @Test
    public void testCreateXADataSource() throws SQLException {
        DataSourceFactory service = getFactory();
        assertThrows(SQLException.class, () -> service.createXADataSource(null));
    }

    @Test
    public void testCreateConnection() throws SQLException {

        Properties props = new Properties();
        props.setProperty(DataSourceFactory.JDBC_URL, "jdbc:sqlite:");
        DataSource dataSource = getFactory().createDataSource(props);
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("create table sample(id, name)");
            stmt.executeUpdate("insert into sample values(1, \"leo\")");
            stmt.executeUpdate("insert into sample values(2, \"yui\")");
        }
    }

    private DataSourceFactory getFactory() {
        DataSourceFactory service = datasourceFactory.getService();
        assertNotNull(service);
        return service;
    }

    private static void assertClass(Class<?> clazz, Object obj) {
        assertNotNull(obj);
        assertEquals(clazz.getName(), obj.getClass().getName());
    }
}
