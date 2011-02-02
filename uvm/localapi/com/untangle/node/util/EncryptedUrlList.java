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
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * <code>UrlList</code> that holds entries that are encoded as
 * described in:
 * {@link http://wiki.mozilla.org/Phishing_Protection:_Server_Spec}.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class EncryptedUrlList extends UrlList
{
    private static final Pattern TUPLE_PATTERN = Pattern.compile("([+-])([0-9A-Fa-f]+)");
    private static final DatabaseEntry EMPTY_ENTRY = new DatabaseEntry(new byte[] { ' ' });
    private final Logger logger = Logger.getLogger(getClass());

    public EncryptedUrlList(URL databaseUrl, String owner, String dbName)
        throws DatabaseException, IOException
    {
        super(databaseUrl, owner, dbName, null, null);
    }

    public EncryptedUrlList(URL databaseUrl, String owner, String dbName,
                            Map<String, String> extraParams, File initFile)
        throws DatabaseException, IOException
    {
        super(databaseUrl, owner, dbName, extraParams, initFile);
    }

    // UrlList methods --------------------------------------------------------

    public boolean contains(String proto, String host, String uri)
    {
        do {
            String url = host + uri;

            if (findKey(url)) {
                return true;
            }
        } while (null != (uri = nextUri(uri)));

        return false;
    }

    protected boolean updateDatabase(Database db, BufferedReader br)
        throws IOException
    {
        boolean blankLine = false;

        String line;
        while (null != (line = br.readLine())) {
            blankLine = line.trim().equals("");

            Matcher matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                boolean add = matcher.group(1).equals("+");
                byte[] host = matcher.group(2).getBytes();

                try {
                    if (add) {
                        db.put(null, new DatabaseEntry(host), EMPTY_ENTRY);
                    } else {
                        db.delete(null, new DatabaseEntry(host));
                    }
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }

        return blankLine;
    }

    private boolean findKey(String url)
    {
        OperationStatus status;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder(new BigInteger(1, digest).toString(16));
            while (sb.length() < 16) {
                sb.insert(0, sb.charAt(0));
            }
            String hexDigest = sb.toString();
            DatabaseEntry k = new DatabaseEntry(hexDigest.getBytes());
            status = db.get(null, k, new DatabaseEntry(),
                            LockMode.READ_UNCOMMITTED);
            if (OperationStatus.SUCCESS == status) {
                return true;
            } else {
                return false;
            }
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return false;
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
            return false;
        }
    }

    // private methods --------------------------------------------------------

    private String nextUri(String uri)
    {
        int i = uri.indexOf('?');
        if (i >= 0) {
            return uri.substring(0, i);
        } else {
            i = uri.lastIndexOf("/", uri.length() - 2);
            if (i >= 0) {
                return uri.substring(0, i + 1);
            } else {
                return null;
            }
        }
    }
}
