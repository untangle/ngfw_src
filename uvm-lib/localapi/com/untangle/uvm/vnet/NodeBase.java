/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStateChange;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;

/**
 * A base class for node instances, both normal and casing.
 *
 * @author Aaron Read <amread@untangle.com>
 * @version 1.0
 */
public abstract class NodeBase implements Node
{
    private final Logger logger = Logger.getLogger(NodeBase.class);

    private final NodeContext nodeContext;
    private final NodeId tid;
    private final Set<NodeBase> parents = new HashSet<NodeBase>();
    private final Set<Node> children = new HashSet<Node>();
    private final NodeManager nodeManager;
    private final List<NodeListener> nodeListeners = new LinkedList<NodeListener>();

    private final Object stateChangeLock = new Object();

    private NodeState runState;
    private boolean wasStarted = false;

    protected NodeBase()
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        nodeManager = uvm.nodeManager();
        nodeContext = nodeManager.threadContext();
        tid = nodeContext.getNodeId();

        Counters c = uvm.localMessageManager().getCounters(tid);
        c.addMetric("s2nChunks", I18nUtil.marktr("Server to node chunks"), null, false);
        c.addMetric("c2nChunks", I18nUtil.marktr("Client to node chunks"), null, false);
        c.addMetric("n2sChunks", I18nUtil.marktr("Node to server chunks"), null, false);
        c.addMetric("n2cChunks", I18nUtil.marktr("Server to node chunks"), null, false);
        c.addMetric("s2nBytes", I18nUtil.marktr("Server to node bytes"), "byte", false);
        c.addMetric("c2nBytes", I18nUtil.marktr("Client to node bytes"), "byte", false);
        c.addMetric("n2sBytes", I18nUtil.marktr("Node to server bytes"), "byte", false);
        c.addMetric("n2cBytes", I18nUtil.marktr("Node to client bytes"), "byte", false);

        runState = NodeState.LOADED;
    }

    // abstract methods --------------------------------------------------------

    protected abstract void connectArgonConnector();
    protected abstract void disconnectArgonConnector();

    // Node methods ------------------------------------------------------------

    public final NodeState getRunState()
    {
        return runState;
    }

    public final void start() throws Exception
    {
        synchronized (stateChangeLock) {
            start(true);
        }
    }

    public final void stop() throws Exception
    {
        synchronized (stateChangeLock) {
            stop(true);
        }
    }

    public NodeContext getNodeContext()
    {
        return nodeContext;
    }

    public NodeId getNodeId()
    {
        return tid;
    }

    public Policy getPolicy()
    {
        return tid.getPolicy();
    }

    public NodeDesc getNodeDesc()
    {
        return nodeContext.getNodeDesc();
    }

    // NodeBase methods ---------------------------------------------------

    public void addNodeListener(NodeListener tl)
    {
        synchronized (nodeListeners) {
            nodeListeners.add(tl);
        }
    }

    public void removeNodeListener(NodeListener tl)
    {
        synchronized (nodeListeners) {
            nodeListeners.remove(tl);
        }
    }

    public void addParent(NodeBase parent)
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

    public void init(String[] args) throws Exception
    {
        synchronized (stateChangeLock) {
            init(true, args);
        }
    }

    public void disable() throws Exception
    {
        if (NodeState.LOADED == runState
            || NodeState.DESTROYED == runState) {
            logger.warn("disabling in: " + runState);
            return;
        } else if (NodeState.RUNNING == runState) {
            stop(false);
        }
        changeState(NodeState.DISABLED, true);
    }

    public void resumeState(NodeState ts, String[] args) throws Exception
    {
        if (NodeState.LOADED == ts) {
            logger.debug("leaving node in LOADED state");
        } else if (NodeState.INITIALIZED == ts) {
            logger.debug("bringing into INITIALIZED state");
            init(false, args);
        } else if (NodeState.RUNNING == ts) {
            logger.debug("bringing into RUNNING state: " + tid);
            init(false, args);
            start(false);
        } else if (NodeState.DESTROYED == ts) {
            logger.debug("bringing into DESTROYED state: " + tid);
            runState = NodeState.DESTROYED;
        } else if (NodeState.DISABLED == ts) {
            logger.debug("bringing into DISABLED state: " + tid);
            init(false, args);
            runState = NodeState.DISABLED;
        } else {
            logger.warn("unknown state: " + ts);
        }
    }

    public void destroy() throws Exception
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
            if (runState == NodeState.LOADED) {
                destroy(false); // XXX
            } else if (runState == NodeState.INITIALIZED) {
                destroy(false);
            } else if (runState == NodeState.RUNNING) {
                stop(false);
                destroy(false);
            } else if (runState == NodeState.DISABLED) {
                destroy(false);
            }
        } catch (Exception exn) {
            logger.warn("could not unload", exn);
        }
    }

    public void enable() throws Exception
    {
        if (NodeState.LOADED == runState
            || NodeState.DESTROYED == runState) {
            logger.warn("enabling in: " + runState);
            return;
        } else if (NodeState.RUNNING == runState
                   || NodeState.INITIALIZED == runState) {
            // We're already fine.
        } else {
            // DISABLED
            changeState(NodeState.INITIALIZED, true);
        }
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
     *
     * @param args[] the node-specific arguments.
     */
    protected void preInit(String args[]) throws Exception
    { }

    /**
     * Same as <code>preInit</code>, except now officially in the
     * {@link NodeState#INITIALIZED} state.
     *
     * @param args[] the node-specific arguments.
     */
    protected void postInit(String args[]) throws Exception
    { }

    /**
     * Called just after connecting to ArgonConnector, but before starting.
     *
     */
    protected void preStart() throws Exception
    { }

    /**
     * Called just after starting ArgonConnector and making subscriptions.
     *
     */
    protected void postStart() throws Exception
    { }

    /**
     * Called just before stopping ArgonConnector and disconnecting.
     *
     */
    protected void preStop() throws Exception
    { }

    /**
     * Called after stopping ArgonConnector and disconnecting.
     *
     */
    protected void postStop() throws Exception
    { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy() throws Exception
    { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link NodeState#DESTROYED} state.
     *
     * @param args[] a <code>String</code> value
     */
    protected void postDestroy() throws Exception
    { }

    // private methods ---------------------------------------------------------

    private void addChild(Node child)
    {
        children.add(child);
    }

    private boolean removeChild(Node child)
    {
        return children.remove(child);
    }

    private void changeState(NodeState ts, boolean syncState)
    {
        changeState(ts, syncState, null);
    }

    private void changeState(NodeState ts, boolean syncState, String[] args)
    {
        runState = ts;

        if (syncState) {
            if (NodeState.RUNNING == ts) {
                wasStarted = true;
            }

            LocalMessageManager mm = LocalUvmContextFactory.context().localMessageManager();
            NodeStateChange nsc = new NodeStateChange(nodeContext.getNodeDesc(), ts);
            mm.submitMessage(nsc);

            NodeStateChangeEvent te = new NodeStateChangeEvent(this, ts, args);
            synchronized (nodeListeners) {
                for (NodeListener tl : nodeListeners) {
                    tl.stateChange(te);
                }
            }
            
            LocalUvmContextFactory.context().nodeManager().flushNodeStateCache();
            LocalUvmContextFactory.context().pipelineFoundry().clearChains();
        }
    }

    private void init(boolean syncState, String[] args) throws Exception
    {
        if (NodeState.LOADED != runState) {
            logger.warn("Init called in state: " + runState);
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            preInit(args);
            changeState(NodeState.INITIALIZED, syncState, args);

            postInit(args); // XXX if exception, state == ?
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void start(boolean syncState) throws Exception
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
                    parent.parentStart();
                } finally {
                    nodeManager.registerThreadContext(nodeContext);
                }
            }
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            preStart();

            connectArgonConnector();

            changeState(NodeState.RUNNING, syncState);
            postStart(); // XXX if exception, state == ?
        } finally {
            nodeManager.deregisterThreadContext();
        }
        logger.info("started node");
    }

    private void stop(boolean syncState) throws Exception
    {
        if (NodeState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
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
                    parent.parentStop();
                } finally {
                    nodeManager.registerThreadContext(nodeContext);
                }
            }
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            postStop(); // XXX if exception, state == ?
        } finally {
            nodeManager.deregisterThreadContext();
        }
        logger.info("stopped node");
    }

    private void destroy(boolean syncState) throws Exception
    {
        if (NodeState.INITIALIZED != runState
            && NodeState.LOADED != runState
            && NodeState.DISABLED != runState) {
            logger.warn("Destroy in state: " + runState);
            return;
        }

        try {
            nodeManager.registerThreadContext(nodeContext);
            preDestroy();
            for (NodeBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(NodeState.DESTROYED, syncState);

            postDestroy(); // XXX if exception, state == ?
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    private void parentStart() throws Exception
    {
        if (NodeState.INITIALIZED == getRunState()) {
            start();
        }
    }

    private void parentStop() throws Exception
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
            stop();
        }
    }

    /**
     * This kills/resets all of the matching sessions 
     */
    protected void killMatchingSessions(SessionMatcher matcher)
    {
        if (matcher == null)
            return;
        
        LocalUvmContextFactory.context().argonManager().shutdownMatches(matcher);
    }
}
