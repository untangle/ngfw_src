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
import java.util.stream.Collectors;
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
import com.untangle.uvm.node.AppManagerSettings;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.AppManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.AppProperties;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.vnet.NodeBase;

/**
 * Implements AppManager.
 */
public class AppManagerImpl implements AppManager
{
    private final static String APP_MANAGER_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/apps.js";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Stores a map of all currently loaded nodes from their nodeId to the Node instance
     */
    private final Map<Long, Node> loadedAppsMap = new ConcurrentHashMap<Long, Node>();

    /**
     * Stores a map of all yet to be loaded nodes from their nodeId to their AppSettings
     */
    private final Map<Long, AppSettings> unloadedAppsMap = new ConcurrentHashMap<Long, AppSettings>();

    /**
     * This stores the count of nodes currently being loaded
     */
    private volatile int appsBeingLoaded = 0;
    
    private AppManagerSettings settings = null;

    private Semaphore startSemaphore = new Semaphore(0);

    private boolean live = true;

    public AppManagerImpl() {}

    public AppManagerSettings getSettings()
    {
        return this.settings;
    }

    public void saveTargetState( Node node, AppSettings.AppState nodeState )
    {
        if ( node == null ) {
            logger.error("Invalid argument saveTargetState(): node is null");
            return;
        }
        if ( nodeState == null ) {
            logger.error("Invalid argument saveTargetState(): nodeState is null");
            return;
        }

        for ( AppSettings appSettings : this.settings.getApps() ) {
            if ( appSettings.getId() == node.getAppSettings().getId() ) {
                if ( nodeState != appSettings.getTargetState() ) {
                    appSettings.setTargetState(nodeState);
                } else {
                    logger.info("ignore saveTargetState(): already in state " + nodeState);
                }
            }
        }
        this._setSettings(this.settings);
    }

    public List<Node> appInstances()
    {
        List<Node> nodeList = new ArrayList<Node>( loadedAppsMap.values() );

        // sort by view position, for convenience
        Collections.sort(nodeList, new Comparator<Node>() {
            public int compare(Node tci1, Node tci2) {
                int rpi1 = tci1.getAppProperties().getViewPosition();
                int rpi2 = tci2.getAppProperties().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getAppProperties().getName().compareToIgnoreCase(tci2.getAppProperties().getName());
                } else if (rpi1 < rpi2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return nodeList;
    }

    public List<Node> nodeInstances()
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances();
    }
    
    public List<Long> appInstancesIds()
    {
        return appToIdList(appInstances());
    }

    public List<Long> nodeInstancesIds()
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstancesIds();
    }
    
    public List<Node> appInstances( String nodeName )
    {
        nodeName = fixupName( nodeName ); // handle old names
        
        List<Node> list = new LinkedList<Node>();

        for (Node node : loadedAppsMap.values()) {
            if ( node.getAppProperties().getName().equals( nodeName ) ) {
                list.add( node );
            }
        }

        return list;
    }

    public List<Node> nodeInstances( String appName )
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances( appName );
    }
    
    public List<Node> appInstances( String name, Integer policyId )
    {
        return appInstances( name, policyId, true);
    }

    public List<Node> nodeInstances( String name, Integer policyId )
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances( name, policyId, true );
    }
    
    public List<Node> appInstances( String name, Integer policyId, boolean parents )
    {
        name = fixupName( name ); // handle old names

        List<Node> list = new ArrayList<Node>(loadedAppsMap.size());

        for ( Node node : getAppsForPolicy( policyId, parents ) ) {
            String nodeName = node.getAppProperties().getName();

            if ( nodeName.equals( name ) ) {
                list.add( node );
            }
        }

        return list;
    }

    public List<Node> nodeInstances( String name, Integer policyId, boolean parents )
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances( name, policyId, parents );
    }
    
    public List<Node> appInstances( Integer policyId )
    {
        return getAppsForPolicy( policyId );
    }

    public List<Node> nodeInstances( Integer policyId )
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances( policyId );
    }
    
    public List<Long> appInstancesIds( Integer policyId )
    {
        return appToIdList( appInstances( policyId ) );
    }

    public List<Long> nodeInstancesIds( Integer policyId )
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstancesIds( policyId );
    }

    protected List<Node> visibleApps( Integer policyId )
    {
        List<Node> loadedNodes = appInstances();
        List<Node> list = new ArrayList<Node>(loadedNodes.size());

        for (Node node : getAppsForPolicy( policyId )) {
            if ( !node.getAppProperties().getInvisible() ) {
                list.add( node );
            }
        }

        for (Node node : getAppsForPolicy( null /* services */ )) {
            if ( !node.getAppProperties().getInvisible() ) {
                list.add( node );
            }
        }

        return list;
    }

    protected List<Node> visibleNodes( Integer policyId )
    {
        logger.warn("deprecated method called.", new Exception());
        return visibleApps( policyId );
    }

    public Node app( Long nodeId )
    {
        return loadedAppsMap.get( nodeId );
    }

    public Node node( Long nodeId )
    {
        logger.warn("deprecated method called.", new Exception());
        return app( nodeId );
    }

    public Node app( String name )
    {
        name = fixupName( name ); // handle old names

        List<Node> nodes = appInstances( name );
        if( nodes.size() > 0 ){
            return nodes.get(0);
        }
        return null;
    }

    public Node node( String name )
    {
        logger.warn("deprecated method called.", new Exception());
        return app( name );
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
        AppProperties appProperties = null;
        AppSettings appSettings = null;

        synchronized (this) {
            if (!live)
                throw new Exception("AppManager is shut down");

            logger.info("initializing node: " + nodeName);
            appProperties = getAppProperties( nodeName );

            if ( appProperties == null ) {
                logger.error("Missing node properties for " + nodeName);
                throw new Exception("Missing node properties for " + nodeName);
            }

            if ( ! checkArchitecture( appProperties.getSupportedArchitectures() ) ) {
                throw new Exception("Unsupported Architecture " + System.getProperty("os.arch"));
            }

            if ( appProperties.getMinimumMemory() != null ) {
                Long requiredMemory = appProperties.getMinimumMemory();
                Long actualMemory = UvmContextFactory.context().metricManager().getMemTotal();
                if ( actualMemory < requiredMemory ) {
                    float requiredGig = ((float)(requiredMemory/(1024*1024))) / (1024.0f);
                    float actualGig = ((float)(actualMemory/(1024*1024))) / (1024.0f);
                    String message = "This app requires more memory (required: " + (Math.round(10.0*requiredGig)/10.0) + "G actual: " + (Math.round(10.0*actualGig)/10.0) + "G)";
                    throw new Exception(message);
                }
            }
            
            if (appProperties.getType() == AppProperties.Type.SERVICE )
                policyId = null;

            if ( appInstances( nodeName, policyId, false ).size() >= 1 )
                throw new Exception("Too many instances of " + nodeName + " in policy " + policyId + ".");
            for ( AppSettings n2 : getSettings().getApps() ) {
                String nodeName1 = nodeName;
                String nodeName2 = n2.getAppName();
                Integer policyId1 = policyId;
                Integer policyId2 = n2.getPolicyId();
                /**
                 * If the node name and policies are equal, they are dupes
                 */
                if ( nodeName1.equals(nodeName2) && ( (policyId1 == policyId2) || ( policyId1 != null && policyId1.equals(policyId2) ) ) )
                     throw new Exception("Too many instances of " + nodeName + " in policy " + policyId + ".");
            }

            appSettings = createNewAppSettings( policyId, nodeName );

            /**
             * Check all the basics
             */
            if (appSettings == null)
                throw new Exception("Null appSettings: " + nodeName);
            if (appProperties == null)
                throw new Exception("Null appProperties: " + nodeName);

            node = NodeBase.loadClass( appProperties, appSettings, true );

            if (node != null) {
                loadedAppsMap.put( appSettings.getId(), node );
                saveNewAppSettings( appSettings );
            } else {
                logger.error( "Failed to initialize node: " + appProperties.getName() );
            }

        }

        /**
         * If AutoStart is true, go ahead and start node
         */
        if ( appProperties != null && appProperties.getAutoStart() ) {
            node.start();
        }

        // Full System GC so the JVM gives memory back
        UvmContextFactory.context().gc();
        
        return node;
    }

    public void destroy( Long nodeId ) throws Exception
    {
        destroy( app( nodeId ));
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
            loadedAppsMap.remove( node.getAppSettings().getId() );
            for (Iterator<AppSettings> iter = this.settings.getApps().iterator(); iter.hasNext();) {
                AppSettings appSettings = iter.next();
                if (appSettings.getId().equals(node.getAppSettings().getId()))
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

    public Map<Long, AppSettings.AppState> allAppStates()
    {
        HashMap<Long, AppSettings.AppState> result = new HashMap<Long, AppSettings.AppState>();
        for (Node node : loadedAppsMap.values()) {
            result.put(node.getAppSettings().getId(), node.getRunState());
        }

        return result;
    }

    public Map<Long, AppSettings.AppState> allNodeStates()
    {
        logger.warn("deprecated method called.", new Exception());
        return allAppStates();
    }
    
    public boolean isInstantiated( String nodeName )
    {
        return (this.app(nodeName) != null);
    }

    public Map<Long, AppSettings> getAllAppSettings()
    {
        HashMap<Long, AppSettings> result = new HashMap<Long, AppSettings>();
        for (Node node : loadedAppsMap.values()) {
            result.put(node.getAppSettings().getId(), node.getAppSettings());
        }
        return result;
    }

    public Map<Long, AppSettings> allNodeSettings()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppSettings();
    }
    
    public Map<Long, AppProperties> getAllAppPropertiesMap()
    {
        HashMap<Long, AppProperties> result = new HashMap<Long, AppProperties>();
        for (Node node : loadedAppsMap.values()) {
            result.put(node.getAppSettings().getId(), node.getAppProperties());
        }
        return result;
    }

    public Map<Long, AppProperties> allNodeProperties()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppPropertiesMap();
    }
    
    public List<AppProperties> getAllAppProperties()
    {
        LinkedList<AppProperties> nodeProps = new LinkedList<AppProperties>();

        File rootDir = new File( System.getProperty( "uvm.lib.dir" ) );

        findAllAppProperties( nodeProps, rootDir );

        return nodeProps;
    }

    public List<AppProperties> getAllNodeProperties()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppProperties();
    }
    
    public AppsView getAppsView( Integer policyId )
    {
        AppManagerImpl nm = (AppManagerImpl)UvmContextFactory.context().appManager();
        LicenseManager lm = UvmContextFactory.context().licenseManager();

        /* This stores a list of installable nodes. (for this rack) */
        Map<String, AppProperties> installableNodesMap =  new HashMap<String, AppProperties>();
        /* This stores a list of all licenses */
        Map<String, License> licenseMap = new HashMap<String, License>();

        /**
         * Build the license map
         */
        List<Node> visibleNodes = nm.visibleApps( policyId );
        for (Node node : visibleNodes) {
            String n = node.getAppProperties().getName();
            licenseMap.put(n, lm.getLicense(n));
        }

        /**
         * Build the rack state
         */
        Map<Long, AppSettings.AppState> runStates = nm.allAppStates();

        /**
         * Iterate through nodes
         */
        for ( AppProperties nodeProps : nm.getAllAppProperties() ) {
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
            Long nodeId = visibleNode.getAppSettings().getId();
            Integer nodePolicyId = visibleNode.getAppSettings().getPolicyId();
            nodeMetrics.put( nodeId , visibleNode.getMetrics());

            if ( nodePolicyId == null || nodePolicyId.equals( policyId ) ) {
                installableNodesMap.remove( visibleNode.getAppProperties().getDisplayName() );
            }
        }

        /**
         * SPECIAL CASE: Web Filter Lite is being deprecated - hide it
         */
        installableNodesMap.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */

        /**
         * SPECIAL CASE: If Web Filter is installed in this rack OR licensed for non-trial, hide Web Monitor
         */
        List<Node> webFilterNodes = UvmContextFactory.context().appManager().appInstances( "web-filter", policyId );
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
        List<Node> spamBlockerNodes = UvmContextFactory.context().appManager().appInstances( "spam-blocker", policyId);
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
        List<AppProperties> installableNodes = new ArrayList<AppProperties>(installableNodesMap.values());
        Collections.sort( installableNodes );

        List<AppProperties> appProperties = new LinkedList<AppProperties>();
        for (Node node : visibleNodes) {
            appProperties.add(node.getAppProperties());
        }
        List<AppSettings> appSettings  = new LinkedList<AppSettings>();
        for (Node node : visibleNodes) {
            appSettings.add(node.getAppSettings());
        }

        return new AppsView(policyId, installableNodes, appSettings, appProperties, nodeMetrics, licenseMap, runStates);
    }

    public AppsView[] getAppsViews()
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");

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
            for ( Node node : loadedAppsMap.values() ) {
                logger.info( node.getAppSettings().getId() + " " + node.getAppSettings().getAppName() );
            }
        }
        
        startAutoLoad();

        logger.info("Initialized AppManager");
    }

    protected synchronized void destroy()
    {
        List<Runnable> tasks = new ArrayList<Runnable>();

        for ( final Node node : loadedAppsMap.values() ) {
            Runnable r = new Runnable() {
                    public void run()
                    {
                        String name = node.getAppProperties().getName();
                        Long id = node.getAppSettings().getId();

                        logger.info("Stopping  : " + name + " (" + id + ")");

                        long startTime = System.currentTimeMillis();
                        ((NodeBase)node).stopIfRunning( );
                        long endTime = System.currentTimeMillis();

                        logger.info("Stopped   : " + name + " (" + id + ") [" + ( ((float)(endTime - startTime))/1000.0f ) + " seconds]");

                        loadedAppsMap.remove( node.getAppSettings().getId() );
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

        logger.info("AppManager destroyed");
    }

    protected void startAutoLoad()
    {
        for ( AppProperties nodeProps : getAllAppProperties() ) {
            if (! nodeProps.getAutoLoad() )
                continue;

            List<Node> list = appInstances( nodeProps.getName() );

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

    private void findAllAppProperties( List<AppProperties> nodeProps, File searchDir )
    {
        if ( ! searchDir.exists() )
            return;

        File[] fileList = searchDir.listFiles();
        if ( fileList == null )
            return;

        for ( File f : fileList ) {
            if ( f.isDirectory() ) {
                findAllAppProperties( nodeProps, f );
            } else {
                if ( "appProperties.js".equals( f.getName() ) ) {
                    try {
                        AppProperties np = getAppPropertiesFilename( f.getAbsolutePath() );
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
            throw new RuntimeException("AppManager is shut down");
        }

        logger.info("Restarting unloaded nodes...");

        for (AppSettings appSettings : settings.getApps()) {
            unloadedAppsMap.put( appSettings.getId(), appSettings );
        }

        while ( unloadedAppsMap.size() > 0 || appsBeingLoaded > 0 ) {
            passCount++;
            List<AppSettings> loadable = getLoadable();

            if ( loadable.size() > 0 ) {
                for (AppSettings ns : loadable)
                    logger.info("Loading in this pass[" + passCount + "]: " + ns.getAppName() + " (" + ns.getId() + ")");
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

    private void startUnloaded(List<AppSettings> startQueue )
    {
        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (final AppSettings appSettings : startQueue) {
            final String name = appSettings.getAppName();
            final AppProperties nodeProps = getAppProperties(appSettings);

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
                                logger.info("Restarting: " + name + " (" + appSettings.getId() + ")");
                                long startTime = System.currentTimeMillis();
                                node = (NodeBase) NodeBase.loadClass(nodeProps, appSettings, false);
                                long endTime   = System.currentTimeMillis();
                                logger.info("Restarted : " + name + " (" + appSettings.getId() + ") [" + ( ((float)(endTime - startTime))/1000.0f ) + " seconds]");

                                // add to loaded nodes
                                loadedAppsMap.put( appSettings.getId(), node );

                            } catch (Exception exn) {
                                logger.error("Could not restart: " + name, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + name, err);
                            } finally {

                                // alert the main thread that a node is done loading
                                appsBeingLoaded--;
                                startSemaphore.release();

                            }
                            if ( node == null ) {
                                logger.error("Failed to load node:" + name);
                                loadedAppsMap.remove(appSettings);
                            } 
                        }
                    };
                // remove from unloaded nodes 
                appsBeingLoaded++;
                unloadedAppsMap.remove( appSettings.getId() );

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

    private List<AppSettings> getLoadable()
    {
        List<AppSettings> loadable = new ArrayList<AppSettings>(unloadedAppsMap.size());
        Set<String> thisPass = new HashSet<String>(unloadedAppsMap.size());

        for (Iterator<AppSettings> i = unloadedAppsMap.values().iterator(); i.hasNext(); ) {
            AppSettings appSettings = i.next();
            if ( appSettings == null ) {
                logger.error("Invalid settings: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
                continue;
            }
            String name = appSettings.getAppName();
            if ( name == null ) {
                logger.error("Missing name for: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
                continue;
            }
            AppProperties nodeProps = getAppProperties( name );
            if ( nodeProps == null ) {
                logger.error("Missing properties for: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
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
                loadable.add(appSettings);
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
        
        for( Node n : loadedAppsMap.values() ) {
            String name = n.getAppSettings().getAppName();
            if ( nodeName.equals( name ) ) {
                return true;
            }
        }

        return false;
    }

    private AppManagerSettings loadSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AppManagerSettings readSettings = null;
        String settingsFileName = APP_MANAGER_SETTINGS_FILE;

        try {
            readSettings = settingsManager.load( AppManagerSettings.class, settingsFileName );
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
            LinkedList<AppSettings> cleanList = new LinkedList<AppSettings>();
            for (AppSettings item : readSettings.getApps()) {
                if (item.getAppName().equals("webfilter-lite")) continue;
                if (item.getAppName().equals("ips")) continue;
                if (item.getAppName().equals("idps")) continue;
                cleanList.add(item);
            }

            // if we removed anything update the node list and save
            if (cleanList.size() != readSettings.getApps().size()) {
                readSettings.setApps(cleanList);
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

        AppManagerSettings newSettings = new AppManagerSettings();
        
        this._setSettings(newSettings);
    }

    /**
     * Get AppProperties from the node settings
     */
    private AppProperties getAppProperties( AppSettings appSettings )
    {
        return getAppProperties( appSettings.getAppName() );
    }

    /**
     * Get AppProperties from the node name (ie "firewall")
     */
    private AppProperties getAppProperties( String name )
    {
        String fileName = System.getProperty("uvm.lib.dir") + "/" + name + "/" + "appProperties.js";
        return getAppPropertiesFilename( fileName );
    }

    /**
     * Get AppProperties from the full path file name
     */
    private AppProperties getAppPropertiesFilename( String fileName )
    {
        AppProperties appProperties = null;

        try {
            appProperties = UvmContextFactory.context().settingsManager().load( AppProperties.class, fileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        return appProperties;
    }

    private AppSettings createNewAppSettings( Integer policyId, String nodeName )
    {
        long newAppId = settings.getNextAppId();

        /**
         * Increment the next node Id (not saved until later)
         */
        settings.setNextAppId( newAppId + 1 );

        return new AppSettings( newAppId, policyId, nodeName );
    }

    private void saveNewAppSettings( AppSettings appSettings )
    {
        List<AppSettings> apps = settings.getApps();
        apps.add(appSettings);
        _setSettings(settings);
        return;
    }

    private List<Integer> getParentPolicies( Integer policyId )
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");
        List<Integer> parentList = new ArrayList<Integer>();
        if (policyManager == null)
            return parentList;

        for ( Integer parentId = policyManager.getParentPolicyId(policyId) ; parentId != null ; parentId = policyManager.getParentPolicyId(parentId) ) {
            parentList.add(parentId);
        }

        return parentList;
    }

    private List<Node> getAppsForPolicy( Integer policyId )
    {
        return getAppsForPolicy( policyId, true );
    }

    private List<Node> getAppsForPolicy( Integer policyId, boolean parents )
    {
        List<Integer> parentPolicies = null;

        if (parents && policyId != null)
            parentPolicies = getParentPolicies(policyId);
        else
            parentPolicies = new ArrayList<Integer>();

        /*
         * This is a list of loadedAppsMap.  Each index of the first list corresponds to its
         * policy in the policies array.  Each index in the second list is a appSettings of the nodes
         * in the policy
         * parentAppSettingsArray[0] == list of loadedAppsMap in parentPolicies[0]
         * parentAppSettingsArray[1] == list of loadedAppsMap in parentPolicies[1]
         * ...
         * parentAppSettingsArray[n] == list of loadedAppsMap in parentPolicies[n]
         * Policies are ordered parentAppSettingsArray[0] is the first parent, etc
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
        for (Node node : loadedAppsMap.values()) {
            Integer nodePolicyId = node.getAppSettings().getPolicyId();

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
         * Add all the loadedAppsMap from the current policy
         * And all the nodes from the parent IFF they don't already exists
         * will only add the first entry (which will be most specific node.
         */
        List<Node> finalList = thisPolicyNodes;
        Set<String> names = new HashSet<String>();

        for (Node node : thisPolicyNodes) {
            String n = node.getAppSettings().getAppName();
            if (!names.contains(n))
                names.add(n);
        }
        for (List<Node> parentPolicyList : parentNodeArray) {
            if (parentPolicyList != null) {
                for (Node node : parentPolicyList) {
                    String n = node.getAppSettings().getAppName();
                    if (!names.contains(n)) {
                        names.add(n);
                        finalList.add( node );
                    }
                }
            }
        }

        return finalList;
    }

    private synchronized void _setSettings( AppManagerSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( APP_MANAGER_SETTINGS_FILE, newSettings );
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

    private List<Long> appToIdList( List<Node> apps )
    {
        if ( apps == null ) return null;
        List<Long> idList = apps.stream().map( a -> a.getAppSettings().getId() ).collect(Collectors.toCollection(ArrayList::new));
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
