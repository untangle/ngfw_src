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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.Period;
import com.untangle.uvm.license.LicenseStatus;
import com.untangle.uvm.license.RemoteLicenseManager;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.Application;
import com.untangle.uvm.toolbox.InstallAndInstantiateComplete;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.MackageException;
import com.untangle.uvm.toolbox.MackageInstallException;
import com.untangle.uvm.toolbox.MackageInstallRequest;
import com.untangle.uvm.toolbox.MackageUninstallException;
import com.untangle.uvm.toolbox.RackView;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.toolbox.RemoteUpstreamManager;
import com.untangle.uvm.toolbox.UpgradeSettings;
import com.untangle.uvm.toolbox.UpgradeStatus;
import com.untangle.uvm.toolbox.UpstreamService;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.NodeBase;

/**
 * Implements RemoteToolboxManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemoteToolboxManagerImpl implements RemoteToolboxManager
{
    private static final int UPDATE_TIMEOUT = 40000;

    static final URL TOOLBOX_URL;

    private static final String DEFAULT_LIBRARY_HOST = "library.untangle.com";

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(getClass());

    private static RemoteToolboxManagerImpl TOOLBOX_MANAGER;

    
    /* Prints out true if the upgrade server is available */
    private static final String UPGRADE_SERVER_AVAILABLE = System.getProperty( "bunnicula.home" ) + 
        "/bin/upgrade-server-available";

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

    private volatile Map<String, MackageDesc> packageMap;
    private volatile MackageDesc[] available;
    private volatile MackageDesc[] installed;
    private volatile MackageDesc[] uninstalled;
    private volatile MackageDesc[] upgradable;
    private volatile MackageDesc[] upToDate;

    private volatile boolean updating = false;
    private volatile boolean upgrading = false;
    private volatile boolean installing = false;
    private volatile boolean removing = false;

    private long lastTailKey = System.currentTimeMillis();

    private List<String> beingInstalled = new ArrayList<String>();

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
        if (cronJob != null) {
            cronJob.cancel();
        }
    }

    // RemoteToolboxManager implementation ------------------------------------

    public RackView getRackView(Policy p)
    {
        return getRackView(p,null);
    }

    public RackView getRackView(Policy p, String installationType)
    {
        if (installationType == null) {
            installationType = LocalUvmContextFactory.context()
                .installationType();
        }

        if (installationType == null || installationType.length()==0) {
            installationType="!nevermatchflag,,!";
        }

        MackageDesc[] available = this.available;
        MackageDesc[] installed = this.installed;

        Map<String, MackageDesc> nodes = new HashMap<String, MackageDesc>();
        Map<String, MackageDesc> trials = new HashMap<String, MackageDesc>();
        Map<String, MackageDesc> libitems = new HashMap<String, MackageDesc>();
        Set<String> displayNames = new HashSet<String>();
        Set<String> hiddenApps = new HashSet<String>();
        for (MackageDesc md : available) {
            String dn = md.getDisplayName();
            MackageDesc.Type type = md.getType();
            if (type == MackageDesc.Type.LIB_ITEM) {
                displayNames.add(dn);
                libitems.put(dn, md);
                if (md.getHideOn().contains(installationType)) {
                    hiddenApps.add(dn);
                }
            } else if (type == MackageDesc.Type.TRIAL) {
                // Workaround for Trial display names. better solution
                // is welcome.
                String realDn=dn.replaceFirst(" [0-9]+.Day Trial","");
                realDn=realDn.replaceFirst(" Limited Trial","");
                displayNames.add(realDn);
                trials.put(realDn, md);
            }
        }

        for (MackageDesc md : installed) {
            String dn = md.getDisplayName();
            MackageDesc.Type type = md.getType();
            if (type == MackageDesc.Type.LIB_ITEM) {
                libitems.remove(dn);
                trials.remove(dn);
                hiddenApps.remove(dn);
            } else if (type == MackageDesc.Type.TRIAL) {
                // Workaround for Trial display names. better solution
                // is welcome.
                String realDn=dn.replaceFirst(" [0-9]+.Day Trial","");
                realDn=realDn.replaceFirst(" Limited Trial","");
                trials.remove(realDn);
                hiddenApps.remove(dn);
            } else if (!md.isInvisible()
                       && (type == MackageDesc.Type.NODE
                           || type == MackageDesc.Type.SERVICE)) {
                displayNames.add(dn);
                nodes.put(dn, md);
                hiddenApps.remove(dn);
            }
        }

        NodeManagerImpl tm = (NodeManagerImpl)LocalUvmContextFactory
            .context().nodeManager();
        List<NodeDesc> instances = tm.visibleNodes(p);

        Map<Tid, StatDescs> statDescs
            = new HashMap<Tid, StatDescs>(instances.size());
        for (NodeDesc nd : instances) {
            Tid t = nd.getTid();
            LocalMessageManager lmm = LocalUvmContextFactory.context()
                .localMessageManager();
            Counters c = lmm.getCounters(t);
            StatDescs sd = c.getStatDescs();
            statDescs.put(t, sd);

            Policy tp = t.getPolicy();
            if (tp == null || tp.equals(p)) {
                nodes.remove(nd.getDisplayName());
            }
        }

        displayNames.remove(null);

        List<Application> apps
            = new ArrayList<Application>(displayNames.size());
        for (String dn : displayNames) {
            MackageDesc l = libitems.get(dn);
            MackageDesc t = trials.get(dn);
            MackageDesc n = nodes.get(dn);

            if (!hiddenApps.contains(dn)
                && ( l != null || t != null || n != null)) {
                Application a = new Application(l, t, n);
                apps.add(a);
            }
        }

        Collections.sort(apps);

        Map<String, LicenseStatus> licenseStatus
            = new HashMap<String, LicenseStatus>();
        RemoteLicenseManager lm = LocalUvmContextFactory.context()
            .remoteLicenseManager();
        for (NodeDesc nd : instances) {
            String n = nd.getMackageDesc().getName();
            licenseStatus.put(n, lm.getMackageStatus(n));
        }
        Map<Tid, NodeState> runStates=tm.allNodeStates();
        return new RackView(apps, instances, statDescs, licenseStatus,
                            runStates);
    }

    public UpgradeStatus getUpgradeStatus(boolean doUpdate)
        throws MackageException, InterruptedException
    {
        if(doUpdate && !upgrading && !installing) {
            if(!updating) {
                update();
            } else {
                int maxTries=UPDATE_TIMEOUT/1000;
                while(updating && maxTries-- > 0) {
                    Thread.sleep(1000);
                }
            }
        }
        return new UpgradeStatus(updating, upgrading, installing, removing,
                                 upgradable.length > 0);
    }

    /**
     * Returns true if the box can reach updates.untangle.com
     */
    public boolean isUpgradeServerAvailable()
    {
        try {
            String result = ScriptRunner.getInstance().exec( UPGRADE_SERVER_AVAILABLE );
            result = result.trim();
            return result.equalsIgnoreCase( "true");
        } catch ( Exception e ) {
            logger.warn( "Unable to run the script '" + UPGRADE_SERVER_AVAILABLE + "'", e );
            return false;
        }
    }
    
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
        for (MackageDesc md : this.installed) {
            if (md.getName().equals(name)) {
                return true;
            }
        }

        return false;
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

    public void install(String name)
    {
        install(name, true);
    }

    private final Object installAndInstantiateLock = new Object();

    public void installAndInstantiate(final String name, final Policy p)
        throws MackageInstallException
    {
        Runnable r = new Runnable()
            {
                public void run()
                {
                    synchronized (installAndInstantiateLock) {
                        doIt();
                    }
                }

                // Now keeps a record of which nodes are being
                // installed (globally), and which ones are being
                // installed by this run.
                public void doIt()
                {
                    UvmContextImpl mctx = UvmContextImpl.getInstance();
                    NodeManagerImpl tm = mctx.nodeManager();
                    List<String> nodes = null;

                    // List of mackages that we will try to install in
                    // this run
                    List<String> willInstall = new ArrayList<String>();

                    try {
                        if (isInstalled(name)) {
                            logger.warn("mackage " + name
                                        + " already installed, debouncing");
                            return;
                        }
                        synchronized (beingInstalled) {
                            if (beingInstalled.contains(name)) {
                                logger.warn("mackage " + name
                                            + " being installed, debouncing");
                                return;
                            }
                            beingInstalled.add(name);
                            willInstall.add(name);
                            nodes = predictNodeInstall(name);
                            for (Iterator<String> iter = nodes.iterator();
                                 iter.hasNext();) {
                                String newNode = iter.next();
                                if (beingInstalled.contains(newNode)) {
                                    logger.warn("mackage " + newNode
                                                + " being subinstalled, debouncing");
                                    // Let the other guy install it.
                                    iter.remove();
                                } else {
                                    beingInstalled.add(newNode);
                                    willInstall.add(newNode);
                                }
                            }
                        }

                        install(name, false);
                        for (String nn : nodes) {
                            try {
                                register(nn);
                                NodeDesc nd = tm.instantiate(nn, p);
                                if (nd != null && !nd.getNoStart()) {
                                    NodeContext nc = tm
                                        .nodeContext(nd.getTid());
                                    nc.node().start();
                                }
                            } catch (NodeStartException exn) {
                                // XXX send out error message
                                logger.warn("could not start", exn);
                            } catch (DeployException exn) {
                                // XXX send out error message
                                logger.warn("could not deploy", exn);
                            } catch (MackageInstallException e) {
                                // TODO Auto-generated catch block
                                logger.warn("could not register", e);
                            }
                        }
                    } finally {
                        synchronized (beingInstalled) {
                            for (String newNode : willInstall)
                                beingInstalled.remove(newNode);
                        }
                    }

                    LocalMessageManager mm = mctx.localMessageManager();
                    MackageDesc mackageDesc = mackageDesc(name);
                    Message m = new InstallAndInstantiateComplete(mackageDesc);
                    mm.submitMessage(m);
                }
            };
        LocalUvmContextFactory.context().newThread(r).start();
    }

    public void uninstall(String name) throws MackageUninstallException
    {
        try {
            removing = true;
            execMkg("remove " + name);
        } catch (MackageException exn) {
            throw new MackageUninstallException(exn);
        } finally {
            removing = false;
        }

    }

    public void update() throws MackageException
    {
        update(UPDATE_TIMEOUT);
    }

    public void update(long millis) throws MackageException
    {
        FutureTask f = new FutureTask(new Callable()
            {
                public Object call() throws Exception
                {
                    updating = true;
                    execMkg("update");
                    updating = false;

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
            alt = new AptLogTail(i, null);
            tails.put(i, alt);
        }

        LocalUvmContextFactory.context().newThread(alt).start();

        LocalUvmContextFactory.context().newThread(new Runnable() {
                public void run()
                {
                    try {
                        upgrading = true;
                        execMkg("upgrade", alt.getKey());
                    } catch (MackageException exn) {
                        logger.warn("could not upgrade", exn);
                    } finally {
                        upgrading = false;
                    }
                }
            }).start();

        return alt.getKey();
    }

    public void enable(String mackageName) throws MackageException
    {
        MackageState ms = mackageState.get(mackageName);
        if (null == ms) {
            ms = new MackageState(mackageName, true);
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
            ms = new MackageState(mackageName, false);
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

    public void requestInstall(String mackageName)
    {
        MackageDesc md = packageMap.get(mackageName);
        if (null == md) {
            logger.warn("Could not find package for: " + mackageName);
            return;
        }

        MackageInstallRequest mir
            = new MackageInstallRequest(md,isInstalled(mackageName));
        LocalMessageManager mm = LocalUvmContextFactory.context()
            .localMessageManager();

        // Make sure there isn't an existing outstanding install
        // request for this mackage.
        for (Message msg : mm.getMessages()) {
            if (msg instanceof MackageInstallRequest) {
                MackageInstallRequest existingMir = (MackageInstallRequest)msg;
                if (existingMir.getMackageDesc() == md) {
                    logger.warn("requestInstall(" + mackageName
                                + "): ignoring request; install request already pending");
                    return;
                }
            }
        }

        logger.info("requestInstall: " + mackageName);
        mm.submitMessage(mir);
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
        TransactionWork<UpgradeSettings> tw
            = new TransactionWork<UpgradeSettings>()
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
                            LocalUvmContextFactory.context().upstreamManager()
                            .getService(RemoteUpstreamManager.AUTO_UPGRADE_SERVICE_NAME);
                        if (upgradeSvc != null) {
                            us.setAutoUpgrade(upgradeSvc.enabled());
                        }
                        s.save(us);
                    }
                    return true;
                }

                public UpgradeSettings getResult() { return us; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        return tw.getResult();
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

    public String getLibraryHost()
    {
        String host = System.getProperty("uvm.store.host");
        if (host == null) host = DEFAULT_LIBRARY_HOST;
        return host;
    }

    // package private methods ------------------------------------------------

    private void install(final String name, boolean async)
    {
        MackageDesc req = mackageDesc(name);
        if (null == req) {
            logger.warn("No such mackage: " + name);
        }

        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i, req);
            tails.put(i, alt);
        }

        LocalUvmContextFactory.context().newThread(alt).start();

        Runnable r = new Runnable() {
                public void run()
                {
                    try {
                        installing = true;
                        execMkg("install " + name, alt.getKey());
                    } catch (MackageException exn) {
                        logger.warn("install failed", exn);
                    } finally {
                        installing = false;
                    }
                }
            };

        if (async) {
            LocalUvmContextFactory.context().newThread(r).start();
        } else {
            r.run();
        }
    }

    URL getResourceDir(MackageDesc md)
    {
        try {
            return new URL(TOOLBOX_URL, md.getName() + "-impl/");
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
            if (line.startsWith("#")) {
                continue;
            }

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

                MackageDesc md = new MackageDesc(m, instList.get(name));
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
                if (0 > cidx) {
                    logger.warn("bad line (no colon): " + line);
                    continue;
                }
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

    private Map<String, String> readInstalledList(InputStream is)
        throws IOException
    {
        Map<String, String> m = new HashMap();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while (null != (line = br.readLine())) {
            StringTokenizer tok = new StringTokenizer(line);

            /* line is a Blank line */
            if ( !tok.hasMoreElements()) {
                continue;
            }
            
            String pkg = tok.nextToken();
            
            if ( !tok.hasMoreElements()) {
                logger.warn("Ignoring package with missing version string '" + pkg + "'");
                continue;
            }
            
            String ver = tok.nextToken();
            
            m.put(pkg, ver);
        }
        is.close();

        return m;
    }

    private synchronized void execMkg(String command, long key)
        throws MackageException
    {
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
            logger.info( "exception while in mackage: ", e);
        }

        refreshLists();
    }

    private void execMkg(String command) throws MackageException
    {
        execMkg(command, -1);
    }

    private List<String> predictNodeInstall(String mkg)
    {
        List<String> l = new ArrayList<String>();

        String cmd = System.getProperty("bunnicula.bin.dir")
            + "/mkg predictInstall " + mkg;
        try {
            Process p = LocalUvmContextFactory.context().exec(cmd);
            InputStream is = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (null != (line = br.readLine())) {
                MackageDesc md = packageMap.get(line);
                if (md == null) {
                    logger.debug("Ignoring non-mackage: " + line);
                    continue;
                }
                MackageDesc.Type mdType = md.getType();
                if (mdType != MackageDesc.Type.NODE
                    && mdType != MackageDesc.Type.SERVICE) {
                    logger.debug("Ignoring non-node/service mackage: " + line);
                    continue;
                }
                l.add(line);
            }
        } catch (IOException exn) {
            logger.warn("could not predict node install: " + mkg, exn);
        }

        return l;
    }
}
