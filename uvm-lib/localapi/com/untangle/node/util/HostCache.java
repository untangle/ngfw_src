/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/Blacklist.java $
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

package com.untangle.node.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Pulse;

public class HostCache
{
    private final long CACHE_TTL = 604800000; // 1 week

    private Database db;

    private final Logger logger = Logger.getLogger(getClass());
    private final Pulse cacheCleaner = new Pulse("HostCacheCleaner", true, new HostCacheCleaner());

    public HostCache()
    {
        cacheCleaner.start(3600000L);
    }

    public void open()
    {
        Environment dbEnv = LocalUvmContextFactory.context().getBdbEnvironment();
        DatabaseConfig dbCfg = new DatabaseConfig();
        dbCfg.setAllowCreate(true);

        Database d;
        try {
            d = dbEnv.openDatabase(null, "esoft-sitefinder", dbCfg);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
            d = null;
        }
        this.db = d;
    }

    public void close()
    {
        Database db = this.db;
        this.db = null;
        try {
            db.close();
        } catch (DatabaseException exn) {
            logger.warn("could not close database", exn);
        }
    }

    public String getCachedCategories(String domain, String uri)
    {
        Database db = this.db;
        
        if (null == db) {
            return null;
        }

        String dom = domain;

        while (null != dom) {
            String s = null;
            try {
                DatabaseEntry k = new DatabaseEntry(dom.getBytes());
                DatabaseEntry v = new DatabaseEntry();

                OperationStatus status = db.get(null, k, v,
                                                LockMode.READ_UNCOMMITTED);
                if (OperationStatus.SUCCESS == status) {
                    byte[] data = v.getData();
                    if (null != data) {
                        s = new String(data);
                    }
                }
            } catch (DatabaseException exn) {
                logger.warn("could not access database", exn);
            }

            if (null != s) {
                for (CacheRecord r : getRecords(s)) {
                    if (r.isExact()) {
                        if (domain.equals(dom)
                            && uri.toLowerCase().equals(r.uri.toLowerCase())) {
                            return r.getCategoryString();
                        }
                    } else {
                        if (uri.toLowerCase().startsWith(r.uri.toLowerCase())) {
                            return r.getCategoryString();
                        }
                    }
                }
            }

            dom = nextHost(dom);
        }
        return null;
    }

    public void cacheCategories(String url, String categories,
                                String origDomain, String origUri)
    {
        String[] s = url.split("/", 2);

        if (s.length >= 1) {
            String domain;
            String uri;

            boolean exact = false;
            domain = s[0];
            if (domain.startsWith(".")) {
                exact = true;
                domain = domain.substring(1);

                if (origDomain.endsWith(domain)) {
                    addRecord(origDomain, categories, true, origUri);
                }
            }

            if (s.length >= 2) {
                uri = "/" + s[1];
            } else {
                uri = "/";
            }

            addRecord(domain, categories, exact, uri);
        }
    }

    private void addRecord(String domain, String categories, boolean exact,
                           String uri)
    {
        if (db == null) {
            return;
        }

        DatabaseEntry k = new DatabaseEntry(domain.getBytes());
        DatabaseEntry v = new DatabaseEntry();

        String s = null;
        try {
            OperationStatus status = db.get(null, k, v,
                                            LockMode.READ_UNCOMMITTED);
            if (OperationStatus.SUCCESS == status) {
                byte[] data = v.getData();
                if (null != data) {
                    s = new String(data);
                }
            }
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
        }

        List<CacheRecord> l = getRecords(s);
        for (Iterator<CacheRecord> i = l.iterator(); i.hasNext(); ) {
            CacheRecord cr = i.next();
            if (uri.equals(cr.getUri())) {
                i.remove();
            }
        }

        l.add(new CacheRecord(domain, categories, exact, uri));

        s = writeRecords(l);
        v.setData(s.getBytes());

        try {
            db.put(null, k, v);
        } catch (DatabaseException exn) {
            logger.warn("could not set " + s, exn);
        }
    }

    private List<CacheRecord> getRecords(String s)
    {
        List<CacheRecord> l = new ArrayList<CacheRecord>();

        if (null != s) {
            for (String r : s.split("\t")) {
                CacheRecord cr = CacheRecord.readRecord(r);
                if (null != cr) {
                    l.add(cr);
                }
            }
        }

        return l;
    }

    private String writeRecords(List<CacheRecord> l)
    {
        StringBuilder sb = new StringBuilder();
        for (CacheRecord cr : l) {
            if (sb.length() > 0) {
                sb.append("\t");
            }

            cr.writeRecord(sb);
        }
        return sb.toString();
    }

    public void cleanCache()
    {
        this.cleanCache(false);
    }
    
    public void cleanCache(boolean expireAll)
    {
        Database cache = this.db;
        if (null == cache) {
            return;
        }

        long cutoff = System.currentTimeMillis() - CACHE_TTL;
        
        /* When expire all is true, just delete all of the records. */
        if ( expireAll ) {
            cutoff = Long.MAX_VALUE;
        }

        Cursor c = null;
        try {
            
            DatabaseEntry k = new DatabaseEntry();
            DatabaseEntry v = new DatabaseEntry();

            c = cache.openCursor(null, null);
            while (OperationStatus.SUCCESS == c.getNext(k, v, LockMode.DEFAULT)) {
                byte[] data = v.getData();
                if (null != data) {
                    String s = new String(data);
                    List<CacheRecord> l = getRecords(s);

                    for (Iterator<CacheRecord> i = l.iterator(); i.hasNext(); ) {
                        CacheRecord cr = i.next();
                        if (cr.getTime() < cutoff) {
                            i.remove();
                        }
                    }

                    if (0 < l.size()) {
                        s = writeRecords(l);
                        v.setData(s.getBytes());
                        c.putCurrent(v);
                    } else {
                        c.delete();
                    }
                }
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
    }

    // XXX duplicated code
    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (-1 == i) {
            return null;
        } else {
            int j = host.indexOf('.', i + 1);
            if (-1 == j) { // skip tld
                return null;
            }

            return host.substring(i + 1);
        }
    }
    
    private class HostCacheCleaner implements Runnable
    {
        public void run()
        {
            HostCache.this.cleanCache(false);
        }
    }

    private static class CacheRecord
    {
        private String time;
        private String categoryStr;
        private String exact;
        private String uri;

        CacheRecord(String domain, String categoryStr, boolean exact,
                    String uri)
        {
            this.time = Long.toString(System.currentTimeMillis());
            this.categoryStr = categoryStr;
            this.exact = exact ? "1" : "0";
            this.uri = uri;
        }

        private CacheRecord(String time, String categoryStr, String exact,
                            String uri)
        {
            this.time = time;
            this.categoryStr = categoryStr;
            this.exact = exact;
            this.uri = uri;
        }

        static CacheRecord readRecord(String s)
        {
            String[] split = s.split(" ", 4);
            if (split.length != 4) {
                Logger.getLogger(CacheRecord.class).warn("bad record: " + s);
                return null;
            } else {
                return new CacheRecord(split[0], split[1], split[2], split[3]);
            }
        }

        private void writeRecord(StringBuilder sb) {
            sb.append(time).append(" ").append(categoryStr).append(" ")
                .append(exact).append(" ").append(uri);
        }

        public long getTime()
        {
            try {
                return Long.parseLong(this.time);
            } catch (NumberFormatException exn) {
                return 0L;
            }
        }

        public String getUri()
        {
            return uri;
        }

        public String getCategoryString()
        {
            return categoryStr;
        }

        public boolean isExact()
        {
            return "1".equals(exact);
        }

        @Override
        public String toString()
        {
            return "CacheRecord time: " + time + " categoryStr: " + categoryStr
                + " exact: " + exact + " uri: " + uri;
        }
    }
}