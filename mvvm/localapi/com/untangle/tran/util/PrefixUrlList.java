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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;

public class PrefixUrlList extends UrlList
{
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[[^ ]+ ([0-9.]+)\\]");
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\+(https?://([^/\t]+)/[^\t]*)\tc");

    private final URL databaseUrl;

    private final Logger logger = Logger.getLogger(getClass());

    public PrefixUrlList(File dbHome, String dbName, URL databaseUrl)
        throws DatabaseException, IOException
    {
        super(dbHome, dbName);
        this.databaseUrl = databaseUrl;
    }

    // UrlList methods --------------------------------------------------------

    protected String initDatabase(Database db)
        throws IOException
    {
        InputStream is = databaseUrl.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();

        String version;

        Matcher matcher = VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            version = matcher.group(1);
        } else {
            logger.warn("No version number: " + line);
            version = null;
        }

        while (null != (line = br.readLine())) {
            matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                byte[] prefix = matcher.group(1).getBytes();
                byte[] host = matcher.group(2).getBytes();

                try {
                    DatabaseEntry k = new DatabaseEntry(host);
                    DatabaseEntry v = new DatabaseEntry();

                    OperationStatus s = db.get(null, k, v, LockMode.READ_UNCOMMITTED);
                    if (OperationStatus.SUCCESS == s) {
                        byte[] data = v.getData();
                        byte[] newData = new byte[data.length + 1 + prefix.length];
                        System.arraycopy(data, 0, newData, 0, data.length);
                        newData[data.length] = '\t';
                        System.arraycopy(prefix, 0, newData, data.length + 1,
                                         prefix.length);

                        v.setData(newData);
                    } else {
                        v.setData(prefix);
                    }

                    db.put(null, k, v);
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }

        return version;
    }

    protected String updateDatabase(Database db, String version) throws IOException
    {
        // XXX implement proper updating
        Cursor c = null;
        try {
            DatabaseEntry k = new DatabaseEntry();
            DatabaseEntry v = new DatabaseEntry();

            c = db.openCursor(null, null);
            while (OperationStatus.SUCCESS == c.getNext(k, v, LockMode.DEFAULT)) {
                c.delete();
            }
        } catch (DatabaseException exn) {
            logger.warn("could not clear database");
        } finally {
            if (null != c) {
                try {
                    c.close();
                } catch (DatabaseException exn) {
                    logger.warn("could not close cursor", exn);
                }
            }
        }

        return initDatabase(db);
    }

    protected byte[] getKey(byte[] host)
    {
        return host;
    }

    protected List<String> getValues(byte[] host, byte[] data)
    {
        return split(data);
    }

    protected boolean matches(String str, String pat)
    {
        return str.startsWith(pat);
    }
}
