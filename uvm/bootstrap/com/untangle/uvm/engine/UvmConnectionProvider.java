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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.connection.ConnectionProvider;

/**
 * Provides database connections to Hibernate.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmConnectionProvider implements ConnectionProvider
{
    private final DataSourceFactory dsf = DataSourceFactory.factory();

    public Connection getConnection() throws SQLException
    {
        return dsf.getConnection();
    }

    public void closeConnection(Connection c) throws SQLException
    {
        c.close();
    }

    public boolean supportsAggressiveRelease()
    {
        return false;
    }

    /**
     * Does nothing, hibernate does not decide when to close the pool,
     * we do.
     */
    public void close() { }

    public void configure(Properties p) { }
}
