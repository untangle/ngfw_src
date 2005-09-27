/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.connection.ConnectionProvider;

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

    public void close() { }
    public void configure(Properties p) { }
}
