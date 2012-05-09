/**
 * $Id: ToolboxManagerImpl.java,v 1.00 2011/08/02 15:01:17 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

import com.untangle.uvm.CronJob;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.Period;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.toolbox.Application;
import com.untangle.uvm.toolbox.InstallAndInstantiateComplete;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.toolbox.PackageException;
import com.untangle.uvm.toolbox.PackageInstallException;
import com.untangle.uvm.toolbox.PackageInstallRequest;
import com.untangle.uvm.toolbox.PackageUninstallException;
import com.untangle.uvm.toolbox.PackageUninstallRequest;
import com.untangle.uvm.toolbox.RackView;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.toolbox.UpgradeSettings;
import com.untangle.uvm.toolbox.UpgradeStatus;

/**
 * Implements ToolboxManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class ToolboxManagerImpl implements ToolboxManager
{
    static final URL TOOLBOX_URL;

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(getClass());

    private static ToolboxManagerImpl TOOLBOX_MANAGER;

    /* Prints out true if the upgrade server is available */
    private static final String UPGRADE_SERVER_AVAILABLE = System.getProperty("uvm.bin.dir") + "/ut-upgrade-avail";

    static {
        try {
            String s = "file://" + System.getProperty("uvm.toolbox.dir") + "/";
            TOOLBOX_URL = new URL(s);
        } catch (MalformedURLException exn) { 
            /* should never happen */
            throw new RuntimeException("bad toolbox URL", exn);
        }
    }

    private CronJob cronJob;
    private final UpdateTask updateTask = new UpdateTask();
    private final Map<Long, AptLogTail> tails = new HashMap<Long, AptLogTail>();

    private volatile Map<String, PackageDesc> packageMap;
    private volatile PackageDesc[] available;
    private volatile PackageDesc[] installed;
    private volatile PackageDesc[] uninstalled;
    private volatile PackageDesc[] upgradable;
    private volatile PackageDesc[] upToDate;

    private volatile boolean updating = false;
    private volatile boolean upgrading = false;
    private volatile boolean installing = false;
    private volatile boolean removing = false;

    protected static ExecManager execManager = null;
    
    private long lastTailKey = System.currentTimeMillis();

    private ToolboxManagerImpl()
    {
        if (this.execManager == null)
            this.execManager = UvmContextFactory.context().createExecManager();

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

    void start()
    {
        UpgradeSettings us = getUpgradeSettings();
        Period p = us.getPeriod();

        cronJob = UvmContextFactory.context().makeCronJob(p, updateTask);
    }

    void destroy()
    {
        logger.info("ToolboxManager destroyed");
        if (cronJob != null) {
            cronJob.cancel();
        }
    }

    // ToolboxManager implementation ------------------------------------

    public RackView getRackView(Long policyId)
    {
        PackageDesc[] available = this.available;
        PackageDesc[] installed = this.installed;

        Map<String, PackageDesc> nodes = new HashMap<String, PackageDesc>();
        Map<String, PackageDesc> trials = new HashMap<String, PackageDesc>();
        Map<String, PackageDesc> libitems = new HashMap<String, PackageDesc>();
        Set<String> displayNames = new HashSet<String>();
        Set<String> hiddenApps = new HashSet<String>();
        for (PackageDesc md : available) {
            String dn = md.getDisplayName();
            PackageDesc.Type type = md.getType();
            if (type == PackageDesc.Type.LIB_ITEM) {
                displayNames.add(dn);
                libitems.put(dn, md);
                String hiddenConditions = md.getHide();
                if (hiddenConditions != null) {
                    if ( hiddenConditions.contains("true") ) {
                        hiddenApps.add(dn);
                    } else {
                        for ( String str : hiddenConditions.split(",") ) {
                            if (isInstalled(str)) {
                                hiddenApps.add(dn);
                            }
                        }
                    }
                }
            } else if (type == PackageDesc.Type.TRIAL) {
                // Workaround for Trial display names. better solution
                // is welcome.
                String realDn=dn.replaceFirst(" [0-9]+.Day Trial","");
                realDn=realDn.replaceFirst(" Limited Trial","");
                displayNames.add(realDn);
                trials.put(realDn, md);
            }
        }

        for (PackageDesc md : installed) {
            String dn = md.getDisplayName();
            PackageDesc.Type type = md.getType();

            if (type == PackageDesc.Type.LIB_ITEM) {
                libitems.remove(dn);
                trials.remove(dn);
                hiddenApps.remove(dn);
            } else if (type == PackageDesc.Type.TRIAL) {
                // Workaround for Trial display names. better solution is welcome.
                String realDn=dn.replaceFirst(" [0-9]+.Day Trial","");
                realDn=realDn.replaceFirst(" Limited Trial","");
                trials.remove(realDn);
                hiddenApps.remove(dn);
            } else if (!md.isInvisible() && (type == PackageDesc.Type.NODE || type == PackageDesc.Type.SERVICE)) {
                displayNames.add(dn);
                nodes.put(dn, md);
                hiddenApps.remove(dn);
            } 
        }

        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        List<Node> instances = nm.visibleNodes( policyId );

        Map<Long, List<NodeMetric>> nodeMetrics = new HashMap<Long, List<NodeMetric>>(instances.size());
        for (Node visibleNode : instances) {
            Long nodeId = visibleNode.getNodeSettings().getId();
            Long nodePolicyId = visibleNode.getNodeSettings().getPolicyId();
            MessageManager lmm = UvmContextFactory.context().messageManager();
            nodeMetrics.put( nodeId , visibleNode.getMetrics());

            if ( nodePolicyId == null || nodePolicyId.equals( policyId ) ) {
                nodes.remove( visibleNode.getNodeProperties().getDisplayName() );
            }
        }

        displayNames.remove(null);

        List<Application> apps = new ArrayList<Application>(displayNames.size());
        for (String dn : displayNames) {
            PackageDesc l = libitems.get(dn);
            PackageDesc t = trials.get(dn);
            PackageDesc n = nodes.get(dn);

            if (!hiddenApps.contains(dn) && ( l != null || t != null || n != null)) {
                Application a = new Application(l, t, n);
                apps.add(a);
            }
        }

        Collections.sort(apps);

        Map<String, License> licenseMap = new HashMap<String, License>();
        LicenseManager lm = UvmContextFactory.context().licenseManager();
        for (Node node : instances) {
            String n = node.getNodeProperties().getName();
            licenseMap.put(n, lm.getLicense(n));
        }
        Map<Long, NodeSettings.NodeState> runStates=nm.allNodeStates();

        List<NodeProperties> nodeProperties = new LinkedList<NodeProperties>();
        for (Node node : instances) {
            nodeProperties.add(node.getNodeProperties());
        }
        List<NodeSettings> nodeSettings  = new LinkedList<NodeSettings>();
        for (Node node : instances) {
            nodeSettings.add(node.getNodeSettings());
        }

        return new RackView(apps, nodeSettings, nodeProperties, nodeMetrics, licenseMap, runStates);
    }

    public UpgradeStatus getUpgradeStatus(boolean doUpdate) throws PackageException, InterruptedException
    {
        if(doUpdate && !upgrading && !installing) 
            update();

        boolean canupgrade = upgradable.length > 0;
        
        return new UpgradeStatus(updating, upgrading, installing, removing, canupgrade);
    }

    /**
     * Returns true if the box can reach updates.untangle.com
     */
    public boolean isUpgradeServerAvailable()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput( UPGRADE_SERVER_AVAILABLE );
            result = result.trim();
            return result.equalsIgnoreCase( "true");
        } catch ( Exception e ) {
            logger.warn( "Unable to run the script '" + UPGRADE_SERVER_AVAILABLE + "'", e );
            return false;
        }
    }
    
    // all known packages
    public PackageDesc[] available()
    {
        PackageDesc[] available = this.available;
        PackageDesc[] retVal = new PackageDesc[available.length];
        System.arraycopy(available, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public PackageDesc[] installed()
    {
        PackageDesc[] installed = this.installed;
        PackageDesc[] retVal = new PackageDesc[installed.length];
        System.arraycopy(installed, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public boolean isInstalled(String name)
    {
        String pkgName = name.trim();
        
        for (PackageDesc md : this.installed) {
            if (md.getName().equals(pkgName)) {
                return true;
            }
        }

        return false;
    }

    public PackageDesc[] installedVisible()
    {
        PackageDesc[] installed = installed();
        Vector<PackageDesc> visibleVector = new Vector<PackageDesc>();
        for( PackageDesc packageDesc : installed ){
            if( packageDesc.getViewPosition() >= 0 )
                visibleVector.add(packageDesc);
        }
        return visibleVector.toArray(new PackageDesc[0]);
    }

    public PackageDesc[] uninstalled()
    {
        PackageDesc[] uninstalled = this.uninstalled;
        PackageDesc[] retVal = new PackageDesc[uninstalled.length];
        System.arraycopy(uninstalled, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public PackageDesc[] upgradable()
    {
        PackageDesc[] upgradable = this.upgradable;
        PackageDesc[] retVal = new PackageDesc[upgradable.length];
        System.arraycopy(upgradable, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public PackageDesc[] upToDate()
    {
        PackageDesc[] upToDate = this.upToDate;
        PackageDesc[] retVal = new PackageDesc[upToDate.length];
        System.arraycopy(upToDate, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public PackageDesc packageDesc(String name)
    {
        return packageMap.get(name);
    }

    public void install(String name) throws PackageInstallException
    {
        logger.info("install(" + name + ")");

        PackageDesc req = packageDesc(name);
        if (null == req) {
            logger.warn("No such package: " + name);
        }

        /**
         * check that all versions match untangle-vm version
         */
        List<String> subnodes;
        try {
            subnodes = predictNodeInstall(name);
        }
        catch (PackageException e) {
            throw new PackageInstallException(e);
        }
        for (String node : subnodes) {
            PackageDesc pkgDesc = packageDesc(node);
            PackageDesc uvmDesc = packageDesc("untangle-vm");
            if (pkgDesc == null || uvmDesc == null) {
                logger.warn("Unable to read package desc");
                continue; //assume it matches
            } 

            String[] pkgVers = pkgDesc.getAvailableVersion().split("~");
            String[] uvmVers = uvmDesc.getInstalledVersion().split("~");

            if (pkgVers.length < 2 || uvmVers.length < 2) {
                //example 7.2.0~svnblahblah
                logger.warn("Misunderstood version strings: " + pkgDesc.getAvailableVersion() + " & " + uvmDesc.getInstalledVersion());
                continue; //assume it matches
            }
            
            String pkgVer = pkgVers[0];
            String uvmVer = uvmVers[0];
            if (pkgVer == null || uvmVer == null) {
                logger.warn("Unable to read package version: " + pkgVer + " " + uvmVer);
                continue; //assume it matches
            }

            if (!pkgVer.equals(uvmVer)) {
                logger.warn("Unable to install: " + node + " version mismatch (" + pkgVer + " != " + uvmVer + ")");
                throw new PackageInstallException("Unable to install: " + node + " version mismatch (" + pkgVer + " != " + uvmVer + ")");
            }
        }

        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i, req);
            tails.put(i, alt);
        }

        UvmContextFactory.context().newThread(alt).start();

        try {
            installing = true;
            execApt("install " + name, alt.getKey());
        } catch (PackageException exn) {
            logger.warn("install failed", exn);
            throw new PackageInstallException(exn);
        } finally {
            installing = false;
        }

        logger.info("install(" + name + ") return"); 
    }

    private final Object installAndInstantiateLock = new Object();

    public void installAndInstantiate(final String name, final Long policyId) throws PackageInstallException
    {
        logger.info("installAndInstantiate( " + name + ")");
        
        synchronized (installAndInstantiateLock) {
            UvmContextImpl uvmContext = UvmContextImpl.getInstance();
            NodeManager nm = uvmContext.nodeManager();
            List<String> subnodes = null;

            if (isInstalled(name)) {
                logger.warn("package " + name + " already installed, ignoring");
                //fix for bug #7675
                //throw new PackageInstallException("package " + name + " already installed");
                return;
            }

            /**
             * Get the list of all subnodes
             */
            try {
                subnodes = predictNodeInstall(name);
            }
            catch (PackageException e) {
                throw new PackageInstallException(e);
            }
            
            
            logger.info("SUBNODES:");
            for (String node : subnodes) 
                logger.info(node);

            /**
             * Install the package
             */
            install(name);

            /**
             * Instantiate all subnodes
             */
            for (String node : subnodes) {
                try {
                    logger.info("instantiate( " + node + ")");
                    register(node);
                    Node thisNode = nm.instantiate(node, policyId);
                    NodeProperties nd = thisNode.getNodeProperties();
                    if (thisNode != null && nd != null && nd.getAutoStart()) {
                        thisNode.start();
                    }
                } catch (DeployException exn) {
                    logger.warn("could not deploy", exn);
                } catch (PackageInstallException e) {
                    logger.warn("could not register", e);
                } catch (Exception exn) {
                    logger.warn("could not instantiate", exn);
                } 
            }

            MessageManager mm = uvmContext.messageManager();
            PackageDesc packageDesc = packageDesc(name);
            Message m = new InstallAndInstantiateComplete(packageDesc);
            mm.submitMessage(m);
        }

        logger.info("installAndInstantiate( " + name + ") return");
    }

    public void uninstall( String name ) throws PackageUninstallException
    {
        // stop intances
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        List<Node> nodeList = nm.nodeInstances(name);
        logger.debug("unloading " + nodeList.size() + " nodes");

        for (Node node : nodeList) {
            nm.unload( node ); 
        }

        try {
            removing = true;
            execApt("remove " + name);
        } catch (PackageException exn) {
            throw new PackageUninstallException(exn);
        } finally {
            removing = false;
        }

    }

    public void update() throws PackageException
    {
        int maxtries = 4;
        for (int i=0; i<maxtries; i++) {
            try {
                //timeout of 15 seconds * attempt# (15,30,45,60)
                update(15000*(i+1));
                return;
            }
            catch (PackageException e) {
                // try again with no timeout
                logger.warn("ut-apt update exception: " + e + " - trying again...");
            }
        }
    }

    public void upgrade() throws PackageException
    {
        final AptLogTail alt;

        synchronized (tails) {
            long i = ++lastTailKey;
            alt = new AptLogTail(i, null);
            tails.put(i, alt);
        }

        UvmContextFactory.context().newThread(alt).start();

        FutureTask<Object> f = new FutureTask<Object>(new Callable<Object>() {
                public Object call() throws Exception
                {
                    try {
                        upgrading = true;
                        execApt("upgrade", alt.getKey());
                    } catch (PackageException exn) {
                        logger.warn("could not upgrade", exn);
                        throw exn;
                    } finally {
                        upgrading = false;
                    }
                    return this;
                }
            });

        UvmContextFactory.context().newThread(f).start();
        
        return;
    }

    public void requestInstall(String packageName)
    {
        PackageDesc md = packageMap.get(packageName);
        if (null == md) {
            logger.warn("Could not find package for: " + packageName);
            return;
        }

        PackageInstallRequest mir = new PackageInstallRequest(md,isInstalled(packageName));
        MessageManager mm = UvmContextFactory.context().messageManager();

        // Make sure there isn't an existing outstanding install request for this package.
        for (Message msg : mm.getMessages()) {
            if (msg instanceof PackageInstallRequest) {
                PackageInstallRequest existingMir = (PackageInstallRequest)msg;
                if (existingMir.getPackageDesc() == md) {
                    logger.warn("requestInstall(" + packageName + "): ignoring request; install request already pending");
                    return;
                }
            }
        }

        logger.info("requestInstall: " + packageName);
        mm.submitMessage(mir);
    }

    public void requestUninstall(String packageName)
    {
        PackageDesc md = packageMap.get(packageName);
        if (null == md) {
            logger.warn("Could not find package for: " + packageName);
            return;
        }

        PackageUninstallRequest mir = new PackageUninstallRequest(md,isInstalled(packageName));
        MessageManager mm = UvmContextFactory.context().messageManager();

        // Make sure there isn't an existing outstanding uninstall request for this package.
        for (Message msg : mm.getMessages()) {
            if (msg instanceof PackageUninstallRequest) {
                PackageUninstallRequest existingMir = (PackageUninstallRequest)msg;
                if (existingMir.getPackageDesc() == md) {
                    logger.warn("requestUninstall(" + packageName + "): ignoring request; install request already pending");
                    return;
                }
            }
        }

        logger.info("requestUninstall: " + packageName);
        mm.submitMessage(mir);
    }
    
    // registers a package and restarts all instances in previous state
    public void register(String pkgName) throws PackageInstallException
    {
        logger.debug("registering package: " + pkgName);

        UvmContextImpl uvmContext = UvmContextImpl.getInstance();
        uvmContext.refreshToolbox();

        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        nm.restart(pkgName);
        nm.startAutoStart(packageDesc(pkgName));
    }

    // unregisters a package and unloads all instances
    public void unregister(String pkgName)
    {
        logger.debug("unregistering package: " + pkgName);

        // stop package intances
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        List<Node> nodeList = nm.nodeInstances( pkgName );
        logger.debug("unloading " + nodeList.size() + " nodes");
        for (Node node : nodeList) {
            nm.unload( node ); 
        }
    }

    public void setUpgradeSettings(final UpgradeSettings us)
    {
//         TransactionWork<Object> tw = new TransactionWork<Object>()
//             {
//                 public boolean doWork(Session s)
//                 {
//                     s.merge(us);
//                     return true;
//                 }

//                 public Object getResult() { return null; }
//             };
//         UvmContextFactory.context().runTransaction(tw);

        cronJob.reschedule(us.getPeriod());
    }

    public UpgradeSettings getUpgradeSettings()
    {
        Random rand = new Random();
        Period period = new Period(23, rand.nextInt(60), true);
        UpgradeSettings us = new UpgradeSettings(period);
        us.setAutoUpgrade(true);
        return us;

        //         TransactionWork<UpgradeSettings> tw
//             = new TransactionWork<UpgradeSettings>()
//             {
//                 private UpgradeSettings us;

//                 public boolean doWork(org.hibernate.Session s)
//                 {
//                     Query q = s.createQuery("from UpgradeSettings us");
//                     us = (UpgradeSettings)q.uniqueResult();

//                     if (null == us) {
//                         logger.info("creating new UpgradeSettings");
//                         // pick a random time.
//                         Random rand = new Random();
//                         Period period = new Period(23, rand.nextInt(60), true);
//                         us = new UpgradeSettings(period);
//                         us.setAutoUpgrade(true);
//                         s.save(us);
//                     }
//                     return true;
//                 }

//                 public UpgradeSettings getResult() { return us; }
//             };
//         UvmContextFactory.context().runTransaction(tw);

//         return tw.getResult();
    }

    protected List<PackageDesc> getInstalledAndAutoStart()
    {
        List<PackageDesc> mds = new ArrayList<PackageDesc>();

        for (PackageDesc md : installed()) {
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
            } catch (PackageException exn) {
                logger.warn("could not update", exn);
            }

            if (getUpgradeSettings().getAutoUpgrade()) {
                logger.debug("doing automatic upgrade");
                try {
                    upgrade();
                } catch (PackageException exn) {
                    logger.warn("could not upgrade", exn);
                }
            }
        }
    }

    // package list functions -------------------------------------------------

    private void update(long millis) throws PackageException
    {
        FutureTask<Object> f = new FutureTask<Object>(new Callable<Object>()
            {
                public Object call() throws Exception
                {
                    updating = true;
                    execApt("update");
                    updating = false;

                    return this;
                }
            });

        UvmContextFactory.context().newThread(f).start();

        boolean tryAgain;
        do {
            tryAgain = false;
            try {
                f.get(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exn) {
                tryAgain = true;
            } catch (ExecutionException exn) {
                Throwable t = exn.getCause();
                if (t instanceof PackageException) {
                    throw (PackageException)t;
                } else {
                    throw new RuntimeException(t);
                }
            } catch (TimeoutException exn) {
                f.cancel(true);
                logger.warn("ut-apt timeout: ", exn);
                throw new PackageException("ut-apt timed out");
            }
        } while (tryAgain);
    }

    private void refreshLists()
    {
        packageMap = parsePkgs();

        List<PackageDesc> availList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> instList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> uninstList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> curList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> upList = new ArrayList<PackageDesc>(packageMap.size());

        for (PackageDesc md : packageMap.values()) {
            availList.add(md);

            if (null == md.getInstalledVersion()) {
                uninstList.add(md);
            } else {
                instList.add(md);


                if (PackageDesc.Type.LIB_ITEM == md.getType()) {
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

        available = availList.toArray(new PackageDesc[availList.size()]);
        installed = instList.toArray(new PackageDesc[instList.size()]);
        uninstalled = uninstList.toArray(new PackageDesc[uninstList.size()]);
        upgradable = upList.toArray(new PackageDesc[upList.size()]);
        upToDate = curList.toArray(new PackageDesc[curList.size()]);
    }

    private Map<String, PackageDesc> parsePkgs()
    {
        Map<String, String> instList = parseInstalled();
        Map<String, PackageDesc> pkgs = parseAvailable(instList);

        return pkgs;
    }

    private Map<String, PackageDesc> parseAvailable(Map<String, String> instList)
    {
        Map<String, PackageDesc> pkgs;

        synchronized(this) {
            try {
                String cmd = System.getProperty("uvm.bin.dir") + "/ut-apt available";
                String availableList = this.execManager.execOutput(cmd);
                pkgs = readPkgList(availableList, instList);
            } catch (Exception exn) {
                logger.fatal("Unable to parse ut-apt available list, proceeding with empty list", exn);
                return new HashMap<String, PackageDesc>();
            }
        }

        return pkgs;
    }

    private Map<String, PackageDesc> readPkgList(String availableList, Map<String, String> instList) throws IOException
    {
        Map<String, PackageDesc> pkgs = new HashMap<String, PackageDesc>();

        Map<String, String> m = new HashMap<String, String>();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        List<String> hidePkgs = new LinkedList<String>();
        if (System.getProperty("uvm.hidden.libitems") != null) {
            String[] libitems = System.getProperty("uvm.hidden.libitems").split(",");
            hidePkgs = Arrays.asList(libitems);
        }

        for (String line : availableList.split("\\n", -1)) {
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

                PackageDesc md = new PackageDesc(m, instList.get(name));
                if (null == md.getType()) {
                    continue;
                }

                if (hidePkgs.contains(name)) {
                    logger.info("Hiding package: " + name);
                }
                else {
                    logger.debug("Added available package: " + name);
                    pkgs.put(name, md);
                }

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

        return pkgs;
    }

    private Map<String, String> parseInstalled()
    {
        Map<String, String> instList;

        synchronized(this) {
            try {
                String cmd = System.getProperty("uvm.bin.dir") + "/ut-apt installed";
                String list = this.execManager.execOutput(cmd);
                instList = readInstalledList(list);
            } catch (IOException exn) {
                throw new RuntimeException(exn); 
            }
        }

        return instList;
    }

    private Map<String, String> readInstalledList(String list) throws IOException
    {
        Map<String, String> m = new HashMap<String,String>();
        for (String line: list.split("\\n", -1)) {
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

        return m;
    }

    private synchronized void execApt(String command, long key) throws PackageException
    {
        String cmdStr = System.getProperty("uvm.bin.dir") + "/ut-apt " + (0 > key ? "" : "-k " + key + " ") + command;

        synchronized(this) {
            Integer exitCode = this.execManager.execResult(cmdStr);
            if (exitCode != 0) {
                throw new PackageException("ut-apt " + command + " exited with: " + exitCode);
            }
        }

        refreshLists();
    }

    private void execApt(String command) throws PackageException
    {
        execApt(command, -1);
    }

    /**
     * Returns a list of packages that will be installed as a result of installing this node
     */
    private List<String> predictNodeInstall(String pkg) throws PackageException
    {
        logger.info("predictNodeInstall(" + pkg + ")");
        
        List<String> l = new ArrayList<String>();
        String cmd = System.getProperty("uvm.bin.dir") + "/ut-apt predictInstall " + pkg;

        synchronized(this) {
            ExecManagerResult result = this.execManager.exec(cmd);

            for (String line : result.getOutput().split("\\n", -1)) {
                PackageDesc md = packageMap.get(line);
                if (md == null) {
                    logger.debug("Ignoring non-package: " + line);
                    continue;
                }
                PackageDesc.Type mdType = md.getType();
                if (mdType != PackageDesc.Type.NODE && mdType != PackageDesc.Type.SERVICE) {
                    logger.debug("Ignoring non-node/service package: " + line);
                    continue;
                }
                l.add(line);
            }

            /**
             * If returns non-zero throw an exception
             */
            if (result.getResult() != 0) {
                throw new PackageException("ut-apt predictInstall exited with: " + result.getResult());
            }
        }

        return l;
    }
}
