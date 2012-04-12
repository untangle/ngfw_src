/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NodeManagerSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.logging.UvmLoggingContext;
import com.untangle.uvm.logging.UvmLoggingContextFactory;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeInstantiated;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implements NodeManager.
 */
public class NodeManagerImpl implements NodeManager, UvmLoggingContextFactory
{
    private final static String NODE_MANAGER_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/node_manager";
    private final static String NODE_MANAGER_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-node-manager.py";

    private final Logger logger = Logger.getLogger(getClass());

    private final Map<NodeSettings, NodeContextImpl> loadedNodesMap = new ConcurrentHashMap<NodeSettings, NodeContextImpl>();
    private final ThreadLocal<NodeContext> threadContexts = new InheritableThreadLocal<NodeContext>();
    private final UvmRepositorySelector repositorySelector;

    private NodeManagerSettings settings = null;
    
    private Map<Long,Set<String>> enabledNodes = new HashMap<Long,Set<String>>();

    private boolean live = true;

    /*
     * Update this value to a new long whenever clearing enabled nodes.  This way
     * it is possible to quickly determine if an enabled nodes lookup should be cached
     * without synchronizing the entire operation.
     */
    private long enabledNodesCleared = 0;

    public NodeManagerImpl(UvmRepositorySelector repositorySelector)
    {
        this.repositorySelector = repositorySelector;
    }

    public List<NodeSettings> nodeInstances()
    {
        List<NodeSettings> nodeList = new ArrayList<NodeSettings>(loadedNodesMap.keySet());

        // sort by view position, for convenience
        Collections.sort(nodeList, new Comparator<NodeSettings>() {
            public int compare(NodeSettings t1, NodeSettings t2) {
                NodeContextImpl tci1 = loadedNodesMap.get(t1);
                NodeContextImpl tci2 = loadedNodesMap.get(t2);
                int rpi1 = tci1.getPackageDesc().getViewPosition();
                int rpi2 = tci2.getPackageDesc().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getPackageDesc().getName().compareToIgnoreCase(tci2.getPackageDesc().getName());
                } else if (rpi1 < rpi2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return nodeList;
    }

    public List<NodeSettings> nodeInstances(String packageName)
    {
        List<NodeSettings> l = new LinkedList<NodeSettings>();

        for (NodeSettings nodeSettings : loadedNodesMap.keySet()) {
            NodeContext tc = loadedNodesMap.get(nodeSettings);
            if (null != tc) {
                if (tc.getNodeDesc().getName().equals(packageName)) {
                    l.add(nodeSettings);
                }
            }
        }

        return l;
    }

    public NodeManagerSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final NodeManagerSettings newSettings)
    {
        _setSettings(newSettings);
    }

    public void saveTargetState( Long nodeId, NodeSettings.NodeState nodeState )
    {
        for ( NodeSettings nSettings : this.settings.getNodes() ) {
            if (nSettings.getId() == nodeId) {
                if (nodeState != nSettings.getTargetState()) {
                    nSettings.setTargetState(nodeState);
                } else {
                    logger.info("ignore saveTargetState(): already in state " + nodeState);
                }
            }
        }
        this.setSettings(this.settings);       
    }
    
    public List<NodeSettings> nodeInstances(String name, Long policyId)
    {
        return nodeInstances( name, policyId, true);
    }

    public List<NodeSettings> nodeInstances(String name, Long policyId, boolean parents)
    {
        List<NodeSettings> l = new ArrayList<NodeSettings>(loadedNodesMap.size());

        for ( NodeSettings nodeSettings : getNodesForPolicy( policyId, parents ) ) {
            NodeContext tc = loadedNodesMap.get(nodeSettings);
            if (null != tc) {
                String n = tc.getNodeDesc().getName();

                if (n.equals(name)) {
                    l.add(nodeSettings);
                }
            }
        }

        return l;
    }

    public List<NodeSettings> nodeInstances( Long policyId )
    {
        return getNodesForPolicy( policyId );
    }

    public List<NodeDesc> visibleNodes( Long policyId )
    {
        List<NodeSettings> loadedNodesMap = nodeInstances();
        List<NodeDesc> l = new ArrayList<NodeDesc>(loadedNodesMap.size());

        for (NodeSettings nodeSettings : getNodesForPolicy( policyId )) {
            NodeContext nc = nodeContext(nodeSettings);
            PackageDesc md = nc.getPackageDesc();

            if (!md.isInvisible()) {
                NodeDesc nd = nc.getNodeDesc();
                l.add(nd);
            }
        }

        for (NodeSettings nodeSettings : loadedNodesMap) {
            NodeContext nc = nodeContext(nodeSettings);
            PackageDesc md = nc.getPackageDesc();

            PackageDesc.Type type = md.getType();
            if (!md.isInvisible() && PackageDesc.Type.SERVICE == type) {
                NodeDesc nd = nc.getNodeDesc();
                l.add(nd);
            }
        }

        return l;
    }

    public NodeContextImpl nodeContext( NodeSettings nodeSettings )
    {
        return loadedNodesMap.get(nodeSettings);
    }

    public NodeContextImpl nodeContext( Long nodeId )
    {
        List<NodeSettings> nodeSettingsList = new ArrayList<NodeSettings>(loadedNodesMap.keySet());
        for (NodeSettings nodeSettings : nodeSettingsList) {
            if (nodeSettings.getId().equals(nodeId))
                return loadedNodesMap.get(nodeSettings);
        }

        return null;
    }
    
    public Node node(String name)
    {
        Node node = null;
        List<NodeSettings> nodeInstances = nodeInstances(name);
        if(nodeInstances.size()>0){
            NodeContext nodeContext = nodeContext(nodeInstances.get(0));
            node = nodeContext.node();
        }
        return node;
    }

    public NodeDesc instantiate(String nodeName) throws DeployException
    {
        Long policyId = getDefaultPolicyForNode( nodeName );
        return instantiate( nodeName, policyId );
    }

    public NodeDesc instantiate(String nodeName, Long policyId) throws DeployException
    {
        UvmContextImpl uvmContext = UvmContextImpl.getInstance();
        ToolboxManagerImpl tbm = uvmContext.toolboxManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);

        if (PackageDesc.Type.SERVICE == packageDesc.getType()) {
            policyId = null;
        }

        /**
         * Check if this type of node already exists in this rack
         * If so, throw an exception (more info in Bug #7801)
         */
        for ( NodeSettings nodeSettings : getNodesForPolicy(policyId,false) ) {
            NodeContext nc = nodeContext(nodeSettings);
            PackageDesc md = nc.getPackageDesc();

            if (md.getName().equals(packageDesc.getName())) {
                throw new DeployException("Node " + packageDesc.getName() + " already exists in Policy " + policyId + ".");
            }
        }
        
        NodeContextImpl tc;
        NodeDesc nodeDesc;
        synchronized (this) {
            //test if not duplicated
            List<NodeSettings> instancesList=this.nodeInstances( nodeName, policyId, false );
            if(instancesList.size()>0) {
                logger.warn("A node instance already exists for " + nodeName + " under " + policyId + " policy; will not instantiate another one.");
                return null; //return if the node is already installed
            }
            NodeSettings nodeSettings = newNodeSettings( policyId, nodeName );

            URL[] resUrls = new URL[] { tbm.getResourceDir(packageDesc) };

            logger.info("initializing node desc for: " + nodeName);
            nodeDesc = initNodeDesc(packageDesc, resUrls, nodeSettings);

            if (!live) {
                throw new DeployException("NodeManager is shut down");
            }

            /* load annotated classes */
            if (nodeDesc != null) {
                List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
                boolean classAdded = false;
                if (annotatedClasses != null) {
                    for (String clz : annotatedClasses) {
                        classAdded |= UvmContextImpl.getInstance().addAnnotatedClass(clz);
                    }
                }
                if (classAdded)
                    UvmContextImpl.getInstance().refreshSessionFactory();
            }

            tc = new NodeContextImpl((URLClassLoader)getClass().getClassLoader(), nodeDesc, packageDesc.getName(), true);
            loadedNodesMap.put(nodeSettings, tc);
            try {
                tc.init();
            } finally {
                if (null == tc.node()) {
                    loadedNodesMap.remove(nodeSettings);
                }
            }
        }

        Node node = tc.node();
        PackageDesc.Type type = packageDesc.getType();

        if (null != node && !packageDesc.isInvisible() && (PackageDesc.Type.NODE == type || PackageDesc.Type.SERVICE == type)) {
            MessageManager lmm = uvmContext.messageManager();
            Counters c = lmm.getCounters(node.getNodeSettings().getId());

            NodeInstantiated ne = new NodeInstantiated(nodeDesc, c.getStatDescs(), uvmContext.licenseManager().getLicense(packageDesc.getName()));
            MessageManager mm = uvmContext.messageManager();
            mm.submitMessage(ne);
        }

        clearEnabledNodes();
        
        return nodeDesc;
    }

    public NodeDesc instantiateAndStart(String nodeName, Long policyId) throws DeployException
    {
        NodeDesc nd = instantiate( nodeName, policyId );
        if (nd.getAutoStart()) {
            NodeContext nc = nodeContext(nd.getNodeSettings());
            try {
                nc.node().start();
            } catch (Exception e) {
                throw new DeployException(e);
            }
                
        }
        return nd;
    }

    public void destroy( Long nodeId ) throws Exception
    {
        final NodeContextImpl nodeContext;

        synchronized (this) {
            nodeContext = nodeContext( nodeId );
            if (null == nodeContext) {
                logger.error("Destroy Failed: Node " + nodeId + " not found");
                throw new Exception("Node " + nodeId + " not found");
            }
            nodeContext.destroy();

            /**
             * Remove from map and list and save settings
             */
            for (Iterator<NodeSettings> iter = loadedNodesMap.keySet().iterator(); iter.hasNext();) {
                NodeSettings nodeSettings = iter.next();
                if (nodeSettings.getId().equals(nodeId))
                    iter.remove();
            }
            for (Iterator<NodeSettings> iter = this.settings.getNodes().iterator(); iter.hasNext();) {
                NodeSettings nodeSettings = iter.next();
                if (nodeSettings.getId().equals(nodeId))
                    iter.remove();
            }
            this.setSettings(this.settings);       
        }

        clearEnabledNodes();

        return;
    }

    public Map<NodeSettings, NodeSettings.NodeState> allNodeStates()
    {
        HashMap<NodeSettings, NodeSettings.NodeState> result = new HashMap<NodeSettings, NodeSettings.NodeState>();
        for (Iterator<NodeSettings> iter = loadedNodesMap.keySet().iterator(); iter.hasNext();) {
            NodeSettings nodeSettings = iter.next();
            NodeContextImpl tci = loadedNodesMap.get(nodeSettings);
            result.put(nodeSettings, tci.getRunState());
        }

        return result;
    }

    /**
     * Get a map of nodes that are enabled for a policy, this takes into account
     * parent / child relationships
     */
    @Override
    public Set<String> getEnabledNodes( Long policyId )
    {
        if ( policyId == null ) {
            return Collections.emptySet();
        }
        
        Set<String> policyNodes = null;
        long enabledNodesCleared = 0;
        
        /* With the lock, check if there is an entry and return it if exists.
         * Otherwise, create an
         */
        synchronized ( this.enabledNodes ) {
            policyNodes = this.enabledNodes.get(policyId);
            enabledNodesCleared = this.enabledNodesCleared ;
        }

        if ( policyNodes == null ) {
            policyNodes = new HashSet<String>();
            List<NodeSettings> policyNodeSettingss = getNodesForPolicy( policyId, true );

            for ( NodeSettings nodeSettings : policyNodeSettingss ) {
                NodeContext nodeContext = loadedNodesMap.get(nodeSettings);
                if ( nodeContext == null ) {
                    logger.warn( "Node context is null for nodeSettings: " + nodeSettings );
                    continue;
                }

                if ( nodeContext.getRunState() == NodeSettings.NodeState.RUNNING ) {
                    policyNodes.add( nodeSettings.getNodeName() );
                }
            }
            
            synchronized( this.enabledNodes ) {
                if ( enabledNodesCleared == this.enabledNodesCleared ) {
                    this.enabledNodes.put( policyId, policyNodes );
                }
            }
                

        }
        
        return policyNodes;
    }
    
    @Override
    public void flushNodeStateCache()
    {
        this.clearEnabledNodes();
    }

    public boolean isInstantiated(String nodeName)
    {
        return (this.node(nodeName) != null);
    }

    // Manager lifetime -------------------------------------------------------

    void init()
    {
        loadSettings();

        restartUnloaded();
        
        clearEnabledNodes();
    }

    void destroy()
    {
        synchronized (this) {
            live = false;

            Set<NodeSettings> s = new HashSet<NodeSettings>(loadedNodesMap.keySet());

            for ( NodeSettings nodeSettings : s ) {
                if (null != nodeSettings) {
                    unload(nodeSettings);
                }
            }

            if (loadedNodesMap.size() > 0) {
                logger.warn("node instances not destroyed: " + loadedNodesMap.size());
            }
        }

        logger.info("NodeManager destroyed");
    }

    // NodeManager methods -----------------------------------------------

    public NodeContext threadContext()
    {
        return threadContexts.get();
    }

    public void registerThreadContext(NodeContext ctx)
    {
        threadContexts.set(ctx);
        repositorySelector.setContextFactory(this);
    }

    public void deregisterThreadContext()
    {
        threadContexts.remove();
        repositorySelector.uvmContext();        
    }

    // UvmLoggingContextFactory methods ---------------------------------------

    public UvmLoggingContext getLoggingContext()
    {
        final NodeContext nodeContext = threadContexts.get();
        if (null == nodeContext) {
            LogLog.warn("null node context in threadContexts");
        }

        return new NodeManagerLoggingContext(nodeContext);
    }

    // package protected methods ----------------------------------------------

    void unload(NodeSettings nodeSettings)
    {
        synchronized (this) {
            NodeContextImpl tc = loadedNodesMap.get(nodeSettings);
            logger.info("Unloading: " + nodeSettings + " (" + tc.getNodeDesc().getName() + ")");

            tc.unload();
            loadedNodesMap.remove(nodeSettings);
        }
        
        clearEnabledNodes();
    }

    void restart(String name)
    {
        ToolboxManager tbm = UvmContextFactory.context().toolboxManager();

        PackageDesc pd = tbm.packageDesc(name);
        if (pd == null) {
            logger.warn("Failed to restart: Unable to find package \"" + name + "\"");
            return;
        }
        String availVer = pd.getInstalledVersion();

        synchronized (this) {
            List<NodeSettings> nNodeSettingss = nodeInstances(name);
            if (0 < nNodeSettingss.size()) {
                NodeSettings t = nNodeSettingss.get(0);
                NodeContext tc = loadedNodesMap.get(t);

                for (NodeSettings nodeSettings : nNodeSettingss) {
                    PackageDesc md = loadedNodesMap.get(nodeSettings).getPackageDesc();
                    if (!md.getInstalledVersion().equals(availVer)) {
                        logger.info("Restarting \"" + name + "\" - new version available. (" + availVer + " > " + md.getInstalledVersion() + ")");
                        unload(nodeSettings);
                    } else {
                        logger.info("Skipping Restart \"" + name + "\" - no new version available. (" + availVer + " = " + md.getInstalledVersion() + ")");
                    }
                }
                restartUnloaded();
            }
        }
        
        clearEnabledNodes();
    }

    void startAutoStart(PackageDesc extraPkg)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)UvmContextFactory.context().toolboxManager();

        List<PackageDesc> mds = new ArrayList<PackageDesc>();

        for (PackageDesc md : tbm.installed()) {
            if (md.isAutoStart()) {
                mds.add(md);
            }
        }

        if (null != extraPkg && extraPkg.isAutoStart()) {
            mds.add(extraPkg);
        }
        for (PackageDesc md : mds) {
            List<NodeSettings> l = nodeInstances(md.getName());

            NodeSettings t = null;

            if (0 == l.size()) {
                try {
                    logger.info("Auto-starting new node: " + md.getName());
                    t = instantiate(md.getName()).getNodeSettings();
                } catch (DeployException exn) {
                    logger.warn("could not deploy: " + md.getName(), exn);
                    continue;
                }
            } else {
                t = l.get(0);
            }

            NodeContext nc = nodeContext(t);
            if (null == nc) {
                logger.warn("No node context for router nodeSettings: " + t);
            } else {
                Node n = nc.node();
                NodeSettings.NodeState ns = n.getRunState();
                switch (ns) {
                case INITIALIZED:
                    try {
                        n.start();
                    } catch (Exception exn) {
                        logger.warn("could not load: " + md.getName(), exn);
                        continue;
                    }
                    break;
                case RUNNING:
                    // nothing left to do.
                    break;
                default:
                    logger.warn(md.getName() + " unexpected state: " + ns);
                    break;
                }
            }
        }
        
        clearEnabledNodes();
    }

    // private methods --------------------------------------------------------

    private void restartUnloaded()
    {
        long t0 = System.currentTimeMillis();

        if (!live) {
            throw new RuntimeException("NodeManager is shut down");
        }

        logger.info("Restarting unloaded nodes...");

        List<NodeSettings> unloaded = getUnloaded();
        Map<NodeSettings, NodeDesc> nodeDescs = loadNodeDescs(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        while (0 < unloaded.size()) {
            List<NodeSettings> startQueue = getLoadable(unloaded, nodeDescs, loadedParents);
            logger.info("loadable in this pass: " + startQueue);
            if (0 == startQueue.size()) {
                logger.info("not all parents loaded, proceeding");
                for (NodeSettings n : unloaded) {
                    List<NodeSettings> l = Collections.singletonList(n);
                    startUnloaded(l, nodeDescs, loadedParents);
                }
                break;
            }

            startUnloaded(startQueue, nodeDescs, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart nodes: " + (t1 - t0) + " millis");

        startAutoStart(null);
        
        clearEnabledNodes();
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodeSettings> startQueue, Map<NodeSettings, NodeDesc> nodeDescs, Set<String> loadedParents)
    {
        ToolboxManager tbm = UvmContextFactory.context().toolboxManager();

        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (final NodeSettings nodeSettings : startQueue) {
            final NodeDesc nodeDesc = nodeDescs.get(nodeSettings);
            final String name = nodeSettings.getNodeName();
            final PackageDesc packageDesc = tbm.packageDesc(name);
            loadedParents.add(name);

            if (nodeDesc != null) {
                List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
                if (annotatedClasses != null) {
                    for (String clz : annotatedClasses) {
                        UvmContextImpl.getInstance().addAnnotatedClass(clz);
                    }
                }
                UvmContextImpl.getInstance().refreshSessionFactory();
            }

            if (packageDesc != null) {
                Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            logger.info("Restarting: " + nodeSettings + " (" + name + ")");
                            NodeContextImpl tc = null;
                            try {
                                tc = new NodeContextImpl((URLClassLoader)getClass().getClassLoader(),nodeDesc,packageDesc.getName(),false);
                                loadedNodesMap.put(nodeSettings, tc);
                                tc.init();
                                logger.info("Restarted: " + nodeSettings);
                            } catch (Exception exn) {
                                logger.error("Could not restart: " + nodeSettings, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + nodeSettings, err);
                            }
                            if (null != tc && null == tc.node()) {
                                loadedNodesMap.remove(nodeSettings);
                            }
                        }
                    };
                restarters.add(r);
            } else {
                logger.error("Unable to find node \"" + name + "\" - Skipping");
            }
        }

        Set<Thread> threads = new HashSet<Thread>(restarters.size());
        int loadLimit = Runtime.getRuntime().availableProcessors() << 1;
        try {
            for (Iterator<Runnable> riter = restarters.iterator(); riter.hasNext();) {
                while (getRunnableCount(threads) < loadLimit && riter.hasNext()) {
                    Thread t = UvmContextFactory.context().
                        newThread(riter.next(), "START_" + startThreadNum++);
                    threads.add(t);
                    t.start();
                }
                if (riter.hasNext())
                    Thread.sleep(200);
            }
            // Must wait for them to start before we can go on to next wave.
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException exn) {
            logger.error("Interrupted while starting transforms"); // Give up
        }
        
        clearEnabledNodes();
    }

    private int getRunnableCount(Set<Thread> threads) {
        int result = 0;
        for (Iterator<Thread> iter = threads.iterator(); iter.hasNext();) {
            Thread t = iter.next();
            if (!t.isAlive()) {
                // logger.info("Thread " + t.getName() + " is dead, removing.");
                iter.remove();
            } else {
                Thread.State state = t.getState();
                // logger.info("Thread " + t.getName() + " is in state " + t.getState());
                if (state == Thread.State.RUNNABLE)
                    result++;
            }
        }
        return result;
    }

    private List<NodeSettings> getLoadable(List<NodeSettings> unloaded, Map<NodeSettings, NodeDesc> nodeDescs, Set<String> loadedParents)
    {
        List<NodeSettings> l = new ArrayList<NodeSettings>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<NodeSettings> i = unloaded.iterator(); i.hasNext(); ) {
            NodeSettings nodeSettings = i.next();
            NodeDesc nodeDesc = nodeDescs.get(nodeSettings);
            if (null == nodeDesc) {
                logger.warn("Missing NodeDesc for: " + nodeSettings);
                continue;
            }

            List<String> parents = nodeDesc.getParents();

            boolean parentsLoaded = true;
            for (String parent : parents) {
                if (!loadedParents.contains(parent)) {
                    parentsLoaded = false;
                }
                if (false == parentsLoaded) { break; }
            }

            String name = nodeDesc.getName();

            // all parents loaded and another instance of this
            // node not loading this pass or already loaded in
            // previous pass (prevents classloader race).
            if (parentsLoaded && (!thisPass.contains(name) || loadedParents.contains(name))) {
                i.remove();
                l.add(nodeSettings);
                thisPass.add(name);
            }
        }

        return l;
    }

    private Map<NodeSettings, NodeDesc> loadNodeDescs(List<NodeSettings> unloaded)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)UvmContextFactory.context().toolboxManager();

        Map<NodeSettings, NodeDesc> nodeDescs = new HashMap<NodeSettings, NodeDesc>(unloaded.size());

        for (NodeSettings nodeSettings : unloaded) {
            String name = nodeSettings.getNodeName();
            logger.info("Getting mackage desc for: " + name);
            PackageDesc md = tbm.packageDesc(name);
            if (null == md) {
                logger.warn("could not get mackage desc for: " + name);
                continue;
            }

            URL[] urls = new URL[] { tbm.getResourceDir(md) };
            nodeSettings.setNodeName(name);

            try {
                logger.info("initializing node desc for: " + name);
                NodeDesc nodeDesc = initNodeDesc(md, urls, nodeSettings);
                nodeDescs.put(nodeSettings, nodeDesc);
            } catch (DeployException exn) {
                logger.warn("NodeDesc could not be parsed", exn);
            }
        }

        return nodeDescs;
    }

    private NodeManagerSettings loadSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NodeManagerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "node_manager";

        try {
            readSettings = settingsManager.load( NodeManagerSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                String convertCmd = NODE_MANAGER_CONVERSION_SCRIPT + " " + settingsFileName + ".js";
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( NodeManagerSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }
        

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.info("Settings: " + this.settings.toJSONString());
        }

        return this.settings;
    }

    private void initializeSettings()
    {
        logger.info("Initializing Settings...");

        NodeManagerSettings newSettings = new NodeManagerSettings();

        this.setSettings(newSettings);
    }
    
    /**
     * Reads the setting and returns all the nodes in the settings that aren't already loaded
     */
    private List<NodeSettings> getUnloaded()
    {
        final List<NodeSettings> unloaded = new LinkedList<NodeSettings>();

        for (NodeSettings nSettings : settings.getNodes()) {
            if (!loadedNodesMap.containsKey(nSettings)) {
                unloaded.add(nSettings);
            }
        }

        return unloaded;
    }

    /**
     * Initialize NodeDesc
     *
     * @param urls urls to find node descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private NodeDesc initNodeDesc(PackageDesc packageDesc, URL[] urls, NodeSettings nodeSettings) throws DeployException
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NodeDesc nodeDesc = null;
        try {
            String fileName = System.getProperty("uvm.lib.dir") + "/" + packageDesc.getName() + "/" + "nodeDesc";
            nodeDesc = settingsManager.load( NodeDesc.class, fileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        if (nodeDesc != null) {
            nodeDesc.setNodeSettings(nodeSettings);

            //             List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
            //             if (annotatedClasses != null) {
            //                 for (String clz : annotatedClasses) {
            //                     UvmContextImpl.getInstance().addAnnotatedClass(clz);
            //                 }
            //             }
            //UvmContextImpl.getInstance().refreshSessionFactory();

            return nodeDesc;
        }

        return null;
    }

    private Long getDefaultPolicyForNode(String nodeName) throws DeployException
    {
        ToolboxManager tbm = UvmContextFactory.context().toolboxManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);
        if (packageDesc == null)
            throw new DeployException("Node named " + nodeName + " not found");
        if (PackageDesc.Type.SERVICE == packageDesc.getType()) {
            return null;
        } else {
            return 1L; /* XXX */
        }
    }

    private NodeSettings newNodeSettings( Long policyId, String nodeName ) throws DeployException
    {
        long newNodeId = settings.getNextNodeId();

        /**
         * Increment the next node Id
         */
        List<NodeSettings> nodes = settings.getNodes();
        settings.setNextNodeId(newNodeId+1);
        
        /**
         * Create the new node settings and add to the node manager settings
         */
        NodeSettings nodeSettings = new NodeSettings( newNodeId, policyId, nodeName );
        nodes.add(nodeSettings);

        /**
         * Save the new node manager settings
         */
        _setSettings(settings);
        
        return nodeSettings;
    }

    private List<Long> getParentPolicies(Long policyId)
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy");
        List<Long> parentList = new ArrayList<Long>();
        if (policyManager == null)
            return parentList;
        
        for ( Long parentId = policyManager.getParentPolicyId(policyId) ; parentId != null ; parentId = policyManager.getParentPolicyId(parentId) ) {
            parentList.add(parentId);
        }

        return parentList;
    }

    private List<NodeSettings> getNodesForPolicy(Long policyId)
    {
        return getNodesForPolicy(policyId,true);
    }

    private List<NodeSettings> getNodesForPolicy(Long policyId, boolean parents)
    {
        List<Long> parentPolicies = null;

        if (parents && policyId != null) 
            parentPolicies = getParentPolicies(policyId);
        else 
            parentPolicies = new ArrayList<Long>();
        
        /*
         * This is a list of loadedNodesMap.  Each index of the first list corresponds to its
         * policy in the policies array.  Each index in the second list is a nodeSettings of the nodes
         * in the policy
         * parentNodeSettingsArray[0] == list of loadedNodesMap in parentPolicies[0]
         * parentNodeSettingsArray[1] == list of loadedNodesMap in parentPolicies[1]
         * ...
         * parentNodeSettingsArray[n] == list of loadedNodesMap in parentPolicies[n]
         * Policies are ordered parentNodeSettingsArray[0] is the first parent, etc 
         */
        List<List<NodeSettings>> parentNodeSettingsArray = new ArrayList<List<NodeSettings>>(parentPolicies.size());
        List<NodeSettings> thisPolicyNodeSettingss = new ArrayList<NodeSettings>();
        for (int i = 0; i < parentPolicies.size(); i++) {
            parentNodeSettingsArray.add(new ArrayList<NodeSettings>());
        }

        /*
         * Fill in the inner list, at the end each of these is the list of 
         * nodes in the policy.
         */
        for (NodeSettings nodeSettings : loadedNodesMap.keySet()) {
            NodeContext nodeContext = loadedNodesMap.get(nodeSettings);

            if (nodeContext != null) {
                Long nodePolicyId = nodeSettings.getPolicyId();

                /**
                 * If its in the parent policy list - add it
                 * Otherwise it its in the policy - add it
                 */
                int i = parentPolicies.indexOf(nodePolicyId);
                if (i >= 0) {
                    parentNodeSettingsArray.get(i).add(nodeSettings);
                } else if (nodePolicyId == null && policyId == null) {
                    thisPolicyNodeSettingss.add(nodeSettings);
                } else if (nodePolicyId != null && policyId != null && nodePolicyId.equals(policyId)) {
                    thisPolicyNodeSettingss.add(nodeSettings);
                }
            }
        }

        /*
         * Add all the loadedNodesMap from the current policy
         * And all the nodes from the parent IFF they don't already exists
         * will only add the first entry (which will be most specific node.
         */
        List<NodeSettings> finalList = thisPolicyNodeSettingss;
        Set<String> names = new HashSet<String>();

        for (NodeSettings nodeSettings : thisPolicyNodeSettingss) {
            String n = nodeSettings.getNodeName();
            if (!names.contains(n))
                names.add(n);
        }
        for (List<NodeSettings> parentPolicyList : parentNodeSettingsArray) {
            if (parentPolicyList != null) {
                for (NodeSettings nodeSettings : parentPolicyList) {
                    String n = nodeSettings.getNodeName();
                    if (!names.contains(n)) {
                        names.add(n);
                        finalList.add(nodeSettings);
                    }
                }
            }
        }

        return finalList;
    }
    
    /**
     * Used to empty the cache of the enabled nodes.  This cache is used to build
     * the pipeline, so it must be updated whenever the node state changes.
     */
    private void clearEnabledNodes()
    {
        logger.debug( "clearing the cache of enabled nodes." );
        synchronized ( this.enabledNodes ) {
            this.enabledNodes.clear();
            this.enabledNodesCleared = System.nanoTime();
        }
    }

    private void _setSettings( NodeManagerSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(NodeManagerSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "node_manager", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    // private static classes -------------------------------------------------

    private static class NodeManagerLoggingContext implements UvmLoggingContext
    {
        private final NodeContext nodeContext;

        // constructors -------------------------------------------------------

        NodeManagerLoggingContext(NodeContext nodeContext)
        {
            this.nodeContext = nodeContext;
        }

        // UvmLoggingContext methods -----------------------------------------

        public String getConfigName()
        {
            return "log4j-node.xml";
        }

        public String getFileName()
        {
            if (null == nodeContext) {
                return "0";
            } else {
                return nodeContext.getNodeSettings().getId().toString();
            }
        }

        public String getName()
        {
            if (null == nodeContext) {
                return "0";
            } else {
                return nodeContext.getNodeSettings().getId().toString();
            }
        }

        // Object methods -----------------------------------------------------

        public boolean equals(Object o)
        {
            if (o instanceof NodeManagerLoggingContext) {
                NodeManagerLoggingContext tmc
                    = (NodeManagerLoggingContext)o;
                return nodeContext.equals(tmc.nodeContext);
            } else {
                return false;
            }
        }

        public int hashCode()
        {
            return nodeContext.hashCode();
        }
    }

}
