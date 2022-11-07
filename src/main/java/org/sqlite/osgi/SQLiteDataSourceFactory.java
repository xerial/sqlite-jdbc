package org.sqlite.osgi;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Consumer;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.osgi.service.jdbc.DataSourceFactory;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class SQLiteDataSourceFactory implements DataSourceFactory {

    @Override
    public DataSource createDataSource(Properties props) throws SQLException {
        SQLiteDataSource dataSource = new SQLiteDataSource(getConfig(props));
        setBasicDataSourceProperties(props, dataSource);
        return dataSource;
    }

    @Override
    public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props)
            throws SQLException {

        SQLiteConnectionPoolDataSource poolDataSource =
                new SQLiteConnectionPoolDataSource(getConfig(props));
        setBasicDataSourceProperties(props, poolDataSource);
        return poolDataSource;
    }

    @Override
    public XADataSource createXADataSource(Properties props) throws SQLException {
        throw new SQLException("XADataSource is not supported by SQLite");
    }

    @Override
    public Driver createDriver(Properties props) throws SQLException {
        return new JDBC();
    }

    /**
     * Method to transfer a property to a setter method
     *
     * @param props
     * @param key
     * @param consumer
     */
    private static void setStandardProperty(
            Properties props, String key, Consumer<String> consumer) {
        String value = props.getProperty(key);
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * Set basic properties common to {@link SQLiteDataSource}s
     *
     * @param props
     * @param dataSource
     */
    private static void setBasicDataSourceProperties(
            Properties props, SQLiteDataSource dataSource) {
        if (props != null) {
            setStandardProperty(
                    props, DataSourceFactory.JDBC_DATABASE_NAME, dataSource::setDatabaseName);
            setStandardProperty(props, DataSourceFactory.JDBC_URL, dataSource::setUrl);
        }
    }

    /**
     * converts user supplied properties into an internal {@link SQLiteConfig} object
     *
     * @param userProperties the user properties, might be <code>null</code>
     * @return a {@link SQLiteConfig} config object reflecting the given user properties
     */
    private static SQLiteConfig getConfig(Properties userProperties) {
        SQLiteConfig config;
        if (userProperties == null) {
            config = new SQLiteConfig();
        } else {
            Properties properties = new Properties(userProperties);
            setStandardProperty(
                    userProperties,
                    DataSourceFactory.JDBC_USER,
                    v -> properties.setProperty("user", v));
            setStandardProperty(
                    userProperties,
                    DataSourceFactory.JDBC_PASSWORD,
                    v -> properties.setProperty("pass", v));
            config = new SQLiteConfig(properties);
        }
        return config;
    }
}
