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
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.NodeInstantiatedMessage;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implements NodeManager.
 */
public class NodeManagerImpl implements NodeManager
{
    private final static String NODE_MANAGER_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/node_manager";
    private final static String NODE_MANAGER_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-node-manager.py";

    private final Logger logger = Logger.getLogger(getClass());

    private final Map<NodeSettings, NodeContextImpl> loadedNodesMap = new ConcurrentHashMap<NodeSettings, NodeContextImpl>();

    private NodeManagerSettings settings = null;
    
    private Map<Long,Set<String>> enabledNodes = new HashMap<Long,Set<String>>();

    private boolean live = true;

    /*
     * Update this value to a new long whenever clearing enabled nodes.  This way
     * it is possible to quickly determine if an enabled nodes lookup should be cached
     * without synchronizing the entire operation.
     */
    private long enabledNodesCleared = 0;

    public NodeManagerImpl() { }

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

    public List<NodeSettings> nodeInstances( String nodeName )
    {
        List<NodeSettings> l = new LinkedList<NodeSettings>();

        for (NodeSettings nodeSettings : loadedNodesMap.keySet()) {
            NodeContext nodeContext = loadedNodesMap.get(nodeSettings);
            if (nodeContext != null) {
                if ( nodeContext.getNodeProperties().getName().equals( nodeName ) ) {
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
                String n = tc.getNodeProperties().getName();

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

    public List<NodeSettings> visibleNodes( Long policyId )
    {
        List<NodeSettings> loadedNodesMap = nodeInstances();
        List<NodeSettings> l = new ArrayList<NodeSettings>(loadedNodesMap.size());

        for (NodeSettings nodeSettings : getNodesForPolicy( policyId )) {
            NodeContext nc = nodeContext(nodeSettings);
            PackageDesc md = nc.getPackageDesc();

            if (!md.isInvisible()) {
                NodeProperties nd = nc.getNodeProperties();
                l.add(nodeSettings);
            }
        }

        for (NodeSettings nodeSettings : loadedNodesMap) {
            NodeContext nc = nodeContext(nodeSettings);
            PackageDesc md = nc.getPackageDesc();

            PackageDesc.Type type = md.getType();
            if (!md.isInvisible() && PackageDesc.Type.SERVICE == type) {
                NodeProperties nd = nc.getNodeProperties();
                l.add(nodeSettings);
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

    public NodeSettings instantiate(String nodeName) throws DeployException
    {
        Long policyId = getDefaultPolicyForNode( nodeName );
        return instantiate( nodeName, policyId );
    }

    public NodeSettings instantiate(String nodeName, Long policyId) throws DeployException
    {
        logger.info("instantiate( name:" + nodeName + " , policy:" + policyId + " )");

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
        
        NodeContextImpl nodeContext;
        NodeProperties nodeProperties;
        NodeSettings nodeSettings;
        synchronized (this) {
            //test if not duplicated
            List<NodeSettings> instancesList=this.nodeInstances( nodeName, policyId, false );
            if(instancesList.size()>0) {
                logger.warn("A node instance already exists for " + nodeName + " under " + policyId + " policy; will not instantiate another one.");
                return null; //return if the node is already installed
            }
            nodeSettings = newNodeSettings( policyId, nodeName );

            URL[] resUrls = new URL[] { tbm.getResourceDir(packageDesc) };

            logger.info("initializing node desc for: " + nodeName);
            nodeProperties = initNodeProperties(packageDesc, resUrls);

            if (!live) {
                throw new DeployException("NodeManager is shut down");
            }

            /* load annotated classes */
            if (nodeProperties != null) {
                List<String> annotatedClasses = nodeProperties.getAnnotatedClasses();
                boolean classAdded = false;
                if (annotatedClasses != null) {
                    for (String clz : annotatedClasses) {
                        classAdded |= UvmContextImpl.getInstance().addAnnotatedClass(clz);
                    }
                }
                if (classAdded)
                    UvmContextImpl.getInstance().refreshSessionFactory();
            }

            nodeContext = new NodeContextImpl(nodeProperties, nodeSettings, packageDesc.getName(), true);
            loadedNodesMap.put(nodeSettings, nodeContext);
            try {
                nodeContext.init();
            } finally {
                if (nodeContext.node() == null) {
                    logger.warn("nodeContext.node() == null, removing node...");
                    loadedNodesMap.remove(nodeSettings);
                }
            }
        }

        Node node = nodeContext.node();
        PackageDesc.Type type = packageDesc.getType();

        if (null != node && !packageDesc.isInvisible() && (PackageDesc.Type.NODE == type || PackageDesc.Type.SERVICE == type)) {
            MessageManager lmm = uvmContext.messageManager();
            Counters c = lmm.getCounters(node.getNodeSettings().getId());

            NodeInstantiatedMessage ne = new NodeInstantiatedMessage(nodeProperties, nodeSettings, c.getStatDescs(), uvmContext.licenseManager().getLicense(packageDesc.getName()), node.getNodeSettings().getPolicyId());
            MessageManager mm = uvmContext.messageManager();
            mm.submitMessage(ne);
        }

        clearEnabledNodes();
        
        return node.getNodeSettings();
    }

    public NodeSettings instantiateAndStart(String nodeName, Long policyId) throws DeployException
    {
        NodeSettings nodeSettings = instantiate( nodeName, policyId );
        NodeProperties nd = UvmContextImpl.context().nodeManager().nodeContext(nodeSettings).getNodeProperties();
        if (nd.getAutoStart()) {
            NodeContext nc = nodeContext(nodeSettings);
            try {
                nc.node().start();
            } catch (Exception e) {
                throw new DeployException(e);
            }
                
        }
        return nodeSettings;
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

    public void setLoggingNode(Long nodeId)
    {
        UvmRepositorySelector.instance().setThreadLoggingInformation(nodeId.toString());
    }

    public void setLoggingUvm()
    {
        UvmRepositorySelector.instance().setThreadLoggingInformation("uvm");
    }

    // package protected methods ----------------------------------------------

    void unload(NodeSettings nodeSettings)
    {
        synchronized (this) {
            NodeContextImpl tc = loadedNodesMap.get(nodeSettings);
            logger.info("Unloading: " + nodeSettings + " (" + tc.getNodeProperties().getName() + ")");

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

            NodeSettings nodeSettings = null;

            if (0 == l.size()) {
                try {
                    logger.info("Auto-starting new node: " + md.getName());
                    nodeSettings = instantiate(md.getName());
                } catch (DeployException exn) {
                    logger.warn("could not deploy: " + md.getName(), exn);
                    continue;
                }
            } else {
                nodeSettings = l.get(0);
            }

            NodeContext nc = nodeContext(nodeSettings);
            if (null == nc) {
                logger.warn("No node context for router nodeSettings: " + nodeSettings);
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
        Map<NodeSettings, NodeProperties> nodePropertiess = loadNodePropertiess(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        while (0 < unloaded.size()) {
            List<NodeSettings> startQueue = getLoadable(unloaded, nodePropertiess, loadedParents);
            logger.info("loadable in this pass: " + startQueue);
            if (0 == startQueue.size()) {
                logger.info("not all parents loaded, proceeding");
                for (NodeSettings n : unloaded) {
                    List<NodeSettings> l = Collections.singletonList(n);
                    startUnloaded(l, nodePropertiess, loadedParents);
                }
                break;
            }

            startUnloaded(startQueue, nodePropertiess, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart nodes: " + (t1 - t0) + " millis");

        startAutoStart(null);
        
        clearEnabledNodes();
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodeSettings> startQueue, Map<NodeSettings, NodeProperties> nodePropertiess, Set<String> loadedParents)
    {
        ToolboxManager tbm = UvmContextFactory.context().toolboxManager();

        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (final NodeSettings nodeSettings : startQueue) {
            final NodeProperties nodeProperties = nodePropertiess.get(nodeSettings);
            final String name = nodeSettings.getNodeName();
            final PackageDesc packageDesc = tbm.packageDesc(name);
            loadedParents.add(name);

            if (nodeProperties != null) {
                List<String> annotatedClasses = nodeProperties.getAnnotatedClasses();
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
                            NodeContextImpl nodeContext = null;
                            try {
                                nodeContext = new NodeContextImpl( nodeProperties, nodeSettings, packageDesc.getName(), false);
                                loadedNodesMap.put( nodeSettings, nodeContext );
                                nodeContext.init();
                                logger.info("Restarted: " + nodeSettings);
                            } catch (Exception exn) {
                                logger.error("Could not restart: " + nodeSettings, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + nodeSettings, err);
                            }
                            if ( nodeContext != null && nodeContext.node() == null ) {
                                logger.warn("nodeContext.node() == null, removing node...");
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

    private List<NodeSettings> getLoadable(List<NodeSettings> unloaded, Map<NodeSettings, NodeProperties> nodePropertiess, Set<String> loadedParents)
    {
        List<NodeSettings> l = new ArrayList<NodeSettings>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<NodeSettings> i = unloaded.iterator(); i.hasNext(); ) {
            NodeSettings nodeSettings = i.next();
            NodeProperties nodeProperties = nodePropertiess.get(nodeSettings);
            if (null == nodeProperties) {
                logger.warn("Missing NodeProperties for: " + nodeSettings);
                continue;
            }

            List<String> parents = nodeProperties.getParents();

            boolean parentsLoaded = true;
            for (String parent : parents) {
                if (!loadedParents.contains(parent)) {
                    parentsLoaded = false;
                }
                if (false == parentsLoaded) { break; }
            }

            String name = nodeProperties.getName();

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

    private Map<NodeSettings, NodeProperties> loadNodePropertiess(List<NodeSettings> unloaded)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)UvmContextFactory.context().toolboxManager();

        Map<NodeSettings, NodeProperties> nodePropertiess = new HashMap<NodeSettings, NodeProperties>(unloaded.size());

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
                logger.info("initializing node properties for: " + name);
                NodeProperties nodeProperties = initNodeProperties(md, urls);
                nodePropertiess.put(nodeSettings, nodeProperties);
            } catch (DeployException exn) {
                logger.warn("NodeProperties could not be parsed", exn);
            }
        }

        return nodePropertiess;
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
     * Initialize NodeProperties
     *
     * @param urls urls to find node descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private NodeProperties initNodeProperties(PackageDesc packageDesc, URL[] urls) throws DeployException
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NodeProperties nodeProperties = null;
        try {
            String fileName = System.getProperty("uvm.lib.dir") + "/" + packageDesc.getName() + "/" + "nodeProperties";
            nodeProperties = settingsManager.load( NodeProperties.class, fileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        return nodeProperties;
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

}
