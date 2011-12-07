/*
 * $Id$
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

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodePreferences;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.VnetSessionDesc;
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
    private final NodeId nodeId;
    private final NodePreferences nodePreferences;
    private final NodePersistentState persistentState;
    private final boolean isNew;

    private NodeBase node;
    private String packageName;

    private final NodeManager nodeManager;
    private final ToolboxManagerImpl toolboxManager;

    NodeContextImpl(URLClassLoader classLoader, NodeDesc tDesc, String packageName, boolean isNew) throws DeployException
    {
        UvmContextImpl mctx = UvmContextImpl.getInstance();

        if (null != tDesc.getNodeBase()) {
            mctx.schemaUtil().initSchema("settings", tDesc.getNodeBase());
        }
        mctx.schemaUtil().initSchema("settings", tDesc.getName());

        nodeManager = mctx.nodeManager();
        toolboxManager = mctx.toolboxManager();

        LoggingManagerImpl lm = mctx.loggingManager();
        if (null != tDesc.getNodeBase()) {
            lm.initSchema(tDesc.getNodeBase());
        }
        lm.initSchema(tDesc.getName());

        this.nodeDesc = tDesc;
        this.nodeId = nodeDesc.getNodeId();
        this.packageName = packageName;
        this.isNew = isNew;

        try {
            checkInstanceCount(nodeDesc);
        } catch (TooManyInstancesException exn) {
            throw new DeployException(exn);
        }

        if (isNew) {
            // XXX this isn't supposed to be meaningful:
            byte[] pKey = new byte[]
                { (byte)(nodeId.getId() & 0xFF),
                  (byte)((nodeId.getId() >> 8) & 0xFF) };


            persistentState = new NodePersistentState(nodeId, packageName, pKey);

            nodePreferences = new NodePreferences(nodeId);

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
            LoadSettings ls = new LoadSettings(nodeId);
            mctx.runTransaction(ls);
            this.persistentState = ls.getPersistentState();
            this.nodePreferences = ls.getNodePreferences();
        }

        logger.info("Creating node context for: " + nodeId + " (" + nodeDesc.getName() + ")");
    }

    void init(String[] args) throws DeployException
    {
        Set<NodeContext>parentCtxs = new HashSet<NodeContext>();
        List<String> parents = nodeDesc.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent, nodeId.getPolicy()));
        }

        UvmContextImpl uctx = UvmContextImpl.getInstance();

        final UvmContext mctx = UvmContextFactory.context();
        try {
            nodeManager.registerThreadContext(this);

            String nodeIdName = nodeId.getName();
            logger.debug("setting node " + nodeIdName + " log4j repository");

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

                            mctx.eventLogger().log(new NodeStateChange(nodeId, ts));
                        }
                    }
                });

            if (isNew) {
                node.initializeSettings();
                node.init(args);
                boolean enabled = toolboxManager.isEnabled(packageName);
                if (!enabled) {
                    node.disable();
                }
            } else {
                node.resumeState(persistentState.getTargetState(), args);
            }
        } catch (ClassNotFoundException exn) {
            logger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (InstantiationException exn) {
            logger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (IllegalAccessException exn) {
            logger.error("Exception during node initialization", exn);
            throw new DeployException(exn);
        } catch (Exception exn) {
            logger.error("Exception during node initialization", exn);
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

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
    }

    public NodePreferences getNodePreferences()
    {
        return nodePreferences;
    }

    public PackageDesc getPackageDesc()
    {
        return toolboxManager.packageDesc(packageName);
    }

    public Node node()
    {
        return node;
    }

    // node call-through methods -----------------------------------------

    public List<VnetSessionDesc> liveSessionDescs()
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
        return UvmContextFactory.context().runTransaction(tw);
    }

    public boolean resourceExists(String res)
    {
        String baseNodeName = nodeDesc.getNodeBase();
        return resourceExistsInt(res, getPackageDesc(), baseNodeName);
    }

    public InputStream getResourceAsStream(String res)
    {
        String baseNodeName = nodeDesc.getNodeBase();
        return getResourceAsStreamInt(res, getPackageDesc(), baseNodeName);
    }

    private boolean resourceExistsInt(String res, PackageDesc packageDesc,
                                      String baseNodeName)
    {
        boolean exists;
        try {
            URL url = new URL(toolboxManager.getResourceDir(packageDesc), res);
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
                PackageDesc baseDesc = toolboxManager.packageDesc(baseNodeName);
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
                                               PackageDesc packageDesc,
                                               String baseNodeName)
    {
        try {
            URL url = new URL(toolboxManager.getResourceDir(packageDesc), res);
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
                PackageDesc baseDesc = toolboxManager.packageDesc(baseNodeName);
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

    void destroy() throws Exception
    {
        try {
            nodeManager.registerThreadContext(this);
            if (node.getRunState() == NodeState.RUNNING) {
                node.stop();
            }
            node.destroy();
            node.destroySettings();
        } catch (Exception exn) {
            throw new Exception(exn);
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
                    nodeId.setPolicy(null);
                    s.update(nodeId);
                    s.delete(persistentState);
                    s.delete(getNodePreferences());
                    return true;
                }

                public Object getResult() { return null; }
            };
        UvmContextFactory.context().runTransaction(tw);
    }

    // private classes --------------------------------------------------------

    private class LoadSettings extends TransactionWork<Object>
    {
        private final NodeId nodeId;

        private NodePersistentState persistentState;
        private NodePreferences nodePreferences;

        public LoadSettings(NodeId nodeId)
        {
            this.nodeId = nodeId;
        }

        public boolean doWork(Session s)
        {
            Query q = s.createQuery("from NodePersistentState tps where tps.nodeId = :nodeId");
            q.setParameter("nodeId", nodeId);

            persistentState = (NodePersistentState)q.uniqueResult();

            if (!toolboxManager.isEnabled(packageName)) {
                persistentState.setTargetState(NodeState.DISABLED);
                s.merge(persistentState);
            } else if (NodeState.DISABLED == persistentState.getTargetState()) {
                persistentState.setTargetState(NodeState.INITIALIZED);
                s.merge(persistentState);
            }

            q = s.createQuery("from NodePreferences tp where tp.nodeId = :nodeId");
            q.setParameter("nodeId", nodeId);
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
            Policy p = nodeDesc.getNodeId().getPolicy();
            List<NodeId> l = nodeManager.nodeInstances(n, p,false);

            if (1 == l.size()) {
                if (!nodeId.equals(l.get(0))) {
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

        PackageDesc md = toolboxManager.packageDesc(parent);

        if (null == md) {
            logger.warn("parent does not exist: " + parent);
            throw new DeployException("could not create parent: " + parent);
        }

        if (PackageDesc.Type.CASING == md.getType()) {
            policy = null;
        }

        logger.debug("Starting parent: " + parent + " for: " + nodeId);

        NodeContext pctx = getParentContext(parent);

        if (null == pctx) {
            logger.debug("Parent does not exist, instantiating");

            try {
                NodeId parentNodeId = nodeManager.instantiate(parent, policy).getNodeId();
                pctx = nodeManager.nodeContext(parentNodeId);
            } catch (Exception exn) {
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
        for (NodeId t : nodeManager.nodeInstances(parent)) {
            Policy p = t.getPolicy();
            if (null == p || p.equals(nodeId.getPolicy())) {
                return nodeManager.nodeContext(t);
            }

        }

        return null;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "NodeContext nodeId: " + nodeId
            + " (" + nodeDesc.getName() + ")";
    }

    @SuppressWarnings("serial")
    private class TooManyInstancesException extends Exception
    {
        public TooManyInstancesException(String s)
        {
            super(s);
        }
    }
}
