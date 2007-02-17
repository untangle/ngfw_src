/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.tran.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;

public abstract class UrlList
{
    private final Database db;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public UrlList(File dbHome, String dbName)
        throws DatabaseException, IOException
    {
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        Environment dbEnv = new Environment(dbHome, envCfg);

        // Open the database. Create it if it does not already exist.
        DatabaseConfig dbCfg = new DatabaseConfig();
        dbCfg.setAllowCreate(true);
        db = dbEnv.openDatabase(null, dbName, dbCfg);

        updateDatabase(db);
    }

    // public methods ---------------------------------------------------------

    // this method for pre-normalized parts
    public boolean contains(String proto, String host, String uri)
    {
        String url = proto + "://" + host + uri;

        for (String p : getPatterns(host)) {
            if (url.matches(p)) {
                return true;
            }
        }

        return false;
    }

    public String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    // protected methods ------------------------------------------------------

    protected abstract void updateDatabase(Database db) throws IOException;

    protected abstract byte[] getKey(byte[] host);
    protected abstract List<String> getValues(byte[] host, byte[] data);

    // private methods --------------------------------------------------------

    private List<String> getPatterns(String hostStr)
    {
        byte[] host = hostStr.getBytes();

        byte[] hash = getKey(host);
        if (null == hash) {
            return Collections.emptyList();
        }
        DatabaseEntry k = new DatabaseEntry(hash);
        DatabaseEntry v = new DatabaseEntry();

        // XXX hopefully we can just use READ_UNCOMMITTED
        OperationStatus status;
        try {
            status = db.get(null, k, v, LockMode.DEFAULT);
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
            return Collections.emptyList();
        }

        if (OperationStatus.SUCCESS == status) {
            byte[] data = v.getData();

            return getValues(host, v.getData());
        } else {
            return Collections.emptyList();
        }
    }
}
