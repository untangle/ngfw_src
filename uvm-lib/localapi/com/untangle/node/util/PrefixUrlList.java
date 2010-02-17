/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * An UrlList that matches by URL prefix.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class PrefixUrlList extends UrlList
{
    private static final Pattern TUPLE_PATTERN = Pattern.compile("([+-])(https?://([^/\t]+)/[^\t]*)\tc");

    private final Logger logger = Logger.getLogger(getClass());

    public PrefixUrlList(URL databaseUrl, String owner, String dbName)
        throws DatabaseException, IOException
    {
        super(databaseUrl, owner, dbName, null, null);
    }

    public PrefixUrlList(URL databaseUrl, String owner, String dbName,
                         Map<String, String> extraParams, File initFile)
        throws DatabaseException, IOException
    {
        super(databaseUrl, owner, dbName, extraParams, initFile);
    }

    // UrlList methods -------------------------------------------------------

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

    protected boolean updateDatabase(Database db, BufferedReader br)
        throws IOException
    {
        String line;

        byte[] lastHost = null;
        StringBuilder sb = new StringBuilder();
        DatabaseEntry k = new DatabaseEntry();
        DatabaseEntry v = new DatabaseEntry();
        DatabaseEntry t = new DatabaseEntry();

        boolean blankLine = false;

        while (null != (line = br.readLine())) {
            blankLine = line.trim().equals("");

            Matcher matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                boolean add = matcher.group(1).equals("+");

                byte[] prefix = matcher.group(2).getBytes();
                byte[] host = matcher.group(3).getBytes();

                if (logger.isDebugEnabled()) {
                    logger.debug("add? " + add + " host: " + new String(host)
                                 + " prefix: " + new String(prefix));
                }

                if (null == lastHost) {
                    lastHost = host;
                    k.setData(host);
                    try {
                        OperationStatus s = db.get(null, k, v, LockMode.DEFAULT);

                        if (OperationStatus.SUCCESS == s) {
                            sb.append(new AsciiString(v.getData()));
                        }
                    } catch (DatabaseException exn) {
                        logger.warn("could not get entry", exn);
                    }
                } else if (!Arrays.equals(lastHost, host)) {
                    k.setData(lastHost);
                    v.setData(sb.toString().getBytes());
                    try {
                        if (0 == sb.length()) {
                            if (OperationStatus.SUCCESS == db.get(null, k, t, LockMode.DEFAULT)) {
                                db.delete(null, k);
                            }
                        } else {
                            db.put(null, k, v);
                        }
                    } catch (DatabaseException exn) {
                        logger.warn("could not save entry", exn);
                    }

                    lastHost = host;

                    k.setData(host);
                    sb.delete(0, sb.length());
                    try {
                        OperationStatus s = db.get(null, k, v, LockMode.DEFAULT);

                        if (OperationStatus.SUCCESS == s) {
                            sb.append(new AsciiString(v.getData()));
                        }
                    } catch (DatabaseException exn) {
                        logger.warn("could not get entry", exn);
                    }
                }

                if (add) {
                    add(sb, prefix);
                } else {
                    del(sb, prefix);
                }
            }
        }

        if (null != lastHost) {
            k.setData(lastHost);
            v.setData(sb.toString().getBytes());
            try {
                if (0 == sb.length()) {
                    if (OperationStatus.SUCCESS == db.get(null, k, t, LockMode.DEFAULT)) {
                        db.delete(null, k);
                    }
                } else {
                    db.put(null, k, v);
                }
            } catch (DatabaseException exn) {
                logger.warn("could not save entry", exn);
            }
        }

        return blankLine;
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
        return !pat.equals("") && str.startsWith(pat);
    }

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
            if (OperationStatus.SUCCESS == status) {
                byte[] data = v.getData();
                return getValues(host, v.getData());
            } else {
                return Collections.emptyList();
            }
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
            return Collections.emptyList();
        }
    }
}
