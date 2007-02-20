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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import org.apache.log4j.Logger;

public class PrefixUrlList extends UrlList
{
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[[^ ]+ ([0-9.]+)\\]");
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\+(https?://([^/\t]+)/[^\t])\tc");

    private final URL databaseUrl;

    private final Logger logger = Logger.getLogger(getClass());

    PrefixUrlList(File dbHome, String dbName, URL databaseUrl)
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

                System.out.println("PUTTING HOST: " + new String(host)
                                   + " prefix: " + new String(prefix));
                try {
                    db.put(null, new DatabaseEntry(host),
                           new DatabaseEntry(prefix));
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }

        return version;
    }

    protected String updateDatabase(Database db, String version) throws IOException
    {
        // XXX implement update
        return null;
    }

    protected byte[] getKey(byte[] host)
    {
        return host;
    }

    protected List<String> getValues(byte[] host, byte[] data)
    {
        return Collections.singletonList(new String(data));
    }

    protected boolean matches(String str, String pat)
    {
        return str.startsWith(pat);
    }
}
