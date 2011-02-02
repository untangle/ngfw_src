/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

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
public class UvmConnectionProvider implements ConnectionProvider
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
