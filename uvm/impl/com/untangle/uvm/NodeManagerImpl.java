/*
 * $Id$
 */
package com.untangle.uvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
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
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetcapManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeManagerSettings;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.vnet.NodeBase;

/**
 * Implements NodeManager.
 */
public class NodeManagerImpl implements NodeManager
{
    private final static String NODE_MANAGER_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/nodes.js";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Stores a map of all currently loaded nodes from their nodeId to the Node instance
     */
    private final Map<Long, Node> loadedNodesMap = new ConcurrentHashMap<Long, Node>();

    /**
     * Stores a map of all yet to be loaded nodes from their nodeId to their NodeSettings
     */
    private final Map<Long, NodeSettings> unloadedNodesMap = new ConcurrentHashMap<Long, NodeSettings>();

    /**
     * This stores the count of nodes currently being loaded
     */
    private volatile int nodesBeingLoaded = 0;
    
    private NodeManagerSettings settings = null;

    private Semaphore startSemaphore = new Semaphore(0);

    private boolean live = true;

    public NodeManagerImpl() {}

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
                int rpi1 = tci1.getNodeProperties().getViewPosition();
                int rpi2 = tci2.getNodeProperties().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getNodeProperties().getName().compareToIgnoreCase(tci2.getNodeProperties().getName());
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
        nodeName = fixupName( nodeName ); // handle old names
        
        List<Node> list = new LinkedList<Node>();

        for (Node node : loadedNodesMap.values()) {
            if ( node.getNodeProperties().getName().equals( nodeName ) ) {
                list.add( node );
            }
        }

        return list;
    }

    public List<Node> nodeInstances( String name, Integer policyId )
    {
        return nodeInstances( name, policyId, true);
    }

    public List<Node> nodeInstances( String name, Integer policyId, boolean parents )
    {
        name = fixupName( name ); // handle old names

        List<Node> list = new ArrayList<Node>(loadedNodesMap.size());

        for ( Node node : getNodesForPolicy( policyId, parents ) ) {
            String nodeName = node.getNodeProperties().getName();

            if ( nodeName.equals( name ) ) {
                list.add( node );
            }
        }

        return list;
    }

    public List<Node> nodeInstances( Integer policyId )
    {
        return getNodesForPolicy( policyId );
    }

    public List<Long> nodeInstancesIds( Integer policyId )
    {
        return nodeToIdList( nodeInstances( policyId ) );
    }

    protected List<Node> visibleNodes( Integer policyId )
    {
        List<Node> loadedNodes = nodeInstances();
        List<Node> list = new ArrayList<Node>(loadedNodes.size());

        for (Node node : getNodesForPolicy( policyId )) {
            if ( !node.getNodeProperties().getInvisible() ) {
                list.add( node );
            }
        }

        for (Node node : getNodesForPolicy( null /* services */ )) {
            if ( !node.getNodeProperties().getInvisible() ) {
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
        name = fixupName( name ); // handle old names

        List<Node> nodes = nodeInstances( name );
        if( nodes.size() > 0 ){
            return nodes.get(0);
        }
        return null;
    }

    public Node instantiate( String nodeName ) throws Exception
    {
        return instantiate( nodeName, 1 /* Default Policy ID */ );
    }

    public Node instantiate( String nodeName, Integer policyId ) throws Exception
    {
        nodeName = fixupName( nodeName ); // handle old names

        logger.info("instantiate( name:" + nodeName + " , policy:" + policyId + " )");

        if ( ! UvmContextFactory.context().licenseManager().isLicenseValid( nodeName ) ) {
            logger.info( "No valid license for: " + nodeName );
            logger.info( "Requesting trial for: " + nodeName );
            UvmContextFactory.context().licenseManager().requestTrialLicense( nodeName );
        }

        Node node = null;
        NodeProperties nodeProperties = null;
        NodeSettings nodeSettings = null;

        synchronized (this) {
            if (!live)
                throw new Exception("NodeManager is shut down");

            logger.info("initializing node: " + nodeName);
            nodeProperties = getNodeProperties( nodeName );

            if ( nodeProperties == null ) {
                logger.error("Missing node properties for " + nodeName);
                throw new Exception("Missing node properties for " + nodeName);
            }

            if ( ! checkArchitecture( nodeProperties.getSupportedArchitectures() ) ) {
                throw new Exception("Unsupported Architecture " + System.getProperty("os.arch"));
            }

            if ( nodeProperties.getMinimumMemory() != null ) {
                Long requiredMemory = nodeProperties.getMinimumMemory();
                Long actualMemory = UvmContextFactory.context().metricManager().getMemTotal();
                if ( actualMemory < requiredMemory ) {
                    float requiredGig = ((float)(requiredMemory/(1024*1024))) / (1024.0f);
                    float actualGig = ((float)(actualMemory/(1024*1024))) / (1024.0f);
                    String message = "This app requires more memory (required: " + (Math.round(10.0*requiredGig)/10.0) + "G actual: " + (Math.round(10.0*actualGig)/10.0) + "G)";
                    throw new Exception(message);
                }
            }
            
            if (nodeProperties.getType() == NodeProperties.Type.SERVICE )
                policyId = null;

            if ( nodeInstances( nodeName, policyId, false ).size() >= 1 )
                throw new Exception("Too many instances of " + nodeName + " in policy " + policyId + ".");
            for ( NodeSettings n2 : getSettings().getNodes() ) {
                String nodeName1 = nodeName;
                String nodeName2 = n2.getNodeName();
                Integer policyId1 = policyId;
                Integer policyId2 = n2.getPolicyId();
                /**
                 * If the node name and policies are equal, they are dupes
                 */
                if ( nodeName1.equals(nodeName2) && ( (policyId1 == policyId2) || ( policyId1 != null && policyId1.equals(policyId2) ) ) )
                     throw new Exception("Too many instances of " + nodeName + " in policy " + policyId + ".");
            }

            nodeSettings = createNewNodeSettings( policyId, nodeName );

            /**
             * Check all the basics
             */
            if (nodeSettings == null)
                throw new Exception("Null nodeSettings: " + nodeName);
            if (nodeProperties == null)
                throw new Exception("Null nodeProperties: " + nodeName);

            node = NodeBase.loadClass( nodeProperties, nodeSettings, true );

            if (node != null) {
                loadedNodesMap.put( nodeSettings.getId(), node );
                saveNewNodeSettings( nodeSettings );
            } else {
                logger.error( "Failed to initialize node: " + nodeProperties.getName() );
            }

        }

        /**
         * If AutoStart is true, go ahead and start node
         */
        if ( nodeProperties != null && nodeProperties.getAutoStart() ) {
            node.start();
        }

        // Full System GC so the JVM gives memory back
        UvmContextFactory.context().gc();
        
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


        // Full System GC so the JVM gives memory back for unloaded memory/classes
        // This is necessary because the G1 does not actually account for MaxHeapFreeRatio
        // except during an full GC.
        UvmContextFactory.context().gc();
        
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

    public List<NodeProperties> getAllNodeProperties()
    {
        LinkedList<NodeProperties> nodeProps = new LinkedList<NodeProperties>();

        File rootDir = new File( System.getProperty( "uvm.lib.dir" ) );

        findAllNodeProperties( nodeProps, rootDir );

        return nodeProps;
    }

    public AppsView getAppsView( Integer policyId )
    {
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        LicenseManager lm = UvmContextFactory.context().licenseManager();

        /* This stores a list of installable nodes. (for this rack) */
        Map<String, NodeProperties> installableNodesMap =  new HashMap<String, NodeProperties>();
        /* This stores a list of all licenses */
        Map<String, License> licenseMap = new HashMap<String, License>();

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
        Map<Long, NodeSettings.NodeState> runStates = nm.allNodeStates();

        /**
         * Iterate through nodes
         */
        for ( NodeProperties nodeProps : nm.getAllNodeProperties() ) {
            if ( nodeProps.getInvisible() )
                continue;

            if ( ! checkArchitecture( nodeProps.getSupportedArchitectures() ) ) {
                logger.debug("Hiding " + nodeProps.getDisplayName() + ". " + System.getProperty("os.arch") + " is not a supported architecture.");
                continue;
            }

            installableNodesMap.put( nodeProps.getDisplayName(), nodeProps );
        }

        /**
         * Build the nodeMetrics (stats in the UI)
         * Remove visible installableNodes from installableNodes
         */
        Map<Long, List<NodeMetric>> nodeMetrics = new HashMap<Long, List<NodeMetric>>(visibleNodes.size());
        for (Node visibleNode : visibleNodes) {
            Long nodeId = visibleNode.getNodeSettings().getId();
            Integer nodePolicyId = visibleNode.getNodeSettings().getPolicyId();
            nodeMetrics.put( nodeId , visibleNode.getMetrics());

            if ( nodePolicyId == null || nodePolicyId.equals( policyId ) ) {
                installableNodesMap.remove( visibleNode.getNodeProperties().getDisplayName() );
            }
        }

        /**
         * SPECIAL CASE: Web Filter Lite is being deprecated - hide it
         */
        installableNodesMap.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */

        /**
         * SPECIAL CASE: If Web Filter is installed in this rack OR licensed for non-trial, hide Web Monitor
         */
        List<Node> webFilterNodes = UvmContextFactory.context().nodeManager().nodeInstances( "web-filter", policyId );
        if (webFilterNodes != null && webFilterNodes.size() > 0) {
            installableNodesMap.remove("Web Monitor"); /* hide web monitor from left hand nav */
        }
        if ( ! UvmContextFactory.context().isDevel() ) {
            License webFilterLicense = lm.getLicense(License.WEB_FILTER);
            if ( webFilterLicense != null && webFilterLicense.getValid() && !webFilterLicense.getTrial() ) {
                installableNodesMap.remove("Web Monitor"); /* hide web monitor from left hand nav */
            }
        }

        /**
         * SPECIAL CASE: If Spam Blocker is installed in this rack OR licensed for non-trial, hide Spam Blocker Lite
         */
        List<Node> spamBlockerNodes = UvmContextFactory.context().nodeManager().nodeInstances( "spam-blocker", policyId);
        if (spamBlockerNodes != null && spamBlockerNodes.size() > 0) {
            installableNodesMap.remove("Spam Blocker Lite"); /* hide spam blocker lite from left hand nav */
        }
        if ( ! UvmContextFactory.context().isDevel() ) {
            License spamBlockerLicense = lm.getLicense(License.SPAM_BLOCKER);
            if ( spamBlockerLicense != null && spamBlockerLicense.getValid() && !spamBlockerLicense.getTrial() ) {
                installableNodesMap.remove("Spam Blocker Lite"); /* hide spam blocker lite from left hand nav */
            }
        }


        /**
         * Build the list of apps to show on the left hand nav
         */
        logger.debug("Building apps panel:");
        List<NodeProperties> installableNodes = new ArrayList<NodeProperties>(installableNodesMap.values());
        Collections.sort( installableNodes );

        List<NodeProperties> nodeProperties = new LinkedList<NodeProperties>();
        for (Node node : visibleNodes) {
            nodeProperties.add(node.getNodeProperties());
        }
        List<NodeSettings> nodeSettings  = new LinkedList<NodeSettings>();
        for (Node node : visibleNodes) {
            nodeSettings.add(node.getNodeSettings());
        }

        return new AppsView(policyId, installableNodes, nodeSettings, nodeProperties, nodeMetrics, licenseMap, runStates);
    }

    public AppsView[] getAppsViews()
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("policy-manager");

        if ( policyManager == null ) {
            AppsView[] views = new AppsView[] { getAppsView(1) };
            return views;
        }

        int policyIds[] = policyManager.getPolicyIds();
        AppsView[] views = new AppsView[policyIds.length];

        for ( int i = 0 ; i < policyIds.length ; i++ ) {
            views[i] = getAppsView(policyIds[i]);
        }

        return views;
    }
    
    protected void init()
    {
        loadSettings();

        restartUnloaded();

        if ( logger.isDebugEnabled() ) {
            logger.debug("Fininshed restarting nodes:");
            for ( Node node : loadedNodesMap.values() ) {
                logger.info( node.getNodeSettings().getId() + " " + node.getNodeSettings().getNodeName() );
            }
        }
        
        startAutoLoad();

        logger.info("Initialized NodeManager");
    }

    protected synchronized void destroy()
    {
        List<Runnable> tasks = new ArrayList<Runnable>();

        for ( final Node node : loadedNodesMap.values() ) {
            Runnable r = new Runnable() {
                    public void run()
                    {
                        String name = node.getNodeProperties().getName();
                        Long id = node.getNodeSettings().getId();

                        logger.info("Stopping  : " + name + " (" + id + ")");

                        long startTime = System.currentTimeMillis();
                        ((NodeBase)node).stopIfRunning( );
                        long endTime = System.currentTimeMillis();

                        logger.info("Stopped   : " + name + " (" + id + ") [" + ( ((float)(endTime - startTime))/1000.0f ) + " seconds]");

                        loadedNodesMap.remove( node.getNodeSettings().getId() );
                    }
                };
            tasks.add(r);
        }

        List<Thread> threads = new ArrayList<Thread>(tasks.size());
        try {
            for (Iterator<Runnable> taskIterator = tasks.iterator() ; taskIterator.hasNext() ; ) {
                Thread t = UvmContextFactory.context().newThread(taskIterator.next(), "STOP_THREAD");
                threads.add(t);
                t.start();
            }
            // Must wait for them to start before we can go on to next wave.
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException exn) {
            logger.error("Interrupted while starting nodes");
        }

        logger.info("NodeManager destroyed");
    }

    protected void startAutoLoad()
    {
        for ( NodeProperties nodeProps : getAllNodeProperties() ) {
            if (! nodeProps.getAutoLoad() )
                continue;

            List<Node> list = nodeInstances( nodeProps.getName() );

            /**
             * If a node is "autoLoad" and is not loaded, instantiate it
             */
            if ( list.size() == 0 ) {
                try {
                    logger.info("Auto-loading new node: " + nodeProps.getName());
                    Node node = instantiate( nodeProps.getName() );

                    if ( nodeProps.getAutoStart() ) {
                        node.start();
                    }

                } catch (Exception exn) {
                    logger.warn("could not deploy: " + nodeProps.getName(), exn);
                    continue;
                }
            }
        }
    }

    private void findAllNodeProperties( List<NodeProperties> nodeProps, File searchDir )
    {
        if ( ! searchDir.exists() )
            return;

        File[] fileList = searchDir.listFiles();
        if ( fileList == null )
            return;

        for ( File f : fileList ) {
            if ( f.isDirectory() ) {
                findAllNodeProperties( nodeProps, f );
            } else {
                if ( "nodeProperties.js".equals( f.getName() ) ) {
                    try {
                        NodeProperties np = getNodePropertiesFilename( f.getAbsolutePath() );
                        nodeProps.add( np );
                    } catch (Exception e) {
                        logger.warn("Ignoring bad node properties: " + f.getAbsolutePath(), e);
                    }
                }
            }

        }
    }

    private void restartUnloaded()
    {
        long t0 = System.currentTimeMillis();
        int passCount = 0;
        
        if (!live) {
            throw new RuntimeException("NodeManager is shut down");
        }

        logger.info("Restarting unloaded nodes...");

        for (NodeSettings nSettings : settings.getNodes()) {
            unloadedNodesMap.put( nSettings.getId(), nSettings );
        }

        while ( unloadedNodesMap.size() > 0 || nodesBeingLoaded > 0 ) {
            passCount++;
            List<NodeSettings> loadable = getLoadable();

            if ( loadable.size() > 0 ) {
                for (NodeSettings ns : loadable)
                    logger.info("Loading in this pass[" + passCount + "]: " + ns.getNodeName() + " (" + ns.getId() + ")");
                startUnloaded( loadable );
            }

            logger.debug("Completing pass[" + passCount + "]");
            do {
                try { startSemaphore.acquire(); } catch (InterruptedException e) { continue; }
                break;
            } while (true);
        }

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart nodes: " + (t1 - t0) + " millis");
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodeSettings> startQueue )
    {
        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (final NodeSettings nodeSettings : startQueue) {
            final String name = nodeSettings.getNodeName();
            final NodeProperties nodeProps = getNodeProperties(nodeSettings);

            if ( name == null ) {
                logger.error("Unable to load node \"" + name + "\": NULL name.");
            } else if ( nodeProps == null ) {
                logger.error("Unable to load node \"" + name + "\": NULL node properties.");
            } else {
                Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            NodeBase node = null;
                            try {
                                logger.info("Restarting: " + name + " (" + nodeSettings.getId() + ")");
                                long startTime = System.currentTimeMillis();
                                node = (NodeBase) NodeBase.loadClass(nodeProps, nodeSettings, false);
                                long endTime   = System.currentTimeMillis();
                                logger.info("Restarted : " + name + " (" + nodeSettings.getId() + ") [" + ( ((float)(endTime - startTime))/1000.0f ) + " seconds]");

                                // add to loaded nodes
                                loadedNodesMap.put( nodeSettings.getId(), node );

                            } catch (Exception exn) {
                                logger.error("Could not restart: " + name, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + name, err);
                            } finally {

                                // alert the main thread that a node is done loading
                                nodesBeingLoaded--;
                                startSemaphore.release();

                            }
                            if ( node == null ) {
                                logger.error("Failed to load node:" + name);
                                loadedNodesMap.remove(nodeSettings);
                            } 
                        }
                    };
                // remove from unloaded nodes 
                nodesBeingLoaded++;
                unloadedNodesMap.remove( nodeSettings.getId() );

                restarters.add(r);
            }
        }

        List<Thread> threads = new ArrayList<Thread>(restarters.size());

        for (Iterator<Runnable> iter = restarters.iterator(); iter.hasNext();) {
            Thread t = UvmContextFactory.context().newThread(iter.next(), "START_" + startThreadNum++);
            threads.add(t);
            t.start();
        }
    }

    private List<NodeSettings> getLoadable()
    {
        List<NodeSettings> loadable = new ArrayList<NodeSettings>(unloadedNodesMap.size());
        Set<String> thisPass = new HashSet<String>(unloadedNodesMap.size());

        for (Iterator<NodeSettings> i = unloadedNodesMap.values().iterator(); i.hasNext(); ) {
            NodeSettings nodeSettings = i.next();
            if ( nodeSettings == null ) {
                logger.error("Invalid settings: " + nodeSettings);
                i.remove(); // remove from unloadedNodesMap because we can never load this one
                continue;
            }
            String name = nodeSettings.getNodeName();
            if ( name == null ) {
                logger.error("Missing name for: " + nodeSettings);
                i.remove(); // remove from unloadedNodesMap because we can never load this one
                continue;
            }
            NodeProperties nodeProps = getNodeProperties( name );
            if ( nodeProps == null ) {
                logger.error("Missing properties for: " + nodeSettings);
                i.remove(); // remove from unloadedNodesMap because we can never load this one
                continue;
            }

            List<String> parents = nodeProps.getParents();
            boolean parentsLoaded = true;
            if ( parents != null ) {
                for (String parent : parents) {
                    if (!isLoaded( parent )) {
                        parentsLoaded = false;
                        break;
                    }
                }
            }

            // all parents loaded and another instance of this
            // node not loading this pass or already loaded in
            // previous pass (prevents classloader race).
            if (parentsLoaded && !thisPass.contains(name)) {
                loadable.add(nodeSettings);
                thisPass.add(name);
            }
        }

        return loadable;
    }

    private boolean isLoaded( String nodeName )
    {
        if ( nodeName == null ) {
            logger.warn("Invalid arguments");
            return false;
        }
        
        for( Node n : loadedNodesMap.values() ) {
            String name = n.getNodeSettings().getNodeName();
            if ( nodeName.equals( name ) ) {
                return true;
            }
        }

        return false;
    }

    private NodeManagerSettings loadSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NodeManagerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "nodes.js";

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

            // look for and remove old nodes that no longer exist
            LinkedList<NodeSettings> cleanList = new LinkedList<NodeSettings>();
            for (NodeSettings item : readSettings.getNodes()) {
                if (item.getNodeName().equals("webfilter-lite")) continue;
                if (item.getNodeName().equals("ips")) continue;
                if (item.getNodeName().equals("idps")) continue;
                cleanList.add(item);
            }

            // if we removed anything update the node list and save
            if (cleanList.size() != readSettings.getNodes().size()) {
                readSettings.setNodes(cleanList);
                this._setSettings(readSettings);
            }

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
     * Get NodeProperties from the node settings
     */
    private NodeProperties getNodeProperties( NodeSettings nodeSettings )
    {
        return getNodeProperties( nodeSettings.getNodeName() );
    }

    /**
     * Get NodeProperties from the node name (ie "firewall")
     */
    private NodeProperties getNodeProperties( String name )
    {
        String fileName = System.getProperty("uvm.lib.dir") + "/" + name + "/" + "nodeProperties.js";
        return getNodePropertiesFilename( fileName );
    }

    /**
     * Get NodeProperties from the full path file name
     */
    private NodeProperties getNodePropertiesFilename( String fileName )
    {
        NodeProperties nodeProperties = null;

        try {
            nodeProperties = UvmContextFactory.context().settingsManager().load( NodeProperties.class, fileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        return nodeProperties;
    }

    private NodeSettings createNewNodeSettings( Integer policyId, String nodeName )
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

    private List<Integer> getParentPolicies( Integer policyId )
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("policy-manager");
        List<Integer> parentList = new ArrayList<Integer>();
        if (policyManager == null)
            return parentList;

        for ( Integer parentId = policyManager.getParentPolicyId(policyId) ; parentId != null ; parentId = policyManager.getParentPolicyId(parentId) ) {
            parentList.add(parentId);
        }

        return parentList;
    }

    private List<Node> getNodesForPolicy( Integer policyId )
    {
        return getNodesForPolicy( policyId, true );
    }

    private List<Node> getNodesForPolicy( Integer policyId, boolean parents )
    {
        List<Integer> parentPolicies = null;

        if (parents && policyId != null)
            parentPolicies = getParentPolicies(policyId);
        else
            parentPolicies = new ArrayList<Integer>();

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
            Integer nodePolicyId = node.getNodeSettings().getPolicyId();

            /**
             * If its in the parent policy list - add it
             * Otherwise it its in the policy - add it
             */
            int i = parentPolicies.indexOf(nodePolicyId);
            if (i >= 0) {
                parentNodeArray.get(i).add( node );
            } else if ( nodePolicyId == null && policyId == null ) {
                thisPolicyNodes.add( node );
            } else if ( nodePolicyId != null && policyId != null && nodePolicyId.equals(policyId) ) {
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

    private synchronized void _setSettings( NodeManagerSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "nodes.js", newSettings );
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

    private boolean policyEquals( Integer policyId1, Integer policyId2 )
    {
        return ( (policyId1 == policyId2) || ( policyId1 != null && policyId1.equals(policyId2) ) );
    }

    private boolean checkArchitecture( List<String> supportedArchitectures )
    {
        boolean foundArch = false;
        String arch = System.getProperty("os.arch");
        
        if ( supportedArchitectures == null )
            return true;

        for ( String supportedArchitecture : supportedArchitectures ) {
            if ( "any".equals(supportedArchitecture) ) {
                return true;
            }
            if ( arch.equals(supportedArchitecture) ) {
                return true;
            }
        }
        return false;
    }

    private String fixupName( String name )
    {
        if ( name == null )
            return null;
        name = name.replaceAll("untangle-node-","").replaceAll("untangle-casing-","");
        if ( name.contains("untangle-base") )
            name = name.replaceAll("untangle-base-","") + "-base";
        return name;
    }
}
