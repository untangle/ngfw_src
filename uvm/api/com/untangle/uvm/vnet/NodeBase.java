/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SessionMatcherFactory;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.NodeSettings.NodeState;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.NodeStateChange;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.logging.LogEvent;

/**
 * A base class for node instances, both normal and casing.
 *
 */
public abstract class NodeBase implements Node
{
    private final Logger logger = Logger.getLogger(NodeBase.class);

    /**
     * This is the nodeContext for this node
     */
    private final NodeContext nodeContext;

    /**
     * These are the (generic) settings for this node
     * The node usually stores more app-specific settings in "settings"
     * This holds the generic NodeSettings that all nodes have.
     */
    private final NodeSettings nodeSettings;

    /**
     * This stores a set of parents of this node
     * Parents are any nodes that this node depends on to operate properly
     */
    private final Set<NodeBase> parents = new HashSet<NodeBase>();

    /**
     * This stores a set of children to this node
     * Children are any nodes that depend on this node to operate properly
     */
    private final Set<Node> children = new HashSet<Node>();

    private NodeSettings.NodeState currentState;
    private boolean wasStarted = false;

    protected NodeBase()
    {
        nodeContext = UvmContextFactory.context().nodeManager().threadContext();
        nodeSettings = nodeContext.getNodeSettings();

        currentState = NodeState.LOADED;
    }

    protected abstract void connectArgonConnector();
    protected abstract void disconnectArgonConnector();

    public final NodeState getRunState()
    {
        return currentState;
    }

    public final void start() 
    {
        start(true);
    }

    public final void stop() 
    {
        stop(true);
    }

    public NodeContext getNodeContext()
    {
        return nodeContext;
    }

    public NodeSettings getNodeSettings()
    {
        return nodeSettings;
    }

    public Long getPolicyId()
    {
        return nodeSettings.getPolicyId();
    }

    public NodeDesc getNodeDesc()
    {
        return nodeContext.getNodeDesc();
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

    public void init() 
    {
        init(true);
    }

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
                destroy(false); 
                break;
        case LOADED:
                destroy(false); 
                break;
        case INITIALIZED:
                destroy(false); 
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
        String tag = nodeContext.getNodeDesc().getSyslogName() + "[" + nodeContext.getNodeSettings().getId() + "]: ";
        evt.setTag(tag);
        
        UvmContextFactory.context().loggingManager().logEvent(evt);
    }
    
    // protected no-op methods -------------------------------------------------

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
            NodeStateChange nsc = new NodeStateChange(nodeContext.getNodeDesc(), nodeState);
            mm.submitMessage(nsc);

            UvmContextFactory.context().nodeManager().saveTargetState(this.nodeSettings.getId(), nodeState);
            
            UvmContextFactory.context().nodeManager().flushNodeStateCache();
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
            UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);

            preInit();
            changeState( NodeState.INITIALIZED, saveNewTargetState );
            postInit();

        } finally {
            UvmContextFactory.context().nodeManager().deregisterThreadContext();
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
                    NodeContext pCtx = parent.getNodeContext();
                    UvmContextFactory.context().nodeManager().registerThreadContext(pCtx);
                    if (parent.getRunState() == NodeState.INITIALIZED) 
                        parent.start( false );
                } finally {
                    UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
                }
            }
        }

        try {
            UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
            logger.info("Starting   node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preStart();

            connectArgonConnector();

            changeState(NodeState.RUNNING, saveNewTargetState);
            postStart(); // XXX if exception, state == ?
            logger.info("Started    node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            UvmContextFactory.context().nodeManager().deregisterThreadContext();
        }
    }

    private void stop( boolean saveNewTargetState ) 
    {
        if (NodeState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        try {
            UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
            logger.info("Stopping   node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preStop();
            disconnectArgonConnector();
            changeState(NodeState.INITIALIZED, saveNewTargetState);
        } finally {
            UvmContextFactory.context().nodeManager().deregisterThreadContext();
        }

        for (NodeBase parent : parents) {
            if (NodeState.RUNNING == parent.getRunState()) {
                try {
                    NodeContext pCtx = parent.getNodeContext();
                    UvmContextFactory.context().nodeManager().registerThreadContext(pCtx);
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
                }
            }
        }

        try {
            UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
            postStop(); // XXX if exception, state == ?
            logger.info("Stopped    node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            UvmContextFactory.context().nodeManager().deregisterThreadContext();
        }
    }

    private void destroy( boolean saveNewTargetState )  
    {
        if (currentState == NodeState.DESTROYED) {
            logger.warn("Ignoring destroy(): Already in state DESTROYED");
            return;
        }
        if (currentState != NodeState.RUNNING) {
            logger.warn("Igroning destroy(): Invalid state: " + currentState);
            return;
        }

        try {
            UvmContextFactory.context().nodeManager().registerThreadContext(nodeContext);
            logger.info("Destroying node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preDestroy();
            for (NodeBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(NodeState.DESTROYED, saveNewTargetState);

            postDestroy(); // XXX if exception, state == ?
            logger.info("Destroyed  node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            UvmContextFactory.context().nodeManager().deregisterThreadContext();
        }
    }

    private void stopIfNotRequiredByChildren() 
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

    /**
     * This kills/resets all of the matching sessions 
     */
    protected void killMatchingSessions( SessionMatcher matcher )
    {
        if (matcher == null)
            return;
        
        UvmContextFactory.context().argonManager().shutdownMatches(matcher);
    }

}
