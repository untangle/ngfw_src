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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * Holds a list of hostname, data keypairs backed by
 * BerkeleyDB. Intended as a data store for lists provided by servers
 * implementing the Phishing Protection Server Spec
 * (http://wiki.mozilla.org/Phishing_Protection:_Server_Spec).
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class UrlList
{
    private static final byte[] VERSION_KEY = "__version".getBytes();
    private static final byte[] LAST_UPDATE_KEY = "__last_update".getBytes();

    private static final long UPDATE_FORCE_INTERVAL = 604800000L; // 1 week

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[[^ ]+ ([0-9.]+)( update)?\\]");

    private static final Set<String> DB_LOCKS = new HashSet<String>();

    private final URL baseUrl;
    private final String dbName;
    private final Database db;
    private final String dbLock;
    private final File initFile;

    private final String suffix;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors ----------------------------------------------------------

    public UrlList(File dbHome, URL baseUrl, String dbName,
                   Map<String, String> extraParams,
                   File initFile)
        throws DatabaseException
    {
        this.baseUrl = baseUrl;
        this.dbName = dbName;
        this.initFile = initFile;

        dbLock = new File(dbHome, dbName).getAbsolutePath().intern();

        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);

        int tries = 0;
        Environment dbEnv = null;
        synchronized (DB_LOCKS) {
            while (null == dbEnv && 3 > tries++) {
                try {
                    dbEnv = new Environment(dbHome, envCfg);
                } catch (DatabaseException exn) {
                    logger.warn("couldn't load environment, try: " + tries, exn);
                    for (File f : dbHome.listFiles()) {
                        boolean deleted = f.delete();
                    }
                }
            }
        }

        // Open the database. Create it if it does not already exist.
        DatabaseConfig dbCfg = new DatabaseConfig();
        dbCfg.setAllowCreate(true);
        db = dbEnv.openDatabase(null, dbName, dbCfg);

        StringBuilder sb = new StringBuilder();
        if (null != extraParams) {
            for (String k : extraParams.keySet()) {
                sb.append("&");
                sb.append(k);
                sb.append("=");
                sb.append(extraParams.get(k));
            }
        }

        suffix = sb.toString();
    }

    // public methods --------------------------------------------------------

    public void update(boolean async) {
        if (async) {
            Thread t = new Thread(new Runnable() {
                    public void run()
                    {
                        doUpdate(false);
                    }
                }, "update-" + dbLock);
            t.setDaemon(true);
            t.start();
        } else {
            doUpdate(false);
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

    // protected methods -----------------------------------------------------

    protected abstract boolean updateDatabase(Database db, BufferedReader br)
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

    protected void del(StringBuilder sb, byte[] prefix)
    {
        String pStr = new String(prefix);

        int i = sb.indexOf(pStr);
        int j;

        if (0 == i) {
            if (sb.length() == pStr.length()) {
                j = pStr.length();
            } else {
                j = pStr.length() + 1;
                if ('\t' != sb.charAt(j - 1)) {
                    logger.warn("tab expected at char " + j + " in: " + sb);
                    i = j = 0;
                }
            }
        } else if (0 < i) {
            i--;
            if ('\t' != sb.charAt(i)) {
                logger.warn("tab expected at char " + i + " in: " + sb);
                i = j = 0;
            }
            j = i + pStr.length() + 1;
        } else {
            i = j = 0;
        }

        sb.delete(i, j);
    }

    protected void add(StringBuilder sb, byte[] prefix)
    {
        String pStr = new String(prefix);

        if (0 == sb.length()) {
            sb.append(pStr);
        } else if (0 > sb.indexOf(pStr)) {
            sb.append('\t');
            sb.append(pStr);
        }
    }

    // private methods -------------------------------------------------------

    private List<String> getPatterns(String hostStr)
    {
        byte[] host = hostStr.getBytes();

        byte[] hash = getKey(host);

        byte[] hippie = new byte[hash.length + 1];
        System.arraycopy(hash, 0, hippie, 1, hash.length);

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

    private String getAttr(byte[] key, Database db)
    {
        DatabaseEntry k = new DatabaseEntry(key);
        DatabaseEntry v = new DatabaseEntry();
        try {
            OperationStatus s = db.get(null, k, v, LockMode.READ_UNCOMMITTED);
            if (OperationStatus.SUCCESS == s) {
                return new String(v.getData());
            } else {
                return null;
            }
        } catch (DatabaseException exn) {
            return null;
        }
    }

    private void setAttr(byte[] key, Database db, String value)
    {
        if (null != value) {
            try {
                DatabaseEntry k = new DatabaseEntry(key);
                DatabaseEntry v = new DatabaseEntry(value.getBytes());
                db.put(null, k, v);
            } catch (DatabaseException exn) {
                logger.warn("could not set " + key, exn);
            }
        }
    }

    private String getVersion(Database db)
    {
        return getAttr(VERSION_KEY, db);
    }

    private void setVersion(Database db, String version)
    {
        setAttr(VERSION_KEY, db, version);
    }

    private long getLastUpdate(Database db)
    {
        String s = getAttr(LAST_UPDATE_KEY, db);
        if (null == s) {
            return 0;
        } else {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException exn) {
                logger.warn("bad time: " + s, exn);
                return 0;
            }
        }
    }

    private void setLastUpdate(Database db, long time)
    {
        setAttr(LAST_UPDATE_KEY, db, Long.toString(time));
    }


    private void doUpdate(boolean updatedOnce)
    {
        boolean fileInit = false;

        synchronized (DB_LOCKS) {
            if (DB_LOCKS.contains(dbLock)) {
                return;
            } else {
                DB_LOCKS.add(dbLock);
            }
        }

        synchronized (DB_LOCKS) {
            BufferedReader br = null;
            try {
                String oldVersion = getVersion(db);

                InputStream is = null;

                if (null == oldVersion && null != initFile && initFile.exists()) {
                    logger.info(dbName + " initilizing from file: " + initFile);
                    try {
                        is = new FileInputStream(initFile);
                        fileInit = true;
                    } catch (FileNotFoundException exn) {
                        is = null;
                    }
                }

                if (null == is) {
                    if (System.currentTimeMillis() - getLastUpdate(db) > UPDATE_FORCE_INTERVAL) {
                        logger.info(dbName + " old database, forcing reinitialization");
                        oldVersion = "1:1";
                    }

                    String v = null == oldVersion ? "1:1" : oldVersion.replace(".", ":");
                    URL url = new URL(baseUrl + "/update?version=" + dbName + ":" + v + suffix);
                    logger.info(dbName + " updating from URL: " + url);

                    HttpClient hc = new HttpClient();
                    HttpMethod get = new GetMethod(url.toString());
                    int rc = hc.executeMethod(get);
                    is = get.getResponseBodyAsStream();
                }

                InputStreamReader isr = new InputStreamReader(is);
                br = new BufferedReader(isr);

                String line = br.readLine();
                if (null == line) {
                    logger.info(dbName + " no update");
                    return;
                }

                String newVersion;
                boolean update;
                Matcher matcher = VERSION_PATTERN.matcher(line);
                if (matcher.find()) {
                    newVersion = matcher.group(1);
                    update = null != matcher.group(2);
                } else {
                    logger.info("no update");
                    return;
                }

                logger.info("updating: " + dbName + " from: " + oldVersion
                            + " to: " + newVersion);

                if (!update) {
                    logger.warn("clearing database: " + dbName);
                    clearDatabase();
                }

                boolean done = updateDatabase(db, br);

                setVersion(db, done ? newVersion : "1.1");
                setLastUpdate(db, System.currentTimeMillis());

                db.getEnvironment().sync();

                logger.info(dbName + " number entries: " + db.count());
            } catch (DatabaseException exn) {
                logger.warn("could not update database: " + dbName, exn);
            } catch (IOException exn) {
                logger.warn("could not update database: " + dbName, exn);
            } finally {
                DB_LOCKS.remove(dbLock);
            }
        }

        if (!updatedOnce && fileInit) {
            doUpdate(true);
        }
    }

    private void clearDatabase()
    {
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
    }
}
