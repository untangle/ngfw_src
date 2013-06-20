/**
 * $Id: AptManagerImpl.java,v 1.00 2011/08/02 15:01:17 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.apt.Application;
import com.untangle.uvm.apt.InstallAndInstantiateComplete;
import com.untangle.uvm.apt.PackageDesc;
import com.untangle.uvm.apt.PackageInstallRequest;
import com.untangle.uvm.apt.RackView;
import com.untangle.uvm.apt.AptManager;
import com.untangle.uvm.apt.UpgradeStatus;

/**
 * Implements AptManager.
 * 
 * The public methods are documented in AptManager.java
 */
public class AptManagerImpl implements AptManager
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The global aptManager instance
     */
    private static AptManagerImpl APT_MANAGER;

    /**
     * The current Package Map.
     * This maps stores a list of all Untangle packages and their package descriptions
     */
    private volatile Map<String, PackageDesc> packageMap;

    /**
     * A list of all available package descs (packages that can be downloaded)
     */
    private volatile PackageDesc[] available;

    /**
     * A list of all installed package descs (packages that are installed on the server)
     */
    private volatile PackageDesc[] installed;

    /**
     * A list of all upgradable package descs (packages with newer versions available)
     */
    private volatile PackageDesc[] upgradable;

    /**
     * True if apt-get update is being run, false otherwise
     */
    private volatile boolean updating = false;

    /**
     * True if apt-get dist-upgrade is being run, false otherwise
     */
    private volatile boolean upgrading = false;

    /**
     * True if any apt-get install is running, false otherwise
     */
    private volatile boolean installing = false;

    /**
     * The dedicate execManager for apt
     */
    protected static ExecManager execManager = null;

    /**
     * The key to use for apt-get tracking
     */
    private long lastTailKey = System.currentTimeMillis();

    /**
     * A Map from key to the Apt Tail thread for each thread running
     */
    private final Map<Long, AptLogTail> tails = new HashMap<Long, AptLogTail>();
    
    /**
     * Private constructor to ensure singleton. use aptManager() to get singleton reference
     */
    private AptManagerImpl()
    {
        if (this.execManager == null)
            this.execManager = UvmContextFactory.context().createExecManager();

        refreshLists();
    }

    /**
     * get the singleton aptManager
     */
    protected static synchronized AptManagerImpl aptManager()
    {
        if ( APT_MANAGER == null ) {
            APT_MANAGER = new AptManagerImpl();
        }
        return APT_MANAGER;
    }

    public RackView getRackView(Long policyId)
    {
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        LicenseManager lm = UvmContextFactory.context().licenseManager();

        PackageDesc[] available = this.available;
        PackageDesc[] installed = this.installed;

        /* This stores a list of all known display names for libitems/nodes */
        Set<String> displayNames = new HashSet<String>();
        /* This stores a list of installable nodes. (for this rack) Display Name -> PackageDesc */
        Map<String, PackageDesc> installableNodes = new HashMap<String, PackageDesc>();
        /* This stores a list of installable libitems. (for this rack) Display Name -> PackageDesc */
        Map<String, PackageDesc> installableLibitems = new HashMap<String, PackageDesc>();
        /* This stores a list of all licenses */
        Map<String, License> licenseMap = new HashMap<String, License>();
        
        /**
         * First add all available libitems to installableLibitems List
         */
        for (PackageDesc md : available) {
            String dn = md.getDisplayName();
            String name = md.getName();

            if ( name.contains("-libitem-") ) {
                displayNames.add(dn);
                installableLibitems.put(dn, md);
            }
        }

        /**
         * Build the license map
         */
        List<Node> visibleNodes = nm.visibleNodes( policyId );
        for (Node node : visibleNodes) {
            String n = node.getNodeProperties().getName();
            licenseMap.put(n, lm.getLicense(n));
        }

        /**
         * Build the rack state
         */
        Map<Long, NodeSettings.NodeState> runStates=nm.allNodeStates();

        /**
         * Iterate through installed packages 
         *
         * If its a libitem, hide it from the left-hand-nav (its already downloaded)
         * If its a node, put it in displayNames nodes (it can be installed)
         */
        for (PackageDesc md : installed) {
            String dn = md.getDisplayName();
            String name = md.getName();

            if ( name.contains("-libitem-") ) {
                installableLibitems.remove(dn); /* don't show it on left hand apps pane */
            } else if ( !md.isInvisible() && ( name.contains("-casing-") || name.contains("-node-") ) ) {
                displayNames.add(dn);
                installableNodes.put(dn, md);
            } 
        }

        /**
         * Build the nodeMetrics (stats in the UI)
         * Remove visible installableNodes from installableNodes
         */
        Map<Long, List<NodeMetric>> nodeMetrics = new HashMap<Long, List<NodeMetric>>(visibleNodes.size());
        for (Node visibleNode : visibleNodes) {
            Long nodeId = visibleNode.getNodeSettings().getId();
            Long nodePolicyId = visibleNode.getNodeSettings().getPolicyId();
            MessageManager lmm = UvmContextFactory.context().messageManager();
            nodeMetrics.put( nodeId , visibleNode.getMetrics());

            if ( nodePolicyId == null || nodePolicyId.equals( policyId ) ) {
                installableNodes.remove( visibleNode.getNodeProperties().getDisplayName() );
            }
        }

        /**
         * SPECIAL CASE: If premium package is installed but its only a trial still show in apps pane
         * This is so there is a place to "buy" the package
         */
        for (PackageDesc md : installed) {
            if ("untangle-libitem-premium-package".equals(md.getName())) {
                boolean justATrial = (lm.getLicense(License.POLICY) != null && lm.getLicense(License.POLICY).getValid() && lm.getLicense(License.POLICY).getTrial());
                if ( justATrial ) 
                    installableLibitems.put(md.getDisplayName(), md); /* show it on the left hand pane */
            }
        }
        /**
         * SPECIAL CASE: If standard package is installed but its only a trial still show in apps pane
         * This is so there is a place to "buy" the package
         */
        for (PackageDesc md : installed) {
            if ("untangle-libitem-standard-package".equals(md.getName())) {
                boolean justATrial = (lm.getLicense(License.POLICY) != null && lm.getLicense(License.POLICY).getValid() && lm.getLicense(License.POLICY).getTrial());
                if ( justATrial ) 
                    installableLibitems.put(md.getDisplayName(), md); /* show it on the left hand pane */
            }
        }
        /**
         * SPECIAL CASE: If premium package is installed and licensed - hide standard package (its included)
         */
        for (PackageDesc md : installed) {
            if ("untangle-libitem-premium-package".equals(md.getName())) {
                boolean justATrial = (lm.getLicense(License.POLICY) != null && lm.getLicense(License.POLICY).getValid() && lm.getLicense(License.POLICY).getTrial());
                if ( ! justATrial ) {
                    PackageDesc stan = null;
                    for ( Iterator<PackageDesc> i = installableLibitems.values().iterator() ; i.hasNext() ; ) {
                        PackageDesc pkgDesc = i.next();
                        if ("untangle-libitem-standard-package".equals( pkgDesc.getName() ))
                            i.remove();
                    }
                }
            }
        }
        /**
         * SPECIAL CASE: If premium package or standard package is installed AND licensed - hide lite package (its included)
         */
        for (PackageDesc md : installed) {
            if ("untangle-libitem-standard-package".equals(md.getName()) || "untangle-libitem-premium-package".equals(md.getName())) {
                boolean justATrial = (lm.getLicense(License.POLICY) != null && lm.getLicense(License.POLICY).getValid() && lm.getLicense(License.POLICY).getTrial());
                if ( ! justATrial ) {
                    PackageDesc lite = null;
                    for ( Iterator<PackageDesc> i = installableLibitems.values().iterator() ; i.hasNext() ; ) {
                        PackageDesc pkgDesc = i.next();
                        if ("untangle-libitem-lite-package".equals( pkgDesc.getName() ))
                            i.remove();
                    }
                }
            }
        }

        /**
         * SPECIAL CASE: If Web Filter is installed in this rack OR licensed for non-trial, hide Web Filter Lite
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            List<Node> sitefilterNodes = UvmContextFactory.context().nodeManager().nodeInstances( "untangle-node-sitefilter", policyId );
            if (sitefilterNodes != null && sitefilterNodes.size() > 0) {
                installableLibitems.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
                installableNodes.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
            }
            License sitefilterLicense = lm.getLicense(License.SITEFILTER);
            if ( sitefilterLicense != null && sitefilterLicense.getValid() && !sitefilterLicense.getTrial() ) {
                installableLibitems.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
                installableNodes.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
            }
        }
        
        /**
         * SPECIAL CASE: If Spam Blocker is installed in this rack OR licensed for non-trial, hide Spam Blocker Lite
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            List<Node> commtouchAsNodes = UvmContextFactory.context().nodeManager().nodeInstances( "untangle-node-commtouchas", policyId);
            if (commtouchAsNodes != null && commtouchAsNodes.size() > 0) {
                installableLibitems.remove("Spam Blocker Lite"); /* hide web filter lite from left hand nav */
                installableNodes.remove("Spam Blocker Lite"); /* hide web filter lite from left hand nav */
            }
            License commtouchAsLicense = lm.getLicense(License.COMMTOUCHAS);
            if ( commtouchAsLicense != null && commtouchAsLicense.getValid() && !commtouchAsLicense.getTrial() ) {
                installableLibitems.remove("Spam Blocker Lite"); /* hide web filter lite from left hand nav */
                installableNodes.remove("Spam Blocker Lite"); /* hide web filter lite from left hand nav */
            }
        }
        
        
        /**
         * Build the list of apps to show on the left hand nav
         */
        logger.debug("Building apps panel:");
        displayNames.remove(null);
        List<Application> apps = new ArrayList<Application>(displayNames.size());
        for (String dn : displayNames) {
            PackageDesc l = installableLibitems.get(dn);
            PackageDesc n = installableNodes.get(dn);

            if ( l == null && n == null )
                continue;

            if (l != null)
                logger.debug("Adding app (libitem) : " + l.getName() + " dn: " + l.getDisplayName());
            if (n != null)
                logger.debug("Adding app (node   ) : " + n.getName() + " dn: " + n.getDisplayName());
            
            Application a = new Application(l, n);
            apps.add(a);
        }
        
        Collections.sort(apps);

        List<NodeProperties> nodeProperties = new LinkedList<NodeProperties>();
        for (Node node : visibleNodes) {
            nodeProperties.add(node.getNodeProperties());
        }
        List<NodeSettings> nodeSettings  = new LinkedList<NodeSettings>();
        for (Node node : visibleNodes) {
            nodeSettings.add(node.getNodeSettings());
        }

        return new RackView(apps, nodeSettings, nodeProperties, nodeMetrics, licenseMap, runStates);
    }

    public UpgradeStatus getUpgradeStatus(boolean doUpdate) throws Exception, InterruptedException
    {
        if(doUpdate && !upgrading && !installing) 
            update();

        boolean canupgrade = upgradable.length > 0;
        
        return new UpgradeStatus(updating, upgrading, installing, canupgrade);
    }

    public boolean isUpgradeServerAvailable()
    {
        for ( int tries = 0 ; tries < 3 ; tries++ ) {
            try {
                String host = "updates.untangle.com";
                InetAddress addr = InetAddress.getByName( host );
                InetSocketAddress remoteAddress = new InetSocketAddress(addr, 80);
                Socket sock = new Socket();
                sock.connect( remoteAddress, 5000 );
                sock.close();
                return true;
            }
            catch ( Exception e) {
                logger.warn("Failed to connect to updates.untangle.com: " + e);
            }
        }
        return false;
    }
    
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

    public boolean isInstalled( String name )
    {
        String pkgName = name.trim();
        
        for (PackageDesc md : this.installed) {
            if (md.getName().equals(pkgName)) {
                return true;
            }
        }

        return false;
    }

    public PackageDesc[] upgradable()
    {
        PackageDesc[] upgradable = this.upgradable;
        PackageDesc[] retVal = new PackageDesc[upgradable.length];
        System.arraycopy(upgradable, 0, retVal, 0, retVal.length);
        return retVal;
    }

    public PackageDesc packageDesc(String name)
    {
        return packageMap.get(name);
    }

    public void install(String name) throws Exception
    {
        logger.info("install(" + name + ")");

        PackageDesc req = packageDesc(name);
        if (null == req) {
            logger.warn("No such package: " + name);
        }

        /**
         * check that the version matches untangle-vm version
         */
        do {
            PackageDesc pkgDesc = packageDesc( name );
            PackageDesc uvmDesc = packageDesc("untangle-vm");
            if (pkgDesc == null || uvmDesc == null) {
                logger.warn("Unable to read package desc");
                break; //assume it matches
            } 
            String[] pkgVers = pkgDesc.getAvailableVersion().split("~");
            String[] uvmVers = uvmDesc.getInstalledVersion().split("~");
            if (pkgVers.length < 2 || uvmVers.length < 2) {
                //example 7.2.0~svnblahblah
                logger.warn("Misunderstood version strings: " + pkgDesc.getAvailableVersion() + " & " + uvmDesc.getInstalledVersion());
                break; //assume it matches
            }
            String pkgVer = pkgVers[0];
            String uvmVer = uvmVers[0];
            if (pkgVer == null || uvmVer == null) {
                logger.warn("Unable to read package version: " + pkgVer + " " + uvmVer);
                break; //assume it matches
            }
            if (!pkgVer.equals(uvmVer)) {
                logger.warn("Unable to install: " + name + " version mismatch (" + pkgVer + " != " + uvmVer + ")");
                throw new Exception("Unable to install: " + name + " version mismatch (" + pkgVer + " != " + uvmVer + ")");
            }
        } while ( false );


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
        } catch (Exception exn) {
            logger.warn("install failed", exn);
            throw exn;
        } finally {
            installing = false;
        }

        logger.info("install(" + name + ") return"); 
    }

    public void installAndInstantiate(final String name, final Long policyId) throws Exception
    {
        logger.info("installAndInstantiate( " + name + ")");
        
        synchronized (this) {
            UvmContextImpl uvmContext = UvmContextImpl.getInstance();
            NodeManager nm = uvmContext.nodeManager();
            List<String> subpkgs = null;

            if (isInstalled(name)) {
                logger.warn("package " + name + " already installed, ignoring");
                //fix for bug #7675
                //throw new Exception("package " + name + " already installed");
                return;
            }

            /**
             * Get the list of all subnodes
             */
            try {
                subpkgs = predictNodeInstall(name);
            }
            catch (Exception e) {
                throw e;
            }
            
            /**
             * Install the package
             */
            install(name);

            /**
             * Instantiate all subpkgs that are nodes
             */
            for (String node : subpkgs) {

                if ( ! node.contains("-node-") )
                    continue;

                try {
                    logger.info("instantiate( " + node + ")");
                    register(node);
                    Node thisNode = nm.instantiate(node, policyId);
                    NodeProperties nd = thisNode.getNodeProperties();
                    if (thisNode != null && nd != null && nd.getAutoStart()) {
                        thisNode.start();
                    }
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

    public void update() throws Exception
    {
        int maxtries = 4;
        for (int i=0; i<maxtries; i++) {
            try {
                //timeout of 15 seconds * attempt# (15,30,45,60)
                update(15000*(i+1));
                return;
            }
            catch (Exception e) {
                // try again with no timeout
                logger.warn("ut-apt update exception: " + e + " - trying again...");
            }
        }
    }

    public void upgrade() throws Exception
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
                    } catch (Exception exn) {
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

    public void register(String pkgName) throws Exception
    {
        logger.info("registering package: " + pkgName);

        UvmContextImpl uvmContext = UvmContextImpl.getInstance();
        uvmContext.refreshLibs();
        Reporting reporting = (Reporting) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
        if ( reporting != null ) {
            reporting.createSchemas();
        }
        
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        nm.startAutoStart(packageDesc(pkgName));
    }

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

    /**
     * cat apt-get update with the given timeout (millis)
     */
    private void update(long millis) throws Exception
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
                throw exn;
            } catch (TimeoutException exn) {
                f.cancel(true);
                logger.warn("ut-apt timeout.");
                throw new Exception("ut-apt timed out");
            }
        } while (tryAgain);
    }

    /**
     * Refreshes all the list of packages (available, installed, upgradable)
     */
    private void refreshLists()
    {
        packageMap = buildPackageMap();

        List<PackageDesc> availList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> instList = new ArrayList<PackageDesc>(packageMap.size());
        List<PackageDesc> upgradeableList = new ArrayList<PackageDesc>(packageMap.size());

        for (PackageDesc md : packageMap.values()) {
            availList.add(md);

            if ( md.getInstalledVersion() != null ) {
                instList.add(md);

                String instVer = md.getInstalledVersion();
                String availVer = md.getAvailableVersion();
                if ( ! instVer.equals(availVer) ) {
                    upgradeableList.add(md);
                }
            }
        }

        available = availList.toArray(new PackageDesc[availList.size()]);
        installed = instList.toArray(new PackageDesc[instList.size()]);
        upgradable = upgradeableList.toArray(new PackageDesc[upgradeableList.size()]);
    }

    /**
     * Build the packageMap
     */
    private Map<String, PackageDesc> buildPackageMap()
    {
        Map<String, String> instList = buildInstalledMap();
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

    /**
     * Builds a map of packages based on availableList
     */
    private Map<String, PackageDesc> readPkgList(String availableList, Map<String, String> instList) throws IOException
    {
        Map<String, PackageDesc> pkgs = new HashMap<String, PackageDesc>();

        Map<String, String> m = new HashMap<String, String>();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        List<String> hidePkgs = new LinkedList<String>();
        String hiddenLibitems = UvmContextFactory.context().oemManager().getHiddenLibitems();
        if (hiddenLibitems != null) {
            String[] libitems = hiddenLibitems.split(",");
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
                // if (null == md.getType()) {
                //     continue;
                // }

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

    /**
     * Build a map of installed packages to their version string
     */
    private Map<String, String> buildInstalledMap()
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

    /**
     * Build a map of installed packages to their version string based on list
     */
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

    /**
     * Exec the apt wrapper with the specified command and key.
     * The key is used to log to apt.log so progress can be tracked
     */
    private synchronized void execApt(String command, long key) throws Exception
    {
        String cmdStr = System.getProperty("uvm.bin.dir") + "/ut-apt " + (0 > key ? "" : "-k " + key + " ") + command;

        synchronized(this) {
            ExecManagerResult result = this.execManager.exec(cmdStr);
            if (result.getResult() != 0) {
                throw new Exception("ut-apt " + command + " error (" + result.getResult() + ") :" + result.getOutput());
            }
        }

        refreshLists();
    }

    /**
     * exec Apt with the specified command (with no key)
     */
    private void execApt(String command) throws Exception
    {
        execApt(command, -1);
    }

    /**
     * Returns a list of packages that will be installed as a result of installing this node
     */
    private List<String> predictNodeInstall(String pkg) throws Exception
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
                String name = md.getName();

                l.add(line);
            }

            /**
             * If returns non-zero throw an exception
             */
            if (result.getResult() != 0) {
                throw new Exception("ut-apt predictInstall error (" + result.getResult() + ") :" + result.getOutput());
            }
        }

        return l;
    }
}
