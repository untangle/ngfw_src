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
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;

public class PrefixUrlList extends UrlList
{
    private static final Pattern TUPLE_PATTERN = Pattern.compile("([+-])(https?://([^/\t]+)/[^\t]*)\tc");

    private final Logger logger = Logger.getLogger(getClass());

    public PrefixUrlList(File dbHome, URL databaseUrl, String dbName)
        throws DatabaseException, IOException
    {
        super(dbHome, databaseUrl, dbName);
    }

    // UrlList methods --------------------------------------------------------

    protected void updateDatabase(Database db, BufferedReader br)
        throws IOException
    {
        String line;

        while (null != (line = br.readLine())) {
            Matcher matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                boolean add = matcher.group(1).equals("+");

                byte[] prefix = matcher.group(2).getBytes();
                byte[] host = matcher.group(3).getBytes();

                if (logger.isDebugEnabled()) {
                    logger.debug("add? " + add + " host: " + new String(host)
                                 + " prefix: " + new String(prefix));
                }

                try {
                    DatabaseEntry k = new DatabaseEntry(host);
                    DatabaseEntry v = new DatabaseEntry();

                    OperationStatus s = db.get(null, k, v,
                                               LockMode.READ_UNCOMMITTED);
                    if (OperationStatus.SUCCESS == s) {
                        byte[] data = v.getData();
                        byte[] newData = add ? add(data, prefix)
                            : del(data, prefix);
                        if (0 == newData.length) {
                            db.delete(null, k);
                        } else {
                            v.setData(newData);
                            db.put(null, k, v);
                        }
                    } else {
                        if (add) {
                            v.setData(prefix);
                            db.put(null, k, v);
                        }
                    }
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }
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
