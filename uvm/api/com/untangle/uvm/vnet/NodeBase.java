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

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SessionMatcherFactory;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeSettings.NodeState;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.NodeStateChangeMessage;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.toolbox.PackageDesc;

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
     * This holds the generic NodeSettings that all nodes have.
     */
    private NodeSettings nodeSettings;

    /**
     * These are the properties for this node
     */
    private NodeProperties nodeProperties;

    /**
     * The package description for this node
     */
    private PackageDesc packageDesc;
    
    /**
     * This is the pipeline/traffic subscriptions for this node
     */
    private PipeSpec[] pipeSpecs;

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
        
    private NodeSettings.NodeState currentState;
    private boolean wasStarted = false;

    protected NodeBase( )
    {
        currentState = NodeState.LOADED;
    }

    protected NodeBase( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        this.nodeSettings = nodeSettings;
        this.nodeProperties = nodeProperties;

        currentState = NodeState.LOADED;
    }

    protected abstract PipeSpec[] getPipeSpecs();

    protected void connectArgonConnector()
    {
        if (null == pipeSpecs) {
            PipeSpec[] pss = getPipeSpecs();
            pipeSpecs = null == pss ? new PipeSpec[0] : pss;
            for (PipeSpec ps : pipeSpecs) {
                ps.connectArgonConnector();
            }
        } else {
            logger.warn("ArgonConnectors already connected");
        }
    }

    protected void disconnectArgonConnector()
    {
        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                ps.disconnectArgonConnector();
            }
            pipeSpecs = null;
        } else {
            logger.warn("ArgonConnectors not connected");
        }
    }
    
    public final NodeState getRunState()
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

    public NodeSettings getNodeSettings()
    {
        return nodeSettings;
    }

    public void setNodeSettings( NodeSettings nodeSettings )
    {
        this.nodeSettings = nodeSettings;
    }

    public NodeProperties getNodeProperties()
    {
        return nodeProperties;
    }

    public void setNodeProperties( NodeProperties nodeProperties )
    {
        this.nodeProperties = nodeProperties;
    }

    public PackageDesc getPackageDesc()
    {
        //return UvmContextFactory.context().toolboxManager().packageDesc(packageName);
        return packageDesc;
    }

    public void setPackageDesc( PackageDesc packageDesc )
    {
        this.packageDesc = packageDesc;
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

    /**
     * Called when the node is new, initial settings should be
     * created and saved in this method.
     */
    public void destroySettings() { }

    public void resumeState( NodeState nodeState ) 
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
            logger.debug("bringing into RUNNING state: " + nodeSettings);
            init(false);
            start(false);
            break;
        case DESTROYED:
            logger.debug("bringing into DESTROYED state: " + nodeSettings);
            currentState = NodeState.DESTROYED;
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

    /**
     * Unloads the node for UVM shutdown, does not change
     * node's target state.
     */
    public void unload()
    {
        switch ( currentState ) {
        case RUNNING:
                stop(false);
                try {destroy(false); } catch(Exception e) {}
                break;
        case LOADED:
                try {destroy(false); } catch(Exception e) {}
                break;
        case INITIALIZED:
                try {destroy(false); } catch(Exception e) {}
                break;
        default:
                break;
        }
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
            changeState(NodeState.INITIALIZED, true);
        }            
    }

    public void logEvent( LogEvent evt )
    {
        String tag = nodeProperties.getSyslogName() + "[" + nodeSettings.getId() + "]: ";
        evt.setTag(tag);
        
        UvmContextFactory.context().loggingManager().logEvent(evt);
    }

    public static final Node loadClass( NodeProperties nodeProperties, NodeSettings nodeSettings, PackageDesc packageDesc, boolean isNew ) throws DeployException
    {
        Set<Node> parentNodes = new HashSet<Node>();
        if (nodeProperties.getParents() != null) {
            for (String parent : nodeProperties.getParents()) {
                parentNodes.add(startParent(parent, nodeSettings.getPolicyId()));
            }
        }

        NodeBase node;
        try {
            UvmContextFactory.context().loggingManager().setLoggingNode(nodeSettings.getId());

            String nodeSettingsName = nodeSettings.getNodeName();
            staticLogger.debug("setting node " + nodeSettingsName + " log4j repository");

            String className = nodeProperties.getClassName();
            java.lang.reflect.Constructor constructor = Class.forName(className).getConstructor(new Class[]{NodeSettings.class, NodeProperties.class});
            node = (NodeBase)constructor.newInstance( nodeSettings, nodeProperties );
            //node = (NodeBase)Class.forName(className).newInstance(getNodeSettings(), getNodeProperties());
            node.setNodeProperties( nodeProperties );
            node.setNodeSettings( nodeSettings );
            node.setPackageDesc( packageDesc );
                
            for (Node parentNode : parentNodes) {
                node.addParent((NodeBase)parentNode);
            }

            if (isNew) {
                node.initializeSettings( );
                node.init( );
            } else {
                node.resumeState(nodeSettings.getTargetState());
            }
        } catch (ClassNotFoundException exn) {
            staticLogger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (InstantiationException exn) {
            staticLogger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (IllegalAccessException exn) {
            staticLogger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (Exception exn) {
            staticLogger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }

        return node;
    }

    public final void destroyClass() throws Exception
    {
        try {
            UvmContextFactory.context().loggingManager().setLoggingNode(nodeSettings.getId());
            if (this.getRunState() == NodeSettings.NodeState.RUNNING) {
                this.stop();
            }
            this.destroy();
            this.destroySettings();
        } catch (Exception exn) {
            throw new Exception(exn);
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    public final void unloadClass()
    {
        try {
            UvmContextFactory.context().loggingManager().setLoggingNode(nodeSettings.getId());
            this.unload();
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }
    
    public List<VnetSessionDesc> liveSessionDescs()
    {
        List<VnetSessionDesc> sessList = new LinkedList<VnetSessionDesc>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (VnetSessionDesc isd : ps.liveSessionDescs()) {
                    sessList.add(isd);
                }
            }
        }

        return sessList;
    }

    public List<IPSession> liveSessions()
    {
        List<IPSession> sessions = new LinkedList<IPSession>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (IPSession sess : ps.liveSessions()) {
                    sessions.add(sess);
                }
            }
        }

        return sessions;
    }

    public List<NodeMetric> getMetrics()
    {
        return metricList;
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
        if (metrics.get(metric.getName()) != null)
            return;
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
        return "Node[" + getNodeSettings().getId() + "," + getNodeSettings().getNodeName() + "]";
    }
    
    // protected methods -------------------------------------------------

    /**
     * Called when the node is being uninstalled, rather than
     * just being taken down with the UVM.
     */
    protected void uninstall() { }

    /**
     * Called as the instance is created, but is not configured.
     */
    protected void preInit() { } 

    /**
     * Same as <code>preInit</code>, except now officially in the
     * {@link NodeState#INITIALIZED} state.
     */
    protected void postInit() { } 

    /**
     * Called just after connecting to ArgonConnector, but before starting.
     *
     */
    protected void preStart() { } 

    /**
     * Called just after starting ArgonConnector and making subscriptions.
     *
     */
    protected void postStart() { } 

    /**
     * Called just before stopping ArgonConnector and disconnecting.
     *
     */
    protected void preStop() { } 

    /**
     * Called after stopping ArgonConnector and disconnecting.
     *
     */
    protected void postStop() { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy()  { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link NodeState#DESTROYED} state.
     */
    protected void postDestroy() { }

    /**
     * This kills/resets all of the matching sessions 
     */
    protected void killMatchingSessions( SessionMatcher matcher )
    {
        if (matcher == null)
            return;
        
        UvmContextFactory.context().argonManager().shutdownMatches(matcher);
    }

    private void addChild( Node child )
    {
        children.add(child);
    }

    private boolean removeChild( Node child )
    {
        return children.remove(child);
    }

    private void changeState( NodeState nodeState, boolean saveNewTargetState )
    {
        currentState = nodeState;

        if ( saveNewTargetState ) {
            if (NodeState.RUNNING == nodeState) {
                wasStarted = true;
            }

            MessageManager mm = UvmContextFactory.context().messageManager();
            NodeStateChangeMessage nsc = new NodeStateChangeMessage(nodeProperties, nodeSettings, nodeState);
            mm.submitMessage(nsc);

            UvmContextFactory.context().nodeManager().saveTargetState( this, nodeState );
            
            UvmContextFactory.context().pipelineFoundry().clearChains();
        }
    }

    private void init( boolean saveNewTargetState ) 
    {
        if (NodeState.LOADED != currentState) {
            logger.warn("Init called in state: " + currentState);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.nodeSettings.getId()) ;

            preInit();
            changeState( NodeState.INITIALIZED, saveNewTargetState );
            postInit();

        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void start( boolean saveNewTargetState ) 
    {
        if (NodeState.INITIALIZED != getRunState()) {
            logger.warn("Start called in state: " + getRunState());
            return;
        }

        for (NodeBase parent : parents) {
            if (NodeState.INITIALIZED == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingNode( parent.getNodeSettings().getId() );
                    if (parent.getRunState() == NodeState.INITIALIZED) 
                        parent.start( false );
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingNode( nodeSettings.getId() );
                }
            }
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.nodeSettings.getId() );
            logger.info("Starting   node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
            preStart();

            connectArgonConnector();

            changeState(NodeState.RUNNING, saveNewTargetState);
            postStart(); // XXX if exception, state == ?
            logger.info("Started    node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void stop( boolean saveNewTargetState ) 
    {
        if (NodeState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.nodeSettings.getId() );
            logger.info("Stopping   node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
            preStop();
            disconnectArgonConnector();
            changeState(NodeState.INITIALIZED, saveNewTargetState);
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }

        for (NodeBase parent : parents) {
            if (NodeState.RUNNING == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingNode( parent.getNodeSettings().getId() );
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingNode( nodeSettings.getId() );
                }
            }
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.nodeSettings.getId() );
            postStop(); // XXX if exception, state == ?
            logger.info("Stopped    node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void destroy( boolean saveNewTargetState )  
    {
        if (currentState == NodeState.DESTROYED) {
            logger.warn("Ignoring destroy(): Already in state DESTROYED");
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingNode( this.nodeSettings.getId() );
            logger.info("Destroying node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
            preDestroy();
            for (NodeBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(NodeState.DESTROYED, saveNewTargetState);

            postDestroy(); // XXX if exception, state == ?
            logger.info("Destroyed  node " + this.getNodeProperties().getName() + "(" + this.getNodeProperties().getName() + ")" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private final void stopIfNotRequiredByChildren() 
    {
        if (getRunState() != NodeState.RUNNING)
            return;

        /**
         * Return if any children are still running
         */
        for (Node node : children) {
            if (node.getRunState() == NodeState.RUNNING) 
                return;
        } 

        stop( false );
    }
    
    private final static Node startParent( String parent, Long policyId )
        throws DeployException
    {
        if (null == parent) {
            return null;
        }

        PackageDesc md = UvmContextFactory.context().toolboxManager().packageDesc(parent);

        if (null == md) {
            staticLogger.warn("parent does not exist: " + parent);
            throw new DeployException("could not create parent: " + parent);
        }

        if (PackageDesc.Type.CASING == md.getType()) {
            policyId = null;
        }

        staticLogger.debug( "Starting required parent: " + parent );

        Node parentNode = getParentNode( parent, policyId );

        if ( parentNode == null ) {
            staticLogger.debug("Parent does not exist, instantiating");

            parentNode = UvmContextFactory.context().nodeManager().instantiate(parent, policyId);
        }

        if ( parentNode == null ) {
            throw new DeployException("could not create parent: " + parent);
        } else {
            return parentNode;
        }
    }

    private final static Node getParentNode( String parent, Long childPolicyId )
    {
        for (Node node : UvmContextFactory.context().nodeManager().nodeInstances(parent)) {
            Long policyId = node.getNodeSettings().getPolicyId();
            if ( policyId == null || policyId.equals( childPolicyId ) )
                return node;
        }

        return null;
    }
    
}
