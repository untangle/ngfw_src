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

package com.untangle.tran.httpblocker;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.untangle.tran.util.AsciiString;
import org.apache.log4j.Logger;

class BlacklistCache
{
    // XXX use the connection pool
    private static final String DB_URL
        = "jdbc:postgresql://localhost/blacklist";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWD = "foo";

    private static final BlacklistCache CACHE = new BlacklistCache();

    // We can leak HashMap entries, there are a small number of categories
    private final Map<String, WeakReference<CharSequence[]>> urls
        = new HashMap<String, WeakReference<CharSequence[]>>();
    private final Map<String, WeakReference<CharSequence[]>>domains
        = new HashMap<String, WeakReference<CharSequence[]>>();

    private final Logger logger = Logger.getLogger(BlacklistCache.class);

    // constructors -----------------------------------------------------------

    private BlacklistCache() { }

    // static factories -------------------------------------------------------

    static BlacklistCache cache()
    {
        return CACHE;
    }

    // package protected methods ----------------------------------------------

    CharSequence[] getUrlBlacklist(String category)
    {
        CharSequence[] blacklist;

        synchronized (urls) {
            WeakReference<CharSequence[]> ref = urls.get(category);
            blacklist = null == ref ? null : ref.get();

            if (null == blacklist) {
                blacklist = populateArray("urls", "url", category);
                urls.put(category, new WeakReference(blacklist));
            }
        }

        return blacklist;
    }

    CharSequence[] getDomainBlacklist(String category)
    {
        CharSequence[] blacklist;

        synchronized (domains) {
            WeakReference<CharSequence[]> ref = domains.get(category);
            blacklist = null == ref ? null : ref.get();

            if (null == blacklist) {
                blacklist = populateArray("domains", "domain", category);
                domains.put(category, new WeakReference(blacklist));
            }
        }

        return blacklist;
    }

    // private methods --------------------------------------------------------

    private CharSequence[] populateArray(String table, String column,
                                        String category)
    {
        CharSequence[] a;

        Connection c = null;
        try {
            c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWD);
            PreparedStatement ps = c.prepareStatement
                ("SELECT count(*) FROM " + table + " WHERE category = ?");
            ps.setString(1, category);

            ResultSet rs = ps.executeQuery();
            rs.next();

            int n = rs.getInt(1);

            a = new CharSequence[n];

            ps = c.prepareStatement
                ("SELECT " + column + " FROM " + table + " WHERE category = ?");
            ps.setString(1, category);

            rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                a[i++] = new AsciiString(rs.getBytes(1));
            }

            if (n != i) {
                logger.warn("blacklist wrong size: " + i + " should be: " + c);
            }

        } catch (SQLException exn) {
            logger.warn("could not populate blacklist array", exn);
            a = new CharSequence[0];
        } finally {
            try {
                if (null != c) {
                    c.close();
                }
            } catch (SQLException exn) {
                logger.warn("could not close connection", exn);
            }
        }

        return a;
    }
}
