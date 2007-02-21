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
    private static final byte[] VERSION_KEY = "__version".getBytes();

    private final Database db;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public UrlList(File dbHome, String dbName)
        throws DatabaseException
    {
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        Environment dbEnv = new Environment(dbHome, envCfg);

        // Open the database. Create it if it does not already exist.
        DatabaseConfig dbCfg = new DatabaseConfig();
        dbCfg.setAllowCreate(true);
        db = dbEnv.openDatabase(null, dbName, dbCfg);
    }

    // public methods ---------------------------------------------------------

    public void update()
        throws DatabaseException, IOException
    {
        try {
            DatabaseEntry k = new DatabaseEntry(VERSION_KEY);
            DatabaseEntry v = new DatabaseEntry();

            String version;
            OperationStatus s = db.get(null, k, v, LockMode.READ_UNCOMMITTED);
            if (OperationStatus.SUCCESS == s) {
                byte[] d = v.getData();
                version = updateDatabase(db, new String(d));
            } else {
                version = initDatabase(db);
            }
            if (null != version) {
                db.put(null, new DatabaseEntry(VERSION_KEY), new DatabaseEntry(version.getBytes()));
            }
        } catch (DatabaseException exn) {
            logger.warn("could not get database version", exn);
        }
    }

    public void close()
        throws DatabaseException
    {
        db.close();
        db.getEnvironment().close();
    }

    // this method for pre-normalized parts
    public boolean contains(String proto, String host, String uri)
    {
        String url = proto + "://" + host + uri;

        for (String p : getPatterns(host)) {
            if (matches(url, p)) {
                return true;
            }
        }

        return false;
    }

    // protected methods ------------------------------------------------------

    protected abstract String initDatabase(Database db)
        throws IOException;

    protected abstract String updateDatabase(Database db, String currentVer)
        throws IOException;

    protected abstract byte[] getKey(byte[] host);
    protected abstract List<String> getValues(byte[] host, byte[] data);
    protected abstract boolean matches(String str, String pattern);

    protected List<String> split(byte[] buf)
    {
        List<String> l = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            char c = (char)buf[i];
            if ('\t' == c) {
                l.add(sb.toString());
                sb.delete(0, sb.length());
            } else {
                sb.append(c);
            }
        }
        l.add(sb.toString());

        return l;
    }

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

        OperationStatus status;
        try {
            status = db.get(null, k, v, LockMode.READ_UNCOMMITTED);
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
