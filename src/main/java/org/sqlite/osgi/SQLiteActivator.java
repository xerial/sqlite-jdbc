package org.sqlite.osgi;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;
import org.sqlite.JDBC;
import org.sqlite.SQLiteJDBCLoader;

public class SQLiteActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, JDBC.class.getName());
        properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, "SQLite JDBC driver");
        properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION, SQLiteJDBCLoader.getVersion());
        context.registerService(DataSourceFactory.class, new SQLiteDataSourceFactory(), properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {}
}
