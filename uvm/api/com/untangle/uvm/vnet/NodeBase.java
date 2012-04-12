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

    private final NodeContext nodeContext;
    private final NodeSettings nodeSettings;
    private final Set<NodeBase> parents = new HashSet<NodeBase>();
    private final Set<Node> children = new HashSet<Node>();
    private final NodeManager nodeManager;

    private final Object stateChangeLock = new Object();

    private NodeSettings.NodeState currentState;
    private boolean wasStarted = false;

    protected NodeBase()
    {
        UvmContext uvm = UvmContextFactory.context();
        nodeManager = uvm.nodeManager();
        nodeContext = nodeManager.threadContext();
        nodeSettings = nodeContext.getNodeSettings();

        currentState = NodeState.LOADED;
    }

    // abstract methods --------------------------------------------------------

    protected abstract void connectArgonConnector();
    protected abstract void disconnectArgonConnector();

    // Node methods ------------------------------------------------------------

    public final NodeState getRunState()
    {
        return currentState;
    }

    public final void start() 
    {
        synchronized (stateChangeLock) {
            start(true);
        }
    }

    public final void stop() 
    {
        synchronized (stateChangeLock) {
            stop(true);
        }
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

    // NodeBase methods ---------------------------------------------------

    public void addParent( NodeBase parent )
    {
        parents.add(parent);
        parent.addChild(this);
    }

    /**
     * Called when the node is new, initial settings should be
     * created and saved in this method.
     *
     * XXX rename these methods to something more general
     */
    public void initializeSettings() { }

    /**
     * Called when the node is new, initial settings should be
     * created and saved in this method.
     *
     * XXX rename these methods to something more general
     */
    public void destroySettings() { }

    public void init() 
    {
        synchronized (stateChangeLock) {
            init(true);
        }
    }

    public void disable()
    {
        if (NodeState.LOADED == currentState
            || NodeState.DESTROYED == currentState) {
            logger.warn("disabling in: " + currentState);
            return;
        } else if (NodeState.RUNNING == currentState) {
            stop(false);
        }
        changeState(NodeState.DISABLED, true);
    }

    public void resumeState( NodeState ts ) 
    {
        if (NodeState.LOADED == ts) {
            logger.debug("leaving node in LOADED state");
        } else if (NodeState.INITIALIZED == ts) {
            logger.debug("bringing into INITIALIZED state");
            init(false);
        } else if (NodeState.RUNNING == ts) {
            logger.debug("bringing into RUNNING state: " + nodeSettings);
            init(false);
            start(false);
        } else if (NodeState.DESTROYED == ts) {
            logger.debug("bringing into DESTROYED state: " + nodeSettings);
            currentState = NodeState.DESTROYED;
        } else if (NodeState.DISABLED == ts) {
            logger.debug("bringing into DISABLED state: " + nodeSettings);
            init(false);
            currentState = NodeState.DISABLED;
        } else {
            logger.warn("unknown state: " + ts);
        }
    }

    public void destroy()
    {
        uninstall();

        synchronized (stateChangeLock) {
            destroy(true);
        }
    }

    /**
     * Unloads the node for UVM shutdown, does not change
     * node's target state.
     *
     * XXX it is incorrect to unload a casing if the child is loaded,
     * enforce that here.
     */
    public void unload()
    {
        try {
            if (currentState == NodeState.LOADED) {
                destroy(false); // XXX
            } else if (currentState == NodeState.INITIALIZED) {
                destroy(false);
            } else if (currentState == NodeState.RUNNING) {
                stop(false);
                destroy(false);
            } else if (currentState == NodeState.DISABLED) {
                destroy(false);
            }
        } catch (Exception exn) {
            logger.warn("could not unload", exn);
        }
    }

    public void enable()
    {
        if (NodeState.LOADED == currentState
            || NodeState.DESTROYED == currentState) {
            logger.warn("enabling in: " + currentState);
            return;
        } else if (NodeState.RUNNING == currentState
                   || NodeState.INITIALIZED == currentState) {
            // We're already fine.
        } else {
            // DISABLED
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
    protected void uninstall()
    { }

    /**
     * Called as the instance is created, but is not configured.
     */
    protected void preInit()  { } 

    /**
     * Same as <code>preInit</code>, except now officially in the
     * {@link NodeState#INITIALIZED} state.
     */
    protected void postInit()  { } 

    /**
     * Called just after connecting to ArgonConnector, but before starting.
     *
     */
    protected void preStart()  { } 

    /**
     * Called just after starting ArgonConnector and making subscriptions.
     *
     */
    protected void postStart()  { } 

    /**
     * Called just before stopping ArgonConnector and disconnecting.
     *
     */
    protected void preStop()  { } 

    /**
     * Called after stopping ArgonConnector and disconnecting.
     *
     */
    protected void postStop()  { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy()  { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link NodeState#DESTROYED} state.
     */
    protected void postDestroy()  { }

    // private methods ---------------------------------------------------------

    private void addChild( Node child )
    {
        children.add(child);
    }

    private boolean removeChild( Node child )
    {
        return children.remove(child);
    }

    private void changeState( NodeState nodeState, boolean syncState )
    {
        currentState = nodeState;

        if (syncState) {
            if (NodeState.RUNNING == nodeState) {
                wasStarted = true;
            }

            MessageManager mm = UvmContextFactory.context().messageManager();
            NodeStateChange nsc = new NodeStateChange(nodeContext.getNodeDesc(), nodeState);
            mm.submitMessage(nsc);

            nodeManager.saveTargetState(this.nodeSettings.getId(), nodeState);
            
            UvmContextFactory.context().nodeManager().flushNodeStateCache();
            UvmContextFactory.context().pipelineFoundry().clearChains();
        }
    }

    private void init( boolean syncState ) 
    {
        if (NodeState.LOADED != currentState) {
            logger.warn("Init called in state: " + currentState);
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);

            preInit();
            changeState( NodeState.INITIALIZED, syncState );
            postInit();

        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void start( boolean syncState ) 
    {
        if (NodeState.INITIALIZED != getRunState()) {
            logger.warn("Start called in state: " + getRunState());
            return;
        }

        for (NodeBase parent : parents) {
            if (NodeState.INITIALIZED == parent.getRunState()) {
                try {
                    NodeContext pCtx = parent.getNodeContext();
                    nodeManager.registerThreadContext(pCtx);
                    if (parent.getRunState() == NodeState.INITIALIZED) 
                        parent.start( false );
                } finally {
                    nodeManager.registerThreadContext(nodeContext);
                }
            }
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            logger.info("Starting   node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preStart();

            connectArgonConnector();

            changeState(NodeState.RUNNING, syncState);
            postStart(); // XXX if exception, state == ?
            logger.info("Started    node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void stop( boolean syncState ) 
    {
        if (NodeState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            logger.info("Stopping   node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preStop();
            disconnectArgonConnector();
            changeState(NodeState.INITIALIZED, syncState);
        } finally {
            nodeManager.deregisterThreadContext();
        }

        for (NodeBase parent : parents) {
            if (NodeState.RUNNING == parent.getRunState()) {
                try {
                    NodeContext pCtx = parent.getNodeContext();
                    nodeManager.registerThreadContext(pCtx);
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    nodeManager.registerThreadContext(nodeContext);
                }
            }
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            postStop(); // XXX if exception, state == ?
            logger.info("Stopped    node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void destroy( boolean syncState )  
    {
        if (NodeState.INITIALIZED != currentState
            && NodeState.LOADED != currentState
            && NodeState.DISABLED != currentState) {
            logger.warn("Destroy in state: " + currentState);
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            logger.info("Destroying node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
            preDestroy();
            for (NodeBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(NodeState.DESTROYED, syncState);

            postDestroy(); // XXX if exception, state == ?
            logger.info("Destroyed  node " + this.getNodeContext().getNodeDesc().getName() + "(" + this.getNodeContext().getNodeDesc().getNodeSettings() + ")" + " ...");
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void stopIfNotRequiredByChildren() 
    {
        boolean childrenStopped = true;

        if (NodeState.RUNNING == getRunState()) {
            for (Node node : children) {
                if (NodeState.RUNNING == node.getRunState()) {
                    childrenStopped = false;
                    break;
                }
            }
        } else {
            childrenStopped = false;
        }

        if (childrenStopped) {
            stop( false );
        }
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
