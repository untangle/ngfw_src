/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.NodeInstantiatedMessage;
import com.untangle.uvm.node.NodeManagerSettings;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.apt.PackageDesc;
import com.untangle.uvm.apt.AptManager;
import com.untangle.uvm.util.Pulse;

/**
 * Implements NodeManager.
 */
public class NodeManagerImpl implements NodeManager
{
    private final static String NODE_MANAGER_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/nodes";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Stores a map of all currently loaded nodes from their nodeId to the Node instance
     */
    private final Map<Long, Node> loadedNodesMap = new ConcurrentHashMap<Long, Node>();

    private NodeManagerSettings settings = null;
    
    private boolean live = true;

    public NodeManagerImpl() { }

    public NodeManagerSettings getSettings()
    {
        return this.settings;
    }

    public void saveTargetState( Node node, NodeSettings.NodeState nodeState )
    {
        if ( node == null ) {
            logger.error("Invalid argument saveTargetState(): node is null");
            return;
        }
        if ( nodeState == null ) {
            logger.error("Invalid argument saveTargetState(): nodeState is null");
            return;
        }

        for ( NodeSettings nSettings : this.settings.getNodes() ) {
            if ( nSettings.getId() == node.getNodeSettings().getId() ) {
                if ( nodeState != nSettings.getTargetState() ) {
                    nSettings.setTargetState(nodeState);
                } else {
                    logger.info("ignore saveTargetState(): already in state " + nodeState);
                }
            }
        }
        this._setSettings(this.settings);       
    }

    public List<Node> nodeInstances()
    {
        List<Node> nodeList = new ArrayList<Node>( loadedNodesMap.values() );

        // sort by view position, for convenience
        Collections.sort(nodeList, new Comparator<Node>() {
            public int compare(Node tci1, Node tci2) {
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

    public List<Long> nodeInstancesIds()
    {
        return nodeToIdList(nodeInstances());
    }

    public List<Node> nodeInstances( String nodeName )
    {
        List<Node> list = new LinkedList<Node>();

        for (Node node : loadedNodesMap.values()) {
            if ( node.getNodeProperties().getName().equals( nodeName ) ) {
                list.add( node );
            }
        }

        return list;
    }

    public List<Node> nodeInstances( String name, Long policyId )
    {
        return nodeInstances( name, policyId, true);
    }

    public List<Node> nodeInstances( String name, Long policyId, boolean parents )
    {
        List<Node> list = new ArrayList<Node>(loadedNodesMap.size());

        for ( Node node : getNodesForPolicy( policyId, parents ) ) {
            String nodeName = node.getNodeProperties().getName();

            if (nodeName.equals(name)) {
                list.add(node);
            }
        }

        return list;
    }

    public List<Node> nodeInstances( Long policyId )
    {
        return getNodesForPolicy( policyId );
    }

    public List<Long> nodeInstancesIds( Long policyId )
    {
        return nodeToIdList( nodeInstances( policyId ) );
    }
    
    protected List<Node> visibleNodes( Long policyId )
    {
        List<Node> loadedNodes = nodeInstances();
        List<Node> list = new ArrayList<Node>(loadedNodes.size());

        for (Node node : getNodesForPolicy( policyId )) {
            if ( !node.getPackageDesc().isInvisible() ) {
                list.add( node );
            }
        }

        for (Node node : loadedNodes) {
            if ( !node.getPackageDesc().isInvisible() && node.getPackageDesc().getType() == PackageDesc.Type.SERVICE ) {
                list.add( node );
            }
        }

        return list;
    }

    public Node node( Long nodeId )
    {
        return loadedNodesMap.get( nodeId );
    }
    
    public Node node( String name )
    {
        List<Node> nodes = nodeInstances( name );
        if( nodes.size() > 0 ){
            return nodes.get(0);
        }
        return null;
    }

    public Node instantiate( String nodeName ) throws Exception
    {
        Long policyId = getDefaultPolicyForNode( nodeName );
        return instantiate( nodeName, policyId );
    }

    public Node instantiate( String nodeName, Long policyId ) throws Exception
    {
        logger.info("instantiate( name:" + nodeName + " , policy:" + policyId + " )");

        UvmContextImpl uvmContext = UvmContextImpl.getInstance();
        AptManagerImpl tbm = uvmContext.aptManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);

        if ( packageDesc == null ) {
            logger.error("Failed to locate package description for : " + nodeName);
            return null;
        }

        if (PackageDesc.Type.SERVICE == packageDesc.getType()) {
            policyId = null;
        }

        Node node = null;
        NodeProperties nodeProperties = null;
        NodeSettings nodeSettings = null;
        
        synchronized (this) {
            logger.info("initializing node: " + nodeName);

            if ( nodeInstances( nodeName, policyId, false ).size() >= 1 ) 
                throw new Exception("too many instances: " + nodeName);

            nodeProperties = initNodeProperties( packageDesc );
            nodeSettings = createNewNodeSettings( policyId, nodeName );

            if (!live) 
                throw new Exception("NodeManager is shut down");

            /**
             * Check all the basics
             */
            if (nodeSettings == null) 
                throw new Exception("Null nodeSettings: " + nodeName);
            if (nodeProperties == null) 
                throw new Exception("Null nodeProperties: " + nodeName);
            if (packageDesc == null) 
                throw new Exception("Null packageDesc: " + nodeName);

            node = NodeBase.loadClass(nodeProperties, nodeSettings, packageDesc, true);

            if (node != null) {
                loadedNodesMap.put(nodeSettings.getId(), node);
                saveNewNodeSettings( nodeSettings );
            } else {
                logger.error("Failed to initialize node: " + packageDesc.getName());
            }
            
        }

        PackageDesc.Type type = packageDesc.getType();

        if ( node != null && !packageDesc.isInvisible() && (PackageDesc.Type.NODE == type || PackageDesc.Type.SERVICE == type)) {
            NodeInstantiatedMessage ne = new NodeInstantiatedMessage(nodeProperties, nodeSettings, node.getMetrics(), uvmContext.licenseManager().getLicense(packageDesc.getName()), node.getNodeSettings().getPolicyId());
            uvmContext.messageManager().submitMessage(ne);
        }

        return node;
    }

    public Node instantiateAndStart( String nodeName, Long policyId ) throws Exception
    {
        Node node = instantiate( nodeName, policyId );
        NodeProperties nd = node.getNodeProperties();
        if (nd.getAutoStart()) {
            try {
                node.start();
            } catch (Exception e) {
                throw new Exception(e);
            }
                
        }
        return node;
    }

    public void destroy( Long nodeId ) throws Exception
    {
        destroy( node( nodeId ));
    }

    public void destroy( Node node ) throws Exception
    {
        if ( node == null) {
            throw new Exception("Node " + node + " not found");
        }
            
        synchronized (this) {
            NodeBase nodeBase = (NodeBase) node;
            nodeBase.destroyClass();

            /**
             * Remove from map and list and save settings
             */
            loadedNodesMap.remove( node.getNodeSettings().getId() );
            for (Iterator<NodeSettings> iter = this.settings.getNodes().iterator(); iter.hasNext();) {
                NodeSettings nodeSettings = iter.next();
                if (nodeSettings.getId().equals(node.getNodeSettings().getId()))
                    iter.remove();
            }
            this._setSettings(this.settings);       
        }

        return;
    }

    public Map<Long, NodeSettings.NodeState> allNodeStates()
    {
        HashMap<Long, NodeSettings.NodeState> result = new HashMap<Long, NodeSettings.NodeState>();
        for (Node node : loadedNodesMap.values()) {
            result.put(node.getNodeSettings().getId(), node.getRunState());
        }

        return result;
    }

    public boolean isInstantiated( String nodeName )
    {
        return (this.node(nodeName) != null);
    }


    
    public Map<Long, NodeSettings> allNodeSettings()
    {
        HashMap<Long, NodeSettings> result = new HashMap<Long, NodeSettings>();
        for (Node node : loadedNodesMap.values()) {
            result.put(node.getNodeSettings().getId(), node.getNodeSettings());
        }
        return result;
    }
    
    public Map<Long, NodeProperties> allNodeProperties()
    {
        HashMap<Long, NodeProperties> result = new HashMap<Long, NodeProperties>();
        for (Node node : loadedNodesMap.values()) {
            result.put(node.getNodeSettings().getId(), node.getNodeProperties());
        }
        return result;
    }

    // Manager lifetime -------------------------------------------------------

    protected void init()
    {
        loadSettings();

        restartUnloaded();

        logger.info("Initialized NodeManager");
    }

    protected void destroy()
    {
        synchronized (this) {
            live = false;

            for ( Node node : loadedNodesMap.values() ) {
                unload( node );
            }

            if ( loadedNodesMap.size() > 0 ) {
                logger.warn("node instances not destroyed: " + loadedNodesMap.size());
            }
        }

        logger.info("NodeManager destroyed");
    }

    protected void unload( Node node )
    {
        synchronized (this) {
            logger.info("Unloading: " + node.getNodeProperties().getName());
            ((NodeBase)node).unloadClass();
            loadedNodesMap.remove( node.getNodeSettings().getId() );
        }
    }

    protected void startAutoStart( PackageDesc extraPkg )
    {
        AptManagerImpl tbm = (AptManagerImpl)UvmContextFactory.context().aptManager();

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
            List<Node> list = nodeInstances(md.getName());

            Node node = null;

            if ( list.size() == 0 ) {
                try {
                    logger.info("Auto-starting new node: " + md.getName());
                    node = instantiate(md.getName());
                } catch (Exception exn) {
                    logger.warn("could not deploy: " + md.getName(), exn);
                    continue;
                }
            } else {
                node = list.get(0);
            }

            if (node == null) {
                logger.warn("No node context for router node: " + node);
            } else {
                NodeSettings.NodeState ns = node.getRunState();
                switch (ns) {
                case INITIALIZED:
                    try {
                        node.start();
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
        Map<NodeSettings, NodeProperties> nodePropertiesMap = loadNodePropertiess(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        while ( unloaded.size() > 0 ) {

            List<NodeSettings> startQueue = getLoadable(unloaded, nodePropertiesMap, loadedParents);

            for (NodeSettings ns : startQueue) 
                logger.info("Loading in this pass: " + ns.getNodeName() + " (" + ns.getId() + ")");

            if ( startQueue.size() == 0 ) {
                logger.info("not all parents loaded, proceeding");
                for (NodeSettings n : unloaded) {
                    List<NodeSettings> l = Collections.singletonList(n);
                    startUnloaded(l, nodePropertiesMap, loadedParents);
                }
                break;
            }

            startUnloaded(startQueue, nodePropertiesMap, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart nodes: " + (t1 - t0) + " millis");

        startAutoStart(null);
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodeSettings> startQueue, Map<NodeSettings, NodeProperties> nodePropertiesMap, Set<String> loadedParents)
    {
        AptManager tbm = UvmContextFactory.context().aptManager();

        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (final NodeSettings nodeSettings : startQueue) {
            final String name = nodeSettings.getNodeName();
            final NodeProperties nodeProps = nodePropertiesMap.get(nodeSettings);
            final PackageDesc packageDesc = tbm.packageDesc(name);

            if ( name == null ) {
                logger.error("Unable to load node \"" + name + "\": NULL name.");
            } else if ( nodeProps == null ) {
                logger.error("Unable to load node \"" + name + "\": NULL node properties.");
            } else if ( packageDesc == null ) {
                logger.error("Unable to load node \"" + name + "\": NULL package desc.");
            } else {
                Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            logger.info("Restarting: " + name + " (" + nodeSettings.getId() + ")");
                            NodeBase node = null;
                                try {
                                node = (NodeBase)NodeBase.loadClass(nodeProps, nodeSettings, packageDesc, false);
                                loadedNodesMap.put( nodeSettings.getId(), node );
                                logger.info("Restarted : " + name + " (" + nodeSettings.getId() + ")");
                            } catch (Exception exn) {
                                logger.error("Could not restart: " + name, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + name, err);
                            }
                            if ( node == null ) {
                                logger.error("Failed to load node:" + name);
                                loadedNodesMap.remove(nodeSettings);
                            }
                        }
                    };
                restarters.add(r);
                loadedParents.add(name);
            } 
        }

        Set<Thread> threads = new HashSet<Thread>(restarters.size());
        int loadLimit = Runtime.getRuntime().availableProcessors() * 2;
        try {
            for (Iterator<Runnable> riter = restarters.iterator(); riter.hasNext();) {
                while (getRunnableCount(threads) < loadLimit && riter.hasNext()) {
                    Thread t = UvmContextFactory.context().newThread(riter.next(), "START_" + startThreadNum++);
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

    private List<NodeSettings> getLoadable(List<NodeSettings> unloaded, Map<NodeSettings, NodeProperties> nodePropertiesMap, Set<String> loadedParents)
    {
        List<NodeSettings> l = new ArrayList<NodeSettings>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<NodeSettings> i = unloaded.iterator(); i.hasNext(); ) {
            NodeSettings nodeSettings = i.next();
            NodeProperties nodeProps = nodePropertiesMap.get(nodeSettings);
            if ( nodeProps == null ) {
                logger.warn("Missing NodeProperties for: " + nodeSettings);
                continue;
            }

            List<String> parents = nodeProps.getParents();

            boolean parentsLoaded = true;
            for (String parent : parents) {
                if (!loadedParents.contains(parent)) {
                    parentsLoaded = false;
                }
                if (false == parentsLoaded) { break; }
            }

            String name = nodeProps.getName();

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
        AptManagerImpl tbm = (AptManagerImpl)UvmContextFactory.context().aptManager();

        Map<NodeSettings, NodeProperties> nodePropertiesMap = new HashMap<NodeSettings, NodeProperties>(unloaded.size());

        for (NodeSettings nodeSettings : unloaded) {
            String name = nodeSettings.getNodeName();
            logger.debug("Getting mackage desc for: " + name);
            PackageDesc md = tbm.packageDesc(name);
            if (null == md) {
                logger.warn("could not get mackage desc for: " + name);
                continue;
            }

            nodeSettings.setNodeName(name);

            try {
                logger.debug("Initializing node properties for: " + name);
                NodeProperties nodeProperties = initNodeProperties( md );
                nodePropertiesMap.put(nodeSettings, nodeProperties);
            } catch (Exception exn) {
                logger.warn("NodeProperties could not be parsed", exn);
            }
        }

        return nodePropertiesMap;
    }

    private NodeManagerSettings loadSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NodeManagerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "nodes";

        try {
            readSettings = settingsManager.load( NodeManagerSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        }
        else {
            logger.debug("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        return this.settings;
    }

    private void initializeSettings()
    {
        logger.info("Initializing Settings...");

        NodeManagerSettings newSettings = new NodeManagerSettings();

        this._setSettings(newSettings);
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
     * @exception Exception the descriptor does not parse or
     * parent cannot be loaded.
     */
    private NodeProperties initNodeProperties( PackageDesc packageDesc ) throws Exception
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

    private Long getDefaultPolicyForNode(String nodeName) throws Exception
    {
        AptManager tbm = UvmContextFactory.context().aptManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);
        if (packageDesc == null)
            throw new Exception("Node named " + nodeName + " not found");
        if (PackageDesc.Type.SERVICE == packageDesc.getType()) {
            return null;
        } else {
            return 1L; /* XXX */
        }
    }

    private NodeSettings createNewNodeSettings( Long policyId, String nodeName )
    {
        long newNodeId = settings.getNextNodeId();

        /**
         * Increment the next node Id (not saved until later)
         */
        settings.setNextNodeId( newNodeId + 1 );
        
        return new NodeSettings( newNodeId, policyId, nodeName );
    }

    private void saveNewNodeSettings( NodeSettings nodeSettings )
    {
        List<NodeSettings> nodes = settings.getNodes();
        nodes.add(nodeSettings);
        _setSettings(settings);
        return;
    }
    
    private List<Long> getParentPolicies( Long policyId )
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

    private List<Node> getNodesForPolicy( Long policyId )
    {
        return getNodesForPolicy( policyId,true );
    }

    private List<Node> getNodesForPolicy( Long policyId, boolean parents )
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
        List<List<Node>> parentNodeArray = new ArrayList<List<Node>>(parentPolicies.size());
        List<Node> thisPolicyNodes = new ArrayList<Node>();
        for (int i = 0; i < parentPolicies.size(); i++) {
            parentNodeArray.add(new ArrayList<Node>());
        }

        /*
         * Fill in the inner list, at the end each of these is the list of 
         * nodes in the policy.
         */
        for (Node node : loadedNodesMap.values()) {
            Long nodePolicyId = node.getNodeSettings().getPolicyId();

            /**
             * If its in the parent policy list - add it
             * Otherwise it its in the policy - add it
             */
            int i = parentPolicies.indexOf(nodePolicyId);
            if (i >= 0) {
                parentNodeArray.get(i).add( node );
            } else if (nodePolicyId == null && policyId == null) {
                thisPolicyNodes.add( node );
            } else if (nodePolicyId != null && policyId != null && nodePolicyId.equals(policyId)) {
                thisPolicyNodes.add( node );
            }
        }

        /*
         * Add all the loadedNodesMap from the current policy
         * And all the nodes from the parent IFF they don't already exists
         * will only add the first entry (which will be most specific node.
         */
        List<Node> finalList = thisPolicyNodes;
        Set<String> names = new HashSet<String>();

        for (Node node : thisPolicyNodes) {
            String n = node.getNodeSettings().getNodeName();
            if (!names.contains(n))
                names.add(n);
        }
        for (List<Node> parentPolicyList : parentNodeArray) {
            if (parentPolicyList != null) {
                for (Node node : parentPolicyList) {
                    String n = node.getNodeSettings().getNodeName();
                    if (!names.contains(n)) {
                        names.add(n);
                        finalList.add( node );
                    }
                }
            }
        }

        return finalList;
    }

    private void _setSettings( NodeManagerSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(NodeManagerSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "nodes", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            throw new RuntimeException(e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    private List<Long> nodeToIdList( List<Node> nodes )
    {
        if ( nodes == null )
            return null;

        List<Long> idList = new ArrayList<Long>();

        for (Node node : nodes) {
            idList.add(node.getNodeSettings().getId());
        }

        return idList;
    }
}
