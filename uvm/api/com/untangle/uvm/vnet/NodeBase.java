/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.MetricManager;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.AppSettings.AppState;
import com.untangle.uvm.node.AppManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.AppProperties;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.logging.LogEvent;

/**
 * A base class for node instances, both normal and casing.
 *
 */
public abstract class NodeBase implements Node
{
    private static final Logger staticLogger = Logger.getLogger(NodeBase.class);
    private        final Logger logger       = Logger.getLogger(NodeBase.class);

    /**
     * These are the (generic) settings for this node
     * The node usually stores more app-specific settings in "settings"
     * This holds the generic AppSettings that all nodes have.
     */
    private AppSettings appSettings;

    /**
     * These are the properties for this node
     */
    private AppProperties appProperties;

    /**
     * This stores a set of parents of this node
     * Parents are any nodes that this node depends on to operate properly
     */
    private Set<NodeBase> parents = new HashSet<NodeBase>();

    /**
     * This stores a set of children to this node
     * Children are any nodes that depend on this node to operate properly
     */
    private Set<Node> children = new HashSet<Node>();

    /**
     * These store this node's metrics (for display in the UI)
     * The hash map is for fast lookups
     * The list is to maintain order for the UI
     */
    private Map<String, NodeMetric> metrics = new HashMap<String, NodeMetric>();
    private List<NodeMetric> metricList = new ArrayList<NodeMetric>();
        
    private AppSettings.AppState currentState;

    protected NodeBase( )
    {
        currentState = AppState.LOADED;
    }

    protected NodeBase( AppSettings appSettings, AppProperties appProperties )
    {
        this.appSettings = appSettings;
        this.appProperties = appProperties;

        currentState = AppState.LOADED;
    }

    protected abstract PipelineConnector[] getConnectors();

    protected void connectPipelineConnectors()
    {
        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                UvmContextFactory.context().pipelineFoundry().registerPipelineConnector( connector );
            }
        }
    }

    protected void disconnectPipelineConnectors()
    {
        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                UvmContextFactory.context().pipelineFoundry().deregisterPipelineConnector( connector );
                connector.destroy();
            }
        }
    }
    
    public final AppState getRunState()
    {
        return currentState;
    }

    public final void init()
    {
        init(true);
    }
    
    public final void start() 
    {
        start(true);
    }

    public final void stop() 
    {
        stop(true);
    }

    public AppSettings getAppSettings()
    {
        return appSettings;
    }

    public void setAppSettings( AppSettings appSettings )
    {
        this.appSettings = appSettings;
    }

    public AppProperties getAppProperties()
    {
        return appProperties;
    }

    public void setAppProperties( AppProperties appProperties )
    {
        this.appProperties = appProperties;
    }

    public void addParent( NodeBase parent )
    {
        parents.add(parent);
        parent.addChild(this);
    }

    /**
     * Called when the node is new, initial settings should be
     * created and saved in this method.
     */
    public void initializeSettings() { }

    public void resumeState( AppState nodeState ) 
    {
        switch ( nodeState ) {
        case LOADED:
            logger.debug("leaving node in LOADED state");
            break;
        case INITIALIZED:
            logger.debug("bringing into INITIALIZED state");
            init(false);
            break;
        case RUNNING:
            logger.debug("bringing into RUNNING state: " + appSettings);
            init(false);
            start(false);
            break;
        case DESTROYED:
            logger.debug("bringing into DESTROYED state: " + appSettings);
            currentState = AppState.DESTROYED;
            break;
        default:
            logger.warn("unknown state: " + nodeState);
        }
    }

    public void destroy()
    {
        uninstall();

        destroy(true);
    }

    public void stopIfRunning()
    {
        UvmContextFactory.context().loggingManager().setLoggingNode(appSettings.getId());

        switch ( currentState ) {
        case RUNNING:
            stop(false);
            break;
        case LOADED:
            break;
        case INITIALIZED:
            break;
        default:
            break;
        }

        UvmContextFactory.context().loggingManager().setLoggingUvm();
    }

    public void enable()
    {
        switch ( currentState ) {
        case LOADED:
            logger.warn("enabling in: " + currentState);
            break;
        case DESTROYED:
            logger.warn("enabling in: " + currentState);
            break;
        case RUNNING:
            break; /* do nothing */
        case INITIALIZED:
            break; /* do nothing */
        default:
            changeState(AppState.INITIALIZED, true);
        }            
    }

    public void logEvent( LogEvent evt )
    {
        String tag = appProperties.getDisplayName().replaceAll("\\s+","_") + " [" + appSettings.getId() + "]:";
        evt.setTag(tag);
        
        UvmContextFactory.context().logEvent(evt);
    }

    @SuppressWarnings("rawtypes")
    public static final Node loadClass( AppProperties appProperties, AppSettings appSettings, boolean isNew ) throws Exception
    {
        if ( appProperties == null || appSettings == null )
            throw new Exception("Invalid Arguments: null");

        try {
            NodeBase node;

            Set<Node> parentNodes = new HashSet<Node>();
            if (appProperties.getParents() != null) {
                for (String parent : appProperties.getParents()) {
                    parentNodes.add(startParent(parent, appSettings.getPolicyId()));
                }
            }

            UvmContextFactory.context().loggingManager().setLoggingNode(appSettings.getId());

            String appSettingsName = appSettings.getAppName();
            staticLogger.debug("setting app " + appSettingsName + " log4j repository");

            String className = appProperties.getClassName();
            java.lang.reflect.Constructor constructor = Class.forName(className).getConstructor(new Class<?>[]{AppSettings.class, AppProperties.class});
            node = (NodeBase)constructor.newInstance( appSettings, appProperties );

            node.setAppProperties( appProperties );
            node.setAppSettings( appSettings );
                
            for (Node parentNode : parentNodes) {
                node.addParent((NodeBase)parentNode);
            }

            if (isNew) {
                node.initializeSettings( );
                node.init();
            } else {
                try {
                    node.resumeState(appSettings.getTargetState());
                }
                catch (Exception exn) {
                    staticLogger.error("Exception during node resumeState", exn);
                    if ( exn.getCause() != null )
                        staticLogger.error("Cause", exn.getCause() );
                    // still return the initialized node
                }
            }
            
            return node;

        } catch (Exception exn) {
            staticLogger.error("Exception during node initialization", exn);
            if ( exn.getCause() != null )
                staticLogger.error("Cause", exn.getCause() );
            throw exn;
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    public final void destroyClass() throws Exception
    {
        try {
            UvmContextFactory.context().loggingManager().setLoggingNode(appSettings.getId());
            if (this.getRunState() == AppSettings.AppState.RUNNING) {
                this.stop();
            }
            this.destroy();
        } catch (Exception exn) {
            throw new Exception(exn);
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    public List<SessionTuple> liveSessions()
    {
        List<SessionTuple> sessions = new LinkedList<SessionTuple>();

        for (NodeSession sess : liveNodeSessions()) {
            SessionTuple tuple = new SessionTuple( sess.getProtocol(),
                                                   sess.getClientAddr(), sess.getServerAddr(),
                                                   sess.getClientPort(), sess.getServerPort() );
            sessions.add( tuple );
        }

        return sessions;
    }

    public List<NodeSession> liveNodeSessions()
    {
        List<NodeSession> sessions = new LinkedList<NodeSession>();

        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                for ( NodeSession sess : connector.liveSessions() ) {
                    /* create a new sessiontupleimpl so the list will be serialized properly */
                    sessions.add( sess );
                }
            }
        }

        return sessions;
    }

    public List<NodeMetric> getMetrics()
    {
        return metricList;
    }

    public NodeMetric getMetric( String name )
    {
        NodeMetric metric = metrics.get( name );
        if ( metric == null )
            logger.warn("Metric not found: " + name);
        return metric;
    }
    
    public void decrementMetric( String name )
    {
        adjustMetric( name, -1L );
    }

    public void incrementMetric( String name )
    {
        adjustMetric( name, 1L );
    }

    public synchronized void setMetric( String name, Long newValue )
    {
        if ( name == null ) {
            logger.warn( "Invalid stat: " + name );
            return;
        }
        
        NodeMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("NodeMetric not found: " + name);
            return;
        }
        metric.setValue( newValue );
    }

    public synchronized void adjustMetric( String name, Long adjustmentValue )
    {
        if ( name == null ) {
            logger.warn( "Invalid stat: " + name );
            return;
        }
        
        NodeMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("NodeMetric not found: " + name);
            return;
        }

        Long value = metric.getValue();
        if (value == null)
            value = 0L;
        value = value + adjustmentValue;
        metric.setValue( value );
    }

    public synchronized void addMetric( NodeMetric metric )
    {
        if (metrics.get(metric.getName()) != null) {
            //logger.warn("addMetric(): Metric already exists: \"" + metric.getName() + "\" - ignoring");
            return;
        }
        this.metrics.put( metric.getName(), metric );
        this.metricList.add( metric );
    }

    public synchronized void removeMetric( NodeMetric metric )
    {
        if ( metric == null ) {
            logger.warn("Invalid argument: null");
            return;
        }
        if (metrics.get(metric.getName()) == null) {
            logger.warn("Invalid argument: metric not found");
            return;
        }        

        this.metrics.remove( metric.getName() );
        this.metricList.remove( metric );
    }
    
    public String toString()
    {
        return "App[" + getAppSettings().getId() + "," + getAppSettings().getAppName() + "]";
    }
    
    // protected methods -------------------------------------------------

    /**
     * Called when the node is being uninstalled, rather than
     * just being taken down with the UVM.
     */
    protected void uninstall() { }

    /**
     * Called before initialization
     *
     * initialization occurs when an app is instantiated or on startup
     */
    protected void preInit() { } 

    /**
     * Called after initialization
     *
     * initialization occurs when an app is instantiated or on startup
     */
    protected void postInit() { } 

    /**
     * Called just after connecting to PipelineConnector, but before starting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this node is being started permanently
     */
    protected void preStart( boolean isPermanentTransition ) { } 

    /**
     * Called just after starting PipelineConnector and making subscriptions.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this node is being started permanently
     */
    protected void postStart( boolean isPermanentTransition ) { } 

    /**
     * Called just before stopping PipelineConnector and disconnecting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this node is being stopped permanently
     */
    protected void preStop( boolean isPermanentTransition ) { } 

    /**
     * Called after stopping PipelineConnector and disconnecting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this node is being stopped permanently
     */
    protected void postStop( boolean isPermanentTransition ) { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy()  { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link AppState#DESTROYED} state.
     */
    protected void postDestroy() { }

    /**
     * This kills/resets all of the matching sessions (runs against all sessions globally)
     */
    protected void killMatchingSessionsGlobal( SessionMatcher matcher )
    {
        if (matcher == null)
            return;

        UvmContextFactory.context().netcapManager().shutdownMatches( matcher );
    }

    /**
     * This kills/resets all of the matching sessions for this node's sessions
     * This includes "released" sessions that we processed previously by one of this node's pipespecs
     */
    protected void killMatchingSessions( SessionMatcher matcher )
    {
        logger.info("killMatchingSessions()");
        if ( matcher == null )
            return;
        if ( getConnectors() == null )
            return;
        
        for ( PipelineConnector connector : getConnectors() )
            UvmContextFactory.context().netcapManager().shutdownMatches( matcher, connector );
    }
    
    /**
     * This kills all this node's sessions (for all its pipespecs)
     */
    public void killAllSessions()
    {
        killMatchingSessions(new SessionMatcher() {
                public boolean isMatch( Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments ) { return true; }
            });
    }

    private void addChild( Node child )
    {
        children.add(child);
    }

    private boolean removeChild( Node child )
    {
        return children.remove(child);
    }

    private void changeState( AppState nodeState, boolean saveNewTargetState )
    {
        if ( saveNewTargetState ) {
            UvmContextFactory.context().appManager().saveTargetState( this, nodeState );

            UvmContextFactory.context().pipelineFoundry().clearCache();
        }

        this.currentState = nodeState;
    }

    private void init( boolean saveNewTargetState ) 
    {
        if ( currentState != AppState.LOADED ) {
            logger.warn("Init called in state: " + currentState);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.appSettings.getId()) ;

            // if no valid license exists, request a trial license
            try {
                if ( ! UvmContextFactory.context().licenseManager().isLicenseValid( appProperties.getName() ) ) {
                    logger.info("No valid license for: " + appProperties.getName());
                    logger.info("Requesting trial for: " + appProperties.getName());
                    UvmContextFactory.context().licenseManager().requestTrialLicense( appProperties.getName() );
                }
            } catch (Exception e) {
                logger.warn( "Exception fetching trial license. Ignoring...", e );
            }
            
            preInit();
            changeState( AppState.INITIALIZED, saveNewTargetState );
            postInit();

        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void start( boolean saveNewTargetState ) 
    {
        if (AppState.INITIALIZED != getRunState()) {
            logger.warn("Start called in state: " + getRunState());
            return;
        }

        for (NodeBase parent : parents) {
            if (AppState.INITIALIZED == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingNode( parent.getAppSettings().getId() );
                    if (parent.getRunState() == AppState.INITIALIZED) 
                        parent.start( false );
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingNode( appSettings.getId() );
                }
            }
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.RUNNING, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to start node",e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.appSettings.getId() );
            logger.info("Starting   node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");

            try {
                preStart( saveNewTargetState );
            } catch (Exception e) {
                logger.warn("Exception in preStart(). Reverting to INITIALIZED state.", e);
                changeState(AppState.INITIALIZED, saveNewTargetState);
                throw e;
            }

            connectPipelineConnectors();

            try {
                postStart( saveNewTargetState ); 
            } catch (Exception e) {
                logger.warn("Exception in postStart().", e);
            }

            logger.info("Started    node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void stop( boolean saveNewTargetState ) 
    {
        if (AppState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.INITIALIZED, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to stop node",e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.appSettings.getId() );
            logger.info("Stopping   node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");
            preStop( saveNewTargetState );
            disconnectPipelineConnectors();
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }

        for (NodeBase parent : parents) {
            if (AppState.RUNNING == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingNode( parent.getAppSettings().getId() );
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingNode( appSettings.getId() );
                }
            }
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.appSettings.getId() );
            postStop( saveNewTargetState ); 
            logger.info("Stopped    node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void destroy( boolean saveNewTargetState )  
    {
        if (currentState == AppState.DESTROYED) {
            logger.warn("Ignoring destroy(): Already in state DESTROYED");
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.appSettings.getId() );
            logger.info("Destroying node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");
            preDestroy();
            for (NodeBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(AppState.DESTROYED, saveNewTargetState);

            postDestroy(); // XXX if exception, state == ?
            logger.info("Destroyed  node " + this.getAppProperties().getName() + "(" + this.getAppProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private final void stopIfNotRequiredByChildren() 
    {
        if (getRunState() != AppState.RUNNING)
            return;

        /**
         * Return if any children are still running
         */
        for (Node node : children) {
            if (node.getRunState() == AppState.RUNNING) 
                return;
        } 

        stop( false );
    }
    
    private final static Node startParent( String parent, Integer policyId ) throws Exception
    {
        if (null == parent) {
            return null;
        }

        staticLogger.debug( "Starting required parent: " + parent );

        Node parentNode = getParentNode( parent, policyId );

        if ( parentNode == null ) {
            staticLogger.debug("Parent does not exist, instantiating");

            parentNode = UvmContextFactory.context().appManager().instantiate(parent, policyId);
        }

        if ( parentNode == null ) {
            throw new Exception("could not create parent: " + parent);
        } else {
            return parentNode;
        }
    }

    private final static Node getParentNode( String parent, Integer childPolicyId )
    {
        for (Node node : UvmContextFactory.context().appManager().appInstances(parent)) {
            Integer policyId = node.getAppSettings().getPolicyId();
            if ( policyId == null || policyId.equals( childPolicyId ) )
                return node;
        }

        return null;
    }
    
}
