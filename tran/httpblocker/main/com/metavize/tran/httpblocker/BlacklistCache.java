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

package com.metavize.tran.httpblocker;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

class BlacklistCache
{
    // XXX use the connection pool
    private static final String DB_URL
        = "jdbc:postgresql://localhost/blacklist";
    private static final String DB_USER = "metavize";
    private static final String DB_PASSWD = "foo";

    private static final BlacklistCache CACHE = new BlacklistCache();

    // We can leak HashMap entries, there are a small number of categories
    private final Map<String, WeakReference<String[]>> urls
        = new HashMap<String, WeakReference<String[]>>();
    private final Map<String, WeakReference<String[]>>domains
        = new HashMap<String, WeakReference<String[]>>();

    private final Logger logger = Logger.getLogger(BlacklistCache.class);

    // constructors -----------------------------------------------------------

    private BlacklistCache() { }

    // static factories -------------------------------------------------------

    static BlacklistCache cache()
    {
        return CACHE;
    }

    // package protected methods ----------------------------------------------

    String[] getUrlBlacklist(String category)
    {
        String[] blacklist;

        synchronized (urls) {
            WeakReference<String[]> ref = urls.get(category);
            blacklist = null == ref ? null : ref.get();

            if (null == blacklist) {
                blacklist = populateArray("urls", "url", category);
                urls.put(category, new WeakReference(blacklist));
            }
        }

        return blacklist;
    }

    String[] getDomainBlacklist(String category)
    {
        String[] blacklist;

        synchronized (domains) {
            WeakReference<String[]> ref = domains.get(category);
            blacklist = null == ref ? null : ref.get();

            if (null == blacklist) {
                blacklist = populateArray("domains", "domain", category);
                domains.put(category, new WeakReference(blacklist));
            }
        }

        return blacklist;
    }

    // private methods --------------------------------------------------------

    private String[] populateArray(String table, String column,
                                   String category)
    {
        String[] a;

        Connection c = null;
        try {
            c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWD);
            PreparedStatement ps = c.prepareStatement
                ("SELECT count(*) FROM " + table + " WHERE category = ?");
            ps.setString(1, category);

            ResultSet rs = ps.executeQuery();
            rs.next();

            int n = rs.getInt(1);

            a = new String[n];

            ps = c.prepareStatement
                ("SELECT " + column + " FROM " + table + " WHERE category = ?");
            ps.setString(1, category);

            rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                a[i++] = rs.getString(1);
            }

            if (n != i) {
                logger.warn("blacklist wrong size: " + i + " should be: " + c);
            }

        } catch (SQLException exn) {
            logger.warn("could not populate blacklist array", exn);
            a = new String[0];
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
