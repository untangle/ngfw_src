/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodePreferences;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.TooManyInstancesException;
import com.untangle.uvm.node.UndeployException;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.NodeListener;
import com.untangle.uvm.vnet.NodeStateChangeEvent;

/**
 * Implements <code>NodeContext</code>. Contains code to load and set
 * up a <code>Node</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class NodeContextImpl implements NodeContext
{
    private final Logger logger = Logger.getLogger(getClass());

    private final NodeDesc nodeDesc;
    private final Tid tid;
    private final NodePreferences nodePreferences;
    private final NodePersistentState persistentState;
    private final boolean isNew;

    private NodeBase node;
    private String mackageName;

    private final NodeManagerImpl nodeManager;
    private final ToolboxManagerImpl toolboxManager;

    NodeContextImpl(URLClassLoader classLoader, NodeDesc tDesc, String mackageName, boolean isNew) throws DeployException
    {
        UvmContextImpl mctx = UvmContextImpl.getInstance();

        if (null != tDesc.getNodeBase()) {
            mctx.schemaUtil().initSchema("settings", tDesc.getNodeBase());
        }
        mctx.schemaUtil().initSchema("settings", tDesc.getName());

        nodeManager = mctx.localNodeManager();
        toolboxManager = mctx.toolboxManager();

        RemoteLoggingManagerImpl lm = mctx.loggingManager();
        if (null != tDesc.getNodeBase()) {
            lm.initSchema(tDesc.getNodeBase());
        }
        lm.initSchema(tDesc.getName());

        this.nodeDesc = tDesc;
        this.tid = nodeDesc.getTid();
        this.mackageName = mackageName;
        this.isNew = isNew;

        checkInstanceCount(nodeDesc);

        if (isNew) {
            // XXX this isn't supposed to be meaningful:
            byte[] pKey = new byte[]
                { (byte)(tid.getId() & 0xFF),
                  (byte)((tid.getId() >> 8) & 0xFF) };


            persistentState = new NodePersistentState
                (tid, mackageName, pKey);

            nodePreferences = new NodePreferences(tid);

            TransactionWork<Object> tw = new TransactionWork<Object>()
                {
                    public boolean doWork(Session s)
                    {
                        s.save(persistentState);
                        s.save(nodePreferences);
                        return true;
                    }

                    public Object getResult() { return null; }
                };
            mctx.runTransaction(tw);
        } else {
            LoadSettings ls = new LoadSettings(tid);
            mctx.runTransaction(ls);
            this.persistentState = ls.getPersistentState();
            this.nodePreferences = ls.getNodePreferences();
        }

        logger.info("Creating node context for: " + tid
                    + " (" + nodeDesc.getName() + ")");
    }

    void init(String[] args) throws DeployException
    {
        Set<NodeContext>parentCtxs = new HashSet<NodeContext>();
        List<String> parents = nodeDesc.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent, tid.getPolicy()));
        }

        UvmContextImpl uctx = UvmContextImpl.getInstance();
        List<String> urs = nodeDesc.getUvmResources();
        for (String uvmResource : urs) {
            uctx.loadUvmResource(uvmResource);
        }
        if (0 < urs.size()) {
            uctx.loadRup();
        }

        final LocalUvmContext mctx = LocalUvmContextFactory.context();
        try {
            nodeManager.registerThreadContext(this);

            String tidName = tid.getName();
            logger.debug("setting node " + tidName + " log4j repository");

            String className = nodeDesc.getClassName();
            node = (NodeBase)Class.forName(className).newInstance();

            for (NodeContext parentCtx : parentCtxs) {
                node.addParent((NodeBase)parentCtx.node());
            }

            node.addNodeListener(new NodeListener()
                {
                    public void stateChange(NodeStateChangeEvent te) {
                        {
                            final NodeState ts = te.getNodeState();

                            TransactionWork<Object> tw = new TransactionWork<Object>()
                                {
                                    public boolean doWork(Session s)
                                    {
                                        persistentState.setTargetState(ts);
                                        s.merge(persistentState);
                                        return true;
                                    }

                                    public Object getResult() { return null; }
                                };
                            mctx.runTransaction(tw);

                            mctx.eventLogger().log(new NodeStateChange(tid, ts));
                        }
                    }
                });

            if (isNew) {
                node.initializeSettings();
                node.init(args);
                boolean enabled = toolboxManager.isEnabled(mackageName);
                if (!enabled) {
                    node.disable();
                }
            } else {
                node.resumeState(persistentState.getTargetState(), args);
            }
        } catch (ClassNotFoundException exn) {
            throw new DeployException(exn);
        } catch (InstantiationException exn) {
            throw new DeployException(exn);
        } catch (IllegalAccessException exn) {
            throw new DeployException(exn);
        } catch (NodeException exn) {
            throw new DeployException(exn);
        } finally {
            nodeManager.deregisterThreadContext();

            if (null == node) {
                TransactionWork<Object> tw = new TransactionWork<Object>()
                    {
                        public boolean doWork(Session s)
                        {
                            s.delete(persistentState);
                            return true;
                        }

                        public Object getResult() { return null; }
                    };
                mctx.runTransaction(tw);
            }
        }
    }

    // NodeContext -------------------------------------------------------

    public Tid getTid()
    {
        return tid;
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
    }

    public NodePreferences getNodePreferences()
    {
        return nodePreferences;
    }

    public MackageDesc getMackageDesc()
    {
        return toolboxManager.mackageDesc(mackageName);
    }

    public Node node()
    {
        return node;
    }

    // node call-through methods -----------------------------------------

    public IPSessionDesc[] liveSessionDescs()
    {
        return node.liveSessionDescs();
    }

    public NodeState getRunState()
    {
        return null == node ? NodeState.LOADED
            : node.getRunState();
    }

    // XXX should be LocalNodeContext ------------------------------------

    // XXX remove this method...
    @Deprecated
    public boolean runTransaction(TransactionWork<?> tw)
    {
        return LocalUvmContextFactory.context().runTransaction(tw);
    }

    public boolean resourceExists(String res)
    {
        String baseNodeName = nodeDesc.getNodeBase();
        return resourceExistsInt(res, getMackageDesc(), baseNodeName);
    }

    public InputStream getResourceAsStream(String res)
    {
        String baseNodeName = nodeDesc.getNodeBase();
        return getResourceAsStreamInt(res, getMackageDesc(), baseNodeName);
    }

    private boolean resourceExistsInt(String res, MackageDesc mackageDesc,
                                      String baseNodeName)
    {
        boolean exists;
        try {
            URL url = new URL(toolboxManager.getResourceDir(mackageDesc), res);
            File f = new File(url.toURI());
            exists = f.exists();
        } catch (MalformedURLException exn) {
            logger.info("resource not found, malformed url: " + res, exn);
            return false;
        } catch (URISyntaxException exn) {
            logger.info("resource not found, uri syntax: " + res, exn);
            return false;
        }

        if (exists)
            return true;
        else {
            // bug3699: Try the base, if any.
            if (baseNodeName != null) {
                MackageDesc baseDesc = toolboxManager.mackageDesc(baseNodeName);
                if (baseDesc == null) {
                    return false;
                }
                // Assume only one level of base.
                return resourceExistsInt(res, baseDesc, null);
            }
            return false;
        }
    }

    private InputStream getResourceAsStreamInt(String res,
                                               MackageDesc mackageDesc,
                                               String baseNodeName)
    {
        try {
            URL url = new URL(toolboxManager.getResourceDir(mackageDesc), res);
            File f = new File(url.toURI());
            return new FileInputStream(f);
        } catch (MalformedURLException exn) {
            logger.warn("resource not found, malformed url: " + res, exn);
            return null;
        } catch (URISyntaxException exn) {
            logger.warn("resource not found, uri syntax: " + res, exn);
            return null;
        } catch (FileNotFoundException exn) {
            // bug3699: Try the base, if any.
            if (baseNodeName != null) {
                MackageDesc baseDesc = toolboxManager.mackageDesc(baseNodeName);
                if (baseDesc == null) {
                    logger.warn("resource not found, base missing: " + baseNodeName);
                    return null;
                }
                // Assume only one level of base.
                return getResourceAsStreamInt(res, baseDesc, null);
            }
            logger.warn("resource not found: " + res, exn);
            return null;
        }
    }

    // package private methods ------------------------------------------------

    void destroy() throws UndeployException
    {
        try {
            nodeManager.registerThreadContext(this);
            if (node.getRunState() == NodeState.RUNNING) {
                node.stop();
            }
            node.destroy();
            node.destroySettings();
        } catch (NodeException exn) {
            throw new UndeployException(exn);
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    void unload()
    {
        if (node != null) {
            try {
                nodeManager.registerThreadContext(this);
                node.unload();
            } finally {
                nodeManager.deregisterThreadContext();
            }
        }
    }

    void destroyPersistentState()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    tid.setPolicy(null);
                    s.update(tid);
                    s.delete(persistentState);
                    s.delete(getNodePreferences());
                    return true;
                }

                public Object getResult() { return null; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
    }

    // private classes --------------------------------------------------------

    private class LoadSettings extends TransactionWork<Object>
    {
        private final Tid tid;

        private NodePersistentState persistentState;
        private NodePreferences nodePreferences;

        public LoadSettings(Tid tid)
        {
            this.tid = tid;
        }

        public boolean doWork(Session s)
        {
            Query q = s.createQuery
                ("from NodePersistentState tps where tps.tid = :tid");
            q.setParameter("tid", tid);

            persistentState = (NodePersistentState)q.uniqueResult();

            if (!toolboxManager.isEnabled(mackageName)) {
                persistentState.setTargetState(NodeState.DISABLED);
                s.merge(persistentState);
            } else if (NodeState.DISABLED == persistentState.getTargetState()) {
                persistentState.setTargetState(NodeState.INITIALIZED);
                s.merge(persistentState);
            }

            q = s.createQuery
                ("from NodePreferences tp where tp.tid = :tid");
            q.setParameter("tid", tid);
            nodePreferences = (NodePreferences)q.uniqueResult();
            return true;
        }

        public Object getResult() { return null; }

        public NodePersistentState getPersistentState()
        {
            return persistentState;
        }

        public NodePreferences getNodePreferences()
        {
            return nodePreferences;
        }
    }

    // private methods --------------------------------------------------------

    private void checkInstanceCount(NodeDesc nodeDesc)
        throws TooManyInstancesException
    {
        if (nodeDesc.isSingleInstance()) {
            String n = nodeDesc.getName();
            Policy p = nodeDesc.getTid().getPolicy();
            List<Tid> l = nodeManager.nodeInstances(n, p,false);

            if (1 == l.size()) {
                if (!tid.equals(l.get(0))) {
                    throw new TooManyInstancesException("too many instances: " + n);
                }
            } else if (1 < l.size()) {
                throw new TooManyInstancesException("too many instances: " + n);
            }
        }
    }

    private NodeContext startParent(String parent, Policy policy)
        throws DeployException
    {
        if (null == parent) {
            return null;
        }

        MackageDesc md = toolboxManager.mackageDesc(parent);

        if (null == md) {
            logger.warn("parent does not exist: " + parent);
            throw new DeployException("could not create parent: " + parent);
        }

        if (MackageDesc.Type.CASING == md.getType()) {
            policy = null;
        }

        logger.debug("Starting parent: " + parent + " for: " + tid);

        NodeContext pctx = getParentContext(parent);

        if (null == pctx) {
            logger.debug("Parent does not exist, instantiating");

            try {
                Tid parentTid = nodeManager.instantiate(parent, policy).getTid();
                pctx = nodeManager.nodeContext(parentTid);
            } catch (TooManyInstancesException exn) {
                pctx = getParentContext(parent);
            }
        }

        if (null == pctx) {
            throw new DeployException("could not create parent: " + parent);
        } else {
            return pctx;
        }
    }

    private NodeContext getParentContext(String parent)
    {
        for (Tid t : nodeManager.nodeInstances(parent)) {
            Policy p = t.getPolicy();
            if (null == p || p.equals(tid.getPolicy())) {
                return nodeManager.nodeContext(t);
            }

        }

        return null;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "NodeContext tid: " + tid
            + " (" + nodeDesc.getName() + ")";
    }
}
