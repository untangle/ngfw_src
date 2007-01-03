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

package com.untangle.mvvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.untangle.mvvm.CronJob;
import com.untangle.mvvm.MessageQueue;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.Period;
import com.untangle.mvvm.security.LoginSession;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.TransformBase;
import com.untangle.mvvm.toolbox.InstallProgress;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.toolbox.MackageException;
import com.untangle.mvvm.toolbox.MackageInstallException;
import com.untangle.mvvm.toolbox.MackageInstallRequest;
import com.untangle.mvvm.toolbox.MackageUninstallException;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.mvvm.toolbox.ToolboxMessage;
import com.untangle.mvvm.toolbox.UpgradeSettings;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class ToolboxManagerImpl implements ToolboxManager
{
    static final URL TOOLBOX_URL;

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(getClass());

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

    private final Map<String, MackageState> mackageState;
    private final CronJob cronJob;
    private final UpdateTask updateTask = new UpdateTask();
    private final Map<Long, AptLogTail> tails = new HashMap<Long, AptLogTail>();
    private final Map<LoginSession, MessageQueueImpl<ToolboxMessage>> messageQueues
        = new WeakHashMap<LoginSession, MessageQueueImpl<ToolboxMessage>>();

    private volatile Map<String, MackageDesc> packageMap;
    private volatile MackageDesc[] available;
    private volatile MackageDesc[] installed;
    private volatile MackageDesc[] uninstalled;
    private volatile MackageDesc[] upgradable;
    private volatile MackageDesc[] upToDate;

    private long lastTailKey = System.currentTimeMillis();

    private ToolboxManagerImpl()
    {
        mackageState = loadMackageState();

        UpgradeSettings us = getUpgradeSettings();
        Period p = us.getPeriod();

        cronJob = MvvmContextFactory.context().makeCronJob(p, updateTask);

        refreshLists();
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

    void destroy()
    {
        logger.info("ToolboxManager destroyed");
        cronJob.cancel();
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

    public MackageDesc[] installedVisible()
    {
        MackageDesc[] installed = installed();
    Vector<MackageDesc> visibleVector = new Vector<MackageDesc>();
    for( MackageDesc mackageDesc : installed ){
        if( mackageDesc.getViewPosition() >= 0 )
        visibleVector.add(mackageDesc);
    }
        return visibleVector.toArray(new MackageDesc[0]);
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
        return packageMap.get(name);
    }

    public List<InstallProgress> getProgress(long key)
    {
        logger.debug("getProgress(" + key + ")");
        AptLogTail alt;
        logger.debug("getting alt");
        synchronized (tails) {
            alt = tails.get(key);
        }
        logger.debug("got alt");

        if (null == alt) {
            logger.warn("no such progress key: " + key);
            throw new RuntimeException("no such key: " + key);
        }

        logger.debug("getting events");
        List<InstallProgress> l = alt.getEvents();
        logger.debug("seeing if isDead");
        if (alt.isDead()) {
            synchronized (tails) {
                logger.debug("removing dead alt");
                tails.remove(key);
            }
        }

        return l;
    }

    public long install(final String name) throws MackageInstallException
    {
        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        MvvmContextFactory.context().newThread(alt).start();

        MvvmContextFactory.context().newThread(new Runnable() {
                public void run()
                {
                    try {
                        execMkg("install " + name, alt.getKey());
                    } catch (MackageException exn) {
                        logger.warn("install failed", exn);
                    }
                }
            }).start();

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
        update(120000);
    }

    public void update(long millis) throws MackageException
    {
        FutureTask f = new FutureTask(new Callable()
            {
                public Object call() throws Exception
                {
                    execMkg("update");

                    return this;
                }
            });

        MvvmContextFactory.context().newThread(f).start();

        TRY_AGAIN:
        try {
            f.get(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exn) {
            break TRY_AGAIN;
        } catch (ExecutionException exn) {
            Throwable t = exn.getCause();
            if (t instanceof MackageException) {
                throw (MackageException)t;
            } else {
                throw new RuntimeException(t);
            }
        } catch (TimeoutException exn) {
            f.cancel(true);
            throw new MackageException("mkg timed out");
        }
    }

    public long upgrade() throws MackageException
    {
        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        MvvmContextFactory.context().newThread(alt).start();

        MvvmContextFactory.context().newThread(new Runnable() {
                public void run()
                {
                    try {
                        execMkg("upgrade", alt.getKey());
                    } catch (MackageException exn) {
                        logger.warn("could not upgrade", exn);
                    }
                }
            }).start();

        return alt.getKey();
    }

    public void enable(String mackageName) throws MackageException
    {
        MackageState ms = mackageState.get(mackageName);
        if (null == ms) {
            ms = new MackageState(mackageName, null, true);
            mackageState.put(mackageName, ms);
        } else {
            ms.setEnabled(true);
        }

        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        for (Tid tid : tm.transformInstances(mackageName)) {
            TransformContext tctx = tm.transformContext(tid);
            try {
                ((TransformBase)tctx.transform()).enable();
            } catch (TransformException exn) {
                logger.warn("could not enable: " + tid, exn);
            }
        }

        syncMackageState(ms);
    }

    public void disable(String mackageName) throws MackageException
    {
        MackageState ms = mackageState.get(mackageName);
        if (null == ms) {
            ms = new MackageState(mackageName, null, false);
            mackageState.put(mackageName, ms);
        } else {
            ms.setEnabled(false);
        }

        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        for (Tid tid : tm.transformInstances(mackageName)) {
            TransformContext tctx = tm.transformContext(tid);
            try {
                ((TransformBase)tctx.transform()).disable();
            } catch (TransformException exn) {
                logger.warn("could not disable: " + tid, exn);
            }
        }

        syncMackageState(ms);
    }

    public void extraName(String mackageName, String extraName)
    {
        MackageState ms = mackageState.get(mackageName);
        if (null == ms) {
            ms = new MackageState(mackageName, extraName, true);
            mackageState.put(mackageName, ms);
        } else {
            ms.setExtraName(extraName);
        }

        syncMackageState(ms);

        refreshLists();
    }

    public void requestInstall(String mackageName)
    {
        synchronized (messageQueues) {
            MackageInstallRequest mir = new MackageInstallRequest(mackageName);
            for (MessageQueueImpl<ToolboxMessage> mq : messageQueues.values()) {
                mq.enqueue(mir);
            }
        }
    }

    public MessageQueue<ToolboxMessage> subscribe()
    {
        LoginSession s = HttpInvoker.invoker().getActiveLogin();

        MessageQueueImpl mq;
         synchronized (messageQueues) {
             mq = messageQueues.get(s);
             if (null == mq) {
                 mq = new MessageQueueImpl<ToolboxMessage>();
                 messageQueues.put(s, mq);
             }
         }

         return mq;
    }

    // ToolboxManagerPriv implementation --------------------------------------

    // registers a mackage and restarts all instances in previous state
    public void register(String pkgName) throws MackageInstallException
    {
        // XXX protect this method
        logger.debug("registering mackage: " + pkgName);

        MvvmContextImpl mctx = MvvmContextImpl.getInstance();
        if (mctx.refreshToolbox()) {
            mctx.refreshSessionFactory();
        }

        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        tm.restart(pkgName);
    }

    // unregisters a mackage and unloads all instances
    public void unregister(String pkgName)
    {
        // XXX protect this method
        // stop mackage intances
        TransformManagerImpl tm = (TransformManagerImpl)MvvmContextFactory
            .context().transformManager();
        List<Tid> tids = tm.transformInstances(pkgName);
        logger.debug("unloading " + tids.size() + " transforms");
        for (Tid t : tids) {
            tm.unload(t); // XXX not destroy, release
        }
    }

    public void setUpgradeSettings(final UpgradeSettings us)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(us);
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        cronJob.reschedule(us.getPeriod());
    }

    public UpgradeSettings getUpgradeSettings()
    {
        TransactionWork<UpgradeSettings> tw = new TransactionWork<UpgradeSettings>()
            {
                private UpgradeSettings us;

                public boolean doWork(org.hibernate.Session s)
                {
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
                    return true;
                }

                public UpgradeSettings getResult() { return us; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return tw.getResult();
    }

    // package private methods ------------------------------------------------

    URL getResourceDir(String tranName)
    {
        try {
            return new URL(TOOLBOX_URL, tranName + "-impl/");
        } catch (MalformedURLException exn) {
            logger.warn(exn); /* should never happen */
            return null;
        }
    }

    boolean isEnabled(String mackageName)
    {
        MackageState ms = mackageState.get(mackageName);
        return null == ms ? true : ms.isEnabled();
    }

    // private classes --------------------------------------------------------

    private class UpdateTask implements Runnable
    {
        public void run()
        {
            logger.debug("doing automatic update");
            try {
                update();
            } catch (MackageException exn) {
                logger.warn("could not update", exn);
            }

            if (getUpgradeSettings().getAutoUpgrade()) {
                logger.debug("doing automatic upgrade");
                try {
                    upgrade();
                } catch (MackageException exn) {
                    logger.warn("could not upgrade", exn);
                }
            }
        }
    }

    // private methods --------------------------------------------------------

    private Map<String, MackageState> loadMackageState()
    {
        final Map<String, MackageState> m = new HashMap<String, MackageState>();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(org.hibernate.Session s)
                {
                    Query q = s.createQuery("from MackageState ms");
                    for (MackageState ms : (List<MackageState>)q.list()) {
                        m.put(ms.getMackageName(), ms);
                    }
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return m;
    }

    private void syncMackageState(final MackageState ms)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(org.hibernate.Session s)
                {
                    s.merge(ms);
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);
    }

    // package list functions -------------------------------------------------

    private void refreshLists()
    {
        packageMap = parsePkgs();

        List<MackageDesc> availList = new ArrayList<MackageDesc>(packageMap.size());
        List<MackageDesc> instList = new ArrayList<MackageDesc>(packageMap.size());
        List<MackageDesc> uninstList = new ArrayList<MackageDesc>(packageMap.size());
        List<MackageDesc> curList = new ArrayList<MackageDesc>(packageMap.size());
        List<MackageDesc> upList = new ArrayList<MackageDesc>(packageMap.size());

        for (MackageDesc md : packageMap.values()) {
            availList.add(md);

            if (null == md.getInstalledVersion()) {
                uninstList.add(md);
            } else {
                instList.add(md);

                if (md.getName().endsWith("-storeitem")) {
                    // store items always up to date
                    curList.add(md);
                } else {
                    String instVer = md.getInstalledVersion();
                    String availVer = md.getAvailableVersion();
                    if (instVer.equals(availVer)) {
                        curList.add(md);
                    } else {
                        upList.add(md);
                    }
                }
            }
        }

        available = availList.toArray(new MackageDesc[availList.size()]);
        installed = instList.toArray(new MackageDesc[instList.size()]);
        uninstalled = uninstList.toArray(new MackageDesc[uninstList.size()]);
        upgradable = upList.toArray(new MackageDesc[upList.size()]);
        upToDate = curList.toArray(new MackageDesc[curList.size()]);
    }

    // XXX we need to hold a lock while updating

    private Map<String, MackageDesc> parsePkgs()
    {
        Map<String, String> instList = parseInstalled();
        Map<String, MackageDesc> pkgs = parseAvailable(instList);

        return pkgs;
    }

    private Map<String, MackageDesc> parseAvailable(Map<String, String> instList)
    {
        Map<String, MackageDesc> pkgs;

        try {
            Process p = MvvmContextFactory.context().exec("mkg available");
            pkgs = readPkgList(p.getInputStream(), instList);
        } catch (Exception exn) {
            logger.fatal("Unable to parse mkg available list, proceeding with empty list", exn);
            return new HashMap<String, MackageDesc>();
        }

        return pkgs;
    }

    private Map<String, MackageDesc> readPkgList(InputStream is,
                                                 Map<String, String> instList)
        throws IOException
    {
        Map<String, MackageDesc> pkgs = new HashMap<String, MackageDesc>();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        Map<String, String> m = new HashMap<String, String>();
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


                String name = m.get("package");
                boolean isTransform = name.endsWith("-transform");

                MackageState mState = mackageState.get(name);
                String en = null == mState ? null : mState.getExtraName();
                MackageDesc md = new MackageDesc(m, instList.get(name), en);

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

    private Map<String, String> parseInstalled()
    {
        Map<String, String> instList;

        try {
            Process p = MvvmContextFactory.context().exec("mkg installed");
            instList = readInstalledList(p.getInputStream());
        } catch (IOException exn) {
            throw new RuntimeException(exn); // XXX
        }

        return instList;
    }

    private Map<String, String> readInstalledList(InputStream is) throws IOException
    {
        Map<String, String> m = new HashMap();
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

        String cmdStr = "mkg " + (0 > key ? "" : "-k " + key + " ") + command;

        logger.debug("running: " + cmdStr);
        try {
            Process proc = MvvmContextFactory.context().exec(cmdStr);
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
}
