/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.metavize.mvvm.InstallProgress;
import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.MackageException;
import com.metavize.mvvm.MackageInstallException;
import com.metavize.mvvm.MackageUninstallException;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.Period;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.UpgradeSettings;
import com.metavize.mvvm.security.Tid;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

class ToolboxManagerImpl implements ToolboxManager
{
    static final URL TOOLBOX_URL;

    private static final String MKG_CMD
        = System.getProperty("bunnicula.home") + "/../../bin/mkg ";

    private static final Object LOCK = new Object();
    private static final MackageDesc[] MACKAGE_DESC_PROTO = new MackageDesc[0];

    private static final Logger logger = Logger
        .getLogger(ToolboxManagerImpl.class);

    private static ToolboxManagerImpl TOOLBOX_MANAGER;

    static {
        try {
            String s = "file://" + System.getProperty("bunnicula.toolbox.dir")
                + "/";
            TOOLBOX_URL = new URL(s);
        } catch (MalformedURLException exn) { /* should never happen */
            throw new RuntimeException("bad toolbox URL", exn);
        }
    }

    private final UpdateDaemon updateDaemon = new UpdateDaemon();
    private final Map<Long, AptLogTail> tails
        = new HashMap<Long, AptLogTail>();

    private volatile Map packageMap;
    private volatile MackageDesc[] available;
    private volatile MackageDesc[] installed;
    private volatile MackageDesc[] uninstalled;
    private volatile MackageDesc[] upgradable;
    private volatile MackageDesc[] upToDate;

    private long lastTailKey = System.currentTimeMillis();

    private ToolboxManagerImpl()
    {
        UpgradeSettings us = getUpgradeSettings();

        refreshLists();

        updateDaemon.reschedule(us);
    }

    static ToolboxManagerImpl toolboxManager()
    {
        synchronized (LOCK) {
            if (null == TOOLBOX_MANAGER) {
                TOOLBOX_MANAGER = new ToolboxManagerImpl();
            }
        }
        return TOOLBOX_MANAGER;
    }

    // XXX permissions
    public void destroy()
    {
        logger.info("ToolboxManager destroyed");
        updateDaemon.destroy();
    }

    // ToolboxManager implementation ------------------------------------------
    // all known mackages
    public MackageDesc[] available()
    {
        MackageDesc[] available = this.available;
        MackageDesc[] retVal = new MackageDesc[available.length];
        System.arraycopy(available, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public MackageDesc[] installed()
    {
        MackageDesc[] installed = this.installed;
        MackageDesc[] retVal = new MackageDesc[installed.length];
        System.arraycopy(installed, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public MackageDesc[] uninstalled()
    {
        MackageDesc[] uninstalled = this.uninstalled;
        MackageDesc[] retVal = new MackageDesc[uninstalled.length];
        System.arraycopy(uninstalled, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public MackageDesc[] upgradable()
    {
        MackageDesc[] upgradable = this.upgradable;
        MackageDesc[] retVal = new MackageDesc[upgradable.length];
        System.arraycopy(upgradable, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public MackageDesc[] upToDate()
    {
        MackageDesc[] upToDate = this.upToDate;
        MackageDesc[] retVal = new MackageDesc[upToDate.length];
        System.arraycopy(upToDate, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public MackageDesc mackageDesc(String name)
    {
        return (MackageDesc)packageMap.get(name);
    }

    public List<InstallProgress> getProgress(long key)
    {
        AptLogTail alt;
        synchronized (tails) {
            alt = tails.get(key);
        }

        if (null == alt) {
            throw new RuntimeException("no such key: " + key);
        }

        List<InstallProgress> l = alt.getEvents();
        if (alt.isDead()) {
            synchronized (tails) {
                tails.remove(key);
            }
        }

        return l;
    }

    public long install(String name) throws MackageInstallException
    {
        AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        new Thread(alt).start();

        try {
            execMkg("install " + name, alt.getKey());
        } catch (MackageException exn) {
            throw new MackageInstallException(exn);
        }

        return alt.getKey();
    }

    public void uninstall(String name) throws MackageUninstallException
    {
        try {
            execMkg("remove " + name);
        } catch (MackageException exn) {
            throw new MackageUninstallException(exn);
        }

    }

    public void update() throws MackageException
    {
        execMkg("update");
    }

    public long upgrade() throws MackageException
    {
        AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        new Thread(alt).start();

        try {
            execMkg("upgrade");
        } catch (MackageException exn) {
            throw new MackageInstallException(exn);
        }

        return alt.getKey();
    }

    // ToolboxManagerPriv implementation --------------------------------------

    // registers a mackage and restarts all instances in previous state
    public void register(String pkgName) throws MackageInstallException
    {
        // XXX protect this method
        logger.debug("registering mackage: " + pkgName);

        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        tm.restartUnloaded();
    }

    // unregisters a mackage and unloads all instances
    public void unregister(String pkgName)
    {
        // XXX protect this method
        // stop mackage intances
        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        Tid[] tids = tm.transformInstances(pkgName);
        logger.debug("unloading " + tids.length + " transforms");
        for (int i = 0; i < tids.length; i++) {
            tm.unload(tids[i]); // XXX not destroy, release
        }
    }

    public void setUpgradeSettings(UpgradeSettings us)
    {
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(us);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not save UpgradeSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn); // XXX TransExn
            }
        }

        updateDaemon.reschedule(us);
    }

    public UpgradeSettings getUpgradeSettings()
    {
        UpgradeSettings us = null;

        net.sf.hibernate.Session s = MvvmContextFactory.context()
            .openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from UpgradeSettings us");
            us = (UpgradeSettings)q.uniqueResult();

            if (null == us) {
                logger.info("creating new UpgradeSettings");
                // pick a random time.
                Random rand = new Random();
                Period period = new Period(rand.nextInt(5), rand.nextInt(60), true);
                us = new UpgradeSettings(period);
                s.save(us);
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get UpgradeSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        return us;
    }

    // package private methods ------------------------------------------------

    URL[] resources(String tranName)
    {
        try {
            return new URL[]
                { new URL(TOOLBOX_URL, tranName + ".mar") };
        } catch (MalformedURLException exn) {
            logger.warn(exn); /* should never happen */
            return null;
        }
    }

    // private classes --------------------------------------------------------


    // private methods --------------------------------------------------------

    private byte[] orgIconForSys(String sysName)
    {
        return imageBytes("com/metavize/gui/system/" + sysName
                          + "/IconOrg42x42.png");
    }

    private byte[] descIconForSys(String sysName)
    {
        return imageBytes("com/metavize/gui/system/" + sysName
                          + "/IconDesc42x42.png");
    }

    private byte[] orgIconForTransform(String tranName)
    {
        int dashIndex = tranName.indexOf('-');
        if(dashIndex == -1) {
            return imageBytes("com/metavize/tran/" + tranName
                              + "/gui/IconOrg42x42.png");
        } else {
            return imageBytes("com/metavize/tran/"
                              + tranName.substring(0, dashIndex)
                              + "/gui/IconOrg42x42.png");
        }
    }

    private byte[] descIconForTransform(String tranName)
    {
        int dashIndex = tranName.indexOf('-');
        if(dashIndex == -1) {
            return imageBytes("com/metavize/tran/" + tranName
                              + "/gui/IconDesc42x42.png");
        } else {
            return imageBytes("com/metavize/tran/"
                              + tranName.substring(0, dashIndex)
                              + "/gui/IconDesc42x42.png");
        }
    }

    private byte[] imageBytes(String filename)
    {
        byte[] buffer = new byte[2048];

        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(filename);

        if (is == null) {
            logger.warn("Resource not found: " + filename);
            return null;
        }

        byte[] imageBytes = null;
        for (int i = 0; ; i++) {
            int c;
            try {
                c = is.read();
            } catch (IOException exn) {
                logger.warn("could not read icon", exn);
                break;
            }
            if (-1 == c) {
                imageBytes = new byte[i];
                System.arraycopy(buffer, 0, imageBytes, 0, i);
                break;
            }
            if (buffer.length <= i) {
                byte[] newBuffer = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, i);
                buffer = newBuffer;
            }
            buffer[i] = (byte)c;
        }

        try {
            is.close();
        } catch (IOException exn) {
            logger.warn("could not close icon file", exn);
        }

        return imageBytes;
    }

    // package list functions -------------------------------------------------

    private void refreshLists()
    {
        packageMap = parsePkgs();

        List availList = new ArrayList(packageMap.size());
        List instList = new ArrayList(packageMap.size());
        List uninstList = new ArrayList(packageMap.size());
        List curList = new ArrayList(packageMap.size());
        List upList = new ArrayList(packageMap.size());

        for (Iterator i = packageMap.values().iterator(); i.hasNext(); ) {
            MackageDesc md = (MackageDesc)i.next();

            availList.add(md);

            if (null == md.getInstalledVersion()) {
                uninstList.add(md);
            } else {
                instList.add(md);

                String instVer = md.getInstalledVersion();
                String availVer = md.getAvailableVersion();
                if (instVer.equals(availVer)) {
                    curList.add(md);
                } else {
                    upList.add(md);
                }
            }
        }

        available = (MackageDesc[])availList.toArray(MACKAGE_DESC_PROTO);
        installed = (MackageDesc[])instList.toArray(MACKAGE_DESC_PROTO);
        uninstalled = (MackageDesc[])uninstList.toArray(MACKAGE_DESC_PROTO);
        upgradable = (MackageDesc[])upList.toArray(MACKAGE_DESC_PROTO);
        upToDate = (MackageDesc[])curList.toArray(MACKAGE_DESC_PROTO);
    }

    // XXX we need to hold a lock while updating

    private Map parsePkgs()
    {
        Map instList = parseInstalled();
        Map pkgs = parseAvailable(instList);

        return pkgs;
    }

    private Map parseAvailable(Map instList)
    {
        Map pkgs;

        try {
            Process p = Runtime.getRuntime().exec("mkg available");
            pkgs = readPkgList(p.getInputStream(), instList);
        } catch (IOException exn) {
            throw new RuntimeException(exn); // XXX
        }

        return pkgs;
    }

    private Map readPkgList(InputStream is, Map instList) throws IOException
    {
        Map pkgs = new HashMap();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        Map m = new HashMap();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        String line;
        while (null != (line = br.readLine())) {
            if (line.trim().equals("")) {
                if (m.size() == 0) {
                    if (null == line) {
                        break;
                    } else {
                        continue;
                    }
                }

                if (key.length() > 0) {
                    m.put(key.toString(), value.toString());
                    key.delete(0, key.length());
                    value.delete(0, value.length());
                }

                if (!m.containsKey("package")) { continue; }

                // end of package
                byte[] orgIcon, descIcon;

                String name = (String)m.get("package");
                boolean isTransform = name.endsWith("-transform");

                if (isTransform) {
                    orgIcon = orgIconForTransform(name);
                    descIcon = descIconForTransform(name);
                } else {
                    orgIcon = orgIconForSys(name);
                    descIcon = descIconForSys(name);
                }

                MackageDesc md = new MackageDesc(m, (String)instList.get(name),
                                                 orgIcon, descIcon);

                logger.debug("Added available mackage: " + name);
                pkgs.put(name, md);

                m.clear();
            } else if (line.startsWith(" ") || line.startsWith("\t")) {
                if (line.charAt(1) == '.') {
                    value.append('\n');
                } else {
                    value.append(" ").append(line.trim());
                }
            } else {
                if (key.length() > 0) {
                    String k = key.toString();
                    m.put(key.toString(), value.toString());
                    key.delete(0, key.length());
                    value.delete(0, value.length());
                }
                int cidx = line.indexOf(':');
                key.append(line.substring(0, cidx).trim().toLowerCase());
                value.append(line.substring(cidx + 1).trim());
                // hack for short/long descriptions
                if (key.toString().equals("description")) {
                    value.append('\n');
                }
            }
        }
        is.close();

        return pkgs;
    }

    private Map parseInstalled()
    {
        Map instList;

        try {
            Process p = Runtime.getRuntime().exec("mkg installed");
            instList = readInstalledList(p.getInputStream());
        } catch (IOException exn) {
            throw new RuntimeException(exn); // XXX
        }

        return instList;
    }

    private Map readInstalledList(InputStream is) throws IOException
    {
        Map m = new HashMap();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while (null != (line = br.readLine())) {
            StringTokenizer tok = new StringTokenizer(line);

            String pkg = tok.nextToken();
            String ver = tok.nextToken();
            m.put(pkg, ver);
        }
        is.close();

        return m;
    }

    private synchronized void execMkg(String command, long key)
        throws MackageException
    {
        Exception exn;

        String cmdStr = MKG_CMD + (0 > key ? "" : "-k " + key + " ") + command;

        logger.debug("running: " + cmdStr);
        try {
            Process proc = Runtime.getRuntime().exec(cmdStr);
            InputStream is = proc.getInputStream();
            byte[] outBuf = new byte[4092];
            int i;
            while (-1 != (i = is.read(outBuf))) {
                System.out.write(outBuf, 0, i);
            }
            is.close();
            TRY_AGAIN:
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                break TRY_AGAIN;
            }
            logger.debug("apt done.");
            int e = proc.exitValue();
            if (0 != e) {
                throw new MackageException("apt exited with: " + e);
            }
        } catch (IOException e) {
            exn = new MackageException(e);
        }

        refreshLists();
    }

    private void execMkg(String command) throws MackageException
    {
        execMkg(command, -1);
    }

    private class UpdateDaemon
    {
        private final Timer timer = new Timer();

        private TimerTask task;

        private UpgradeSettings upgradeSettings;

        public synchronized void reschedule(UpgradeSettings upgradeSettings)
        {
            this.upgradeSettings = upgradeSettings;
            reschedule();
        }

        public void destroy()
        {
            task.cancel();
        }

        private synchronized void reschedule()
        {
            logger.debug("scheduling update");

            if (null != task) {
                task.cancel();
            }

            Calendar next = upgradeSettings.getPeriod().nextTime();
            if (null == next) { return; } /* never */

            logger.debug("scheduling timer for: " + next);

            task = new TimerTask() {
                    public void run()
                    {
                        logger.debug("doing automatic update");
                        try {
                            update();
                        } catch (MackageException exn) {
                            logger.warn("could not update", exn);
                        }

                        if (upgradeSettings.getAutoUpgrade()) {
                            logger.debug("doing automatic upgrade");
                            try {
                                upgrade();
                            } catch (MackageException exn) {
                                logger.warn("could not upgrade", exn);
                            }
                        }
                        reschedule();
                    }
                };

            timer.schedule(task, next.getTime());
        }
    }
}
