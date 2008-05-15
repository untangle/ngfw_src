/*
 * $HeadURL$
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

package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
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

import com.untangle.uvm.CronJob;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.Period;
import com.untangle.uvm.alerts.MessageQueue;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.security.LoginSession;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.DownloadComplete;
import com.untangle.uvm.toolbox.DownloadProgress;
import com.untangle.uvm.toolbox.DownloadSummary;
import com.untangle.uvm.toolbox.InstallComplete;
import com.untangle.uvm.toolbox.InstallProgress;
import com.untangle.uvm.toolbox.InstallTimeout;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.MackageException;
import com.untangle.uvm.toolbox.MackageInstallException;
import com.untangle.uvm.toolbox.MackageInstallRequest;
import com.untangle.uvm.toolbox.MackageUninstallException;
import com.untangle.uvm.toolbox.MackageUpdateExtraName;
import com.untangle.uvm.toolbox.ProgressVisitor;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.toolbox.RemoteUpstreamManager;
import com.untangle.uvm.toolbox.ToolboxMessage;
import com.untangle.uvm.toolbox.UpgradeSettings;
import com.untangle.uvm.toolbox.UpstreamService;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.NodeBase;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Implements RemoteToolboxManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemoteToolboxManagerImpl implements RemoteToolboxManager
{
    static final URL TOOLBOX_URL;

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(getClass());

    private static RemoteToolboxManagerImpl TOOLBOX_MANAGER;

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
    private CronJob cronJob;
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

    private RemoteToolboxManagerImpl()
    {
        mackageState = loadMackageState();

        refreshLists();
    }

    static RemoteToolboxManagerImpl toolboxManager()
    {
        synchronized (LOCK) {
            if (null == TOOLBOX_MANAGER) {
                TOOLBOX_MANAGER = new RemoteToolboxManagerImpl();
            }
        }
        return TOOLBOX_MANAGER;
    }

    void start()
    {
        UpgradeSettings us = getUpgradeSettings();
        Period p = us.getPeriod();

        cronJob = LocalUvmContextFactory.context().makeCronJob(p, updateTask);
    }

    void destroy()
    {
        logger.info("RemoteToolboxManager destroyed");
        if (cronJob != null)
            cronJob.cancel();
    }

    // RemoteToolboxManager implementation ------------------------------------

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

    public boolean isInstalled(String name)
    {
        return null != packageMap.get(name);
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

    public long install(final String name)
    {
        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        LocalUvmContextFactory.context().newThread(alt).start();

        LocalUvmContextFactory.context().newThread(new Runnable() {
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

    public void installSynchronously(String name) throws MackageInstallException
    {
        long k = install(name);

        InstallVisitor v = new InstallVisitor();
        while (!v.isDone()) {
            for (InstallProgress p : getProgress(k)) {
                p.accept(v);
            }
        }
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

        LocalUvmContextFactory.context().newThread(f).start();

        boolean tryAgain;
        do {
            tryAgain = false;
            try {
                f.get(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exn) {
                tryAgain = true;
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
        } while (tryAgain);
    }

    public long upgrade() throws MackageException
    {
        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i);
            tails.put(i, alt);
        }

        LocalUvmContextFactory.context().newThread(alt).start();

        LocalUvmContextFactory.context().newThread(new Runnable() {
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

        NodeManagerImpl tm = (NodeManagerImpl)LocalUvmContextFactory
            .context().nodeManager();
        for (Tid tid : tm.nodeInstances(mackageName)) {
            NodeContext tctx = tm.nodeContext(tid);
            try {
                ((NodeBase)tctx.node()).enable();
            } catch (NodeException exn) {
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

        NodeManagerImpl tm = (NodeManagerImpl)LocalUvmContextFactory
            .context().nodeManager();
        for (Tid tid : tm.nodeInstances(mackageName)) {
            NodeContext tctx = tm.nodeContext(tid);
            try {
                ((NodeBase)tctx.node()).disable();
            } catch (NodeException exn) {
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

        MackageUpdateExtraName mue = new MackageUpdateExtraName(mackageName,extraName);

        synchronized (messageQueues) {
            logger.info("updateExtraName: " + mackageName + " new extraname: " + extraName);
            for (MessageQueueImpl<ToolboxMessage> mq : messageQueues.values()) {
                mq.enqueue(mue);
            }
        }
    }

    public void requestInstall(String mackageName)
    {
        MackageDesc md = packageMap.get(mackageName);
        if (null == md) {
            logger.warn("Could not find package for: " + mackageName);
            return;
        }
        MackageInstallRequest mir = new MackageInstallRequest(md);

        synchronized (messageQueues) {
            logger.info("requestInstall: " + mackageName);
            for (MessageQueueImpl<ToolboxMessage> mq : messageQueues.values()) {
                mq.enqueue(mir);
            }
        }
    }

    public MessageQueue<ToolboxMessage> subscribe()
    {
        LoginSession s = HttpInvokerImpl.invoker().getActiveLogin();

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

    // RemoteToolboxManagerPriv implementation --------------------------------

    // registers a mackage and restarts all instances in previous state
    public void register(String pkgName) throws MackageInstallException
    {
        // XXX protect this method
        logger.debug("registering mackage: " + pkgName);

        UvmContextImpl mctx = UvmContextImpl.getInstance();
        if (mctx.refreshToolbox()) {
            mctx.refreshSessionFactory();
        }

        NodeManagerImpl tm = (NodeManagerImpl)LocalUvmContextFactory
            .context().nodeManager();
        tm.restart(pkgName);
        tm.startAutoStart(mackageDesc(pkgName));
    }

    // unregisters a mackage and unloads all instances
    public void unregister(String pkgName)
    {
        // XXX protect this method
        // stop mackage intances
        NodeManagerImpl tm = (NodeManagerImpl)LocalUvmContextFactory
            .context().nodeManager();
        List<Tid> tids = tm.nodeInstances(pkgName);
        logger.debug("unloading " + tids.size() + " nodes");
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
        LocalUvmContextFactory.context().runTransaction(tw);

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
                        Period period = new Period(23, rand.nextInt(60), true);
                        us = new UpgradeSettings(period);
			// only turn on auto-upgrade for full ISO install
                        UpstreamService upgradeSvc =
                            LocalUvmContextFactory.context().upstreamManager().getService(RemoteUpstreamManager.AUTO_UPGRADE_SERVICE_NAME);
                        if (upgradeSvc != null)
                            us.setAutoUpgrade(upgradeSvc.enabled());
                        s.save(us);
                    }
                    return true;
                }

                public UpgradeSettings getResult() { return us; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        return tw.getResult();
    }

    public List<String> getWebstartResources()
    {
        List<String> resources = new ArrayList<String>();

        String d = System.getProperty("bunnicula.web.dir") + "/webstart";
        for (File f : new File(d).listFiles()) {
            String s = f.getName();
            if (s.endsWith(".jar")) {
                resources.add(s);
            }
        }

        return resources;
    }

    public boolean hasPremiumSubscription()
    {
        for (MackageDesc md : this.installed) {
            if (md.getName().equals("untangle-libitem-update-service")) {
                return true;
            }
        }

        return false;
    }

    // package private methods ------------------------------------------------

    URL getResourceDir(MackageDesc md)
    {
        try {
            return new URL(TOOLBOX_URL, md.getJarPrefix() + "-impl/");
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

    List<MackageDesc> getInstalledAndAutoStart()
    {
        List<MackageDesc> mds = new ArrayList<MackageDesc>();

        for (MackageDesc md : installed()) {
            if (md.isAutoStart()) {
                mds.add(md);
            }
        }

        return mds;
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

    private static class InstallVisitor implements ProgressVisitor
    {
        private final Logger logger = Logger.getLogger(getClass());

        private boolean done = false;

        // ProgressVisitor methods --------------------------------------------

        public void visitDownloadSummary(DownloadSummary ds) { }

        public void visitDownloadProgress(DownloadProgress dp) { }

        public void visitDownloadComplete(DownloadComplete dc) { }

        public void visitInstallComplete(InstallComplete ic)
        {
            logger.info("install complete, success: " + ic.getSuccess());
            done = true;
        }

        public void visitInstallTimeout(InstallTimeout it)
        {
            logger.info("install timed out");
            done = true;
        }

        // package protected methods ------------------------------------------

        boolean isDone()
        {
            return done;
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
        LocalUvmContextFactory.context().runTransaction(tw);

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
        LocalUvmContextFactory.context().runTransaction(tw);
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


                if (MackageDesc.Type.LIB_ITEM == md.getType()) {
                    // lib items always up to date
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
            String cmd = System.getProperty("bunnicula.bin.dir")
                + "/mkg available";
            Process p = LocalUvmContextFactory.context().exec(cmd);
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

                MackageState mState = mackageState.get(name);
                String en = null == mState ? null : mState.getExtraName();
                MackageDesc md = new MackageDesc(m, instList.get(name), en);
                if (null == md.getType()) {
                    continue;
                }

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
            String cmd = System.getProperty("bunnicula.bin.dir")
                + "/mkg installed";
            Process p = LocalUvmContextFactory.context().exec(cmd);
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

        String cmdStr = System.getProperty("bunnicula.bin.dir") + "/mkg "
            + (0 > key ? "" : "-k " + key + " ") + command;

        logger.debug("running: " + cmdStr);
        try {
            Process proc = LocalUvmContextFactory.context().exec(cmdStr);
            InputStream is = proc.getInputStream();
            byte[] outBuf = new byte[4092];
            int i;
            while (-1 != (i = is.read(outBuf))) {
                System.out.write(outBuf, 0, i);
            }
            is.close();
            boolean tryAgain;
            do {
                tryAgain = false;
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    tryAgain = true;
                }
            } while (tryAgain);
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
