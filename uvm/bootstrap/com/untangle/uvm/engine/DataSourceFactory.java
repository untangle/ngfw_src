/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Provides database connections from the connection pool.
 */
public class DataSourceFactory
{
    private static final DataSourceFactory FACTORY = new DataSourceFactory();

    private final ComboPooledDataSource dataSource;

    private DataSourceFactory()
    {
        dataSource = new ComboPooledDataSource();
        try {
            dataSource.setDriverClass("org.postgresql.Driver");
        } catch (PropertyVetoException exn) {
            throw new RuntimeException(exn); // won't happen
        }
        dataSource.setJdbcUrl("jdbc:postgresql://localhost/uvm?charset=unicode");
        dataSource.setUser("postgres");
        dataSource.setPassword("foo");
        dataSource.setMaxStatements(180);
        dataSource.setMinPoolSize(5);
        dataSource.setMaxPoolSize(50);
        dataSource.setMaxIdleTime(300);
        dataSource.setTestConnectionOnCheckout(true);
        dataSource.setPreferredTestQuery("SELECT 1");
    }

    // public factories -------------------------------------------------------

    public static DataSourceFactory factory()
    {
        return FACTORY;
    }

    // public methods ---------------------------------------------------------

    /**
     * Get new database connection from the pool.
     *
     * @return a database connection.
     * @exception SQLException if cannot get a new connection.
     */
    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }

    /**
     * Closes the connection, returning it to the pool.
     *
     * @param c connection to close.
     * @exception SQLException if cannot close the connection.
     */
    public void closeConnection(Connection c) throws SQLException
    {
        c.close();
    }

    // package protected methods ----------------------------------------------

    /**
     * Destroys the connection pool, freeing its resources.
     *
     * @exception SQLException if pool cannot be destroyed.
     */
    void destroy() throws SQLException
    {
        //DataSources.destroy(dataSource);
    }
}
