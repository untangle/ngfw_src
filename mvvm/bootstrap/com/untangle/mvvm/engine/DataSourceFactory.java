/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

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
        dataSource.setJdbcUrl("jdbc:postgresql://localhost/mvvm?charset=unicode");
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

    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }

    public void closeConnection(Connection c) throws SQLException
    {
        c.close();
    }

    // package protected methods ----------------------------------------------

    void destroy() throws SQLException
    {
        //DataSources.destroy(dataSource);
    }
}
