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

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NodeManagerSettings;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.vnet.NodeBase;

/**
 * Implements <code>NodeContext</code>. Contains code to load and set
 * up a <code>Node</code>.
 */
public class NodeContextImpl implements NodeContext
{
    private final Logger logger = Logger.getLogger(getClass());

    private final NodeDesc nodeDesc;
    private final NodeSettings nodeSettings;
    private final boolean isNew;

    private NodeBase node;
    private String packageName;

    private final NodeManager nodeManager;
    private final ToolboxManagerImpl toolboxManager;

    public NodeContextImpl(URLClassLoader classLoader, NodeDesc nodeDesc, String packageName, boolean isNew) throws DeployException
    {
        UvmContextImpl mctx = UvmContextImpl.getInstance();

        if (null != nodeDesc.getNodeBase()) {
            mctx.schemaUtil().initSchema("settings", nodeDesc.getNodeBase());
        }
        mctx.schemaUtil().initSchema("settings", nodeDesc.getName());

        nodeManager = mctx.nodeManager();
        toolboxManager = mctx.toolboxManager();

        LoggingManagerImpl lm = mctx.loggingManager();
        if (null != nodeDesc.getNodeBase()) {
            lm.initSchema(nodeDesc.getNodeBase());
        }
        lm.initSchema(nodeDesc.getName());

        this.nodeDesc = nodeDesc;
        this.nodeSettings = nodeDesc.getNodeSettings();
        this.packageName = packageName;
        this.isNew = isNew;

        try {
            checkInstanceCount(nodeDesc);
        } catch (TooManyInstancesException exn) {
            throw new DeployException(exn);
        }

        logger.info("Creating node context for: " + nodeSettings + " (" + nodeDesc.getName() + ")");
    }

    void init() throws DeployException
    {
        Set<NodeContext>parentCtxs = new HashSet<NodeContext>();
        List<String> parents = nodeDesc.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent, nodeSettings.getPolicyId()));
        }

        UvmContextImpl uctx = UvmContextImpl.getInstance();

        final UvmContext mctx = UvmContextFactory.context();
        try {
            nodeManager.registerThreadContext(this);

            String nodeSettingsName = nodeSettings.getNodeName();
            logger.debug("setting node " + nodeSettingsName + " log4j repository");

            String className = nodeDesc.getClassName();
            node = (NodeBase)Class.forName(className).newInstance();

            for (NodeContext parentCtx : parentCtxs) {
                node.addParent((NodeBase)parentCtx.node());
            }

            if (isNew) {
                node.initializeSettings();
                node.init();
                boolean enabled = toolboxManager.isEnabled(packageName);
                if (!enabled) {
                    node.disable();
                }
            } else {
                node.resumeState(nodeSettings.getTargetState());
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
        }
    }

    // NodeContext -------------------------------------------------------

    public NodeSettings getNodeSettings()
    {
        return nodeSettings;
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
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

    public NodeSettings.NodeState getRunState()
    {
        return null == node ? NodeSettings.NodeState.LOADED : node.getRunState();
    }

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

    private boolean resourceExistsInt(String res, PackageDesc packageDesc, String baseNodeName)
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

    private InputStream getResourceAsStreamInt(String res, PackageDesc packageDesc, String baseNodeName)
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

    protected void destroy() throws Exception
    {
        try {
            nodeManager.registerThreadContext(this);
            if (node.getRunState() == NodeSettings.NodeState.RUNNING) {
                node.stop();
            }
            node.destroy();
            node.destroySettings();
        } catch (Exception exn) {
            throw new Exception(exn);
        } finally {
            nodeManager.deregisterThreadContext();
        }

        logger.error("FIXME REMOVE SETTINGS FROM NODE MANAGER");
    }

    protected void unload()
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

    // private methods --------------------------------------------------------

    private void checkInstanceCount(NodeDesc nodeDesc) 
        throws TooManyInstancesException
    {
        if (nodeDesc.isSingleInstance()) {
            String nodeName = nodeDesc.getName();
            Long policyId = nodeDesc.getNodeSettings().getPolicyId();
            List<NodeSettings> l = nodeManager.nodeInstances( nodeName, policyId, false );

            if (1 == l.size()) {
                if (!nodeSettings.equals(l.get(0))) {
                    throw new TooManyInstancesException("too many instances: " + nodeName);
                }
            } else if (1 < l.size()) {
                throw new TooManyInstancesException("too many instances: " + nodeName);
            }
        }
    }

    private NodeContext startParent(String parent, Long policyId)
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
            policyId = null;
        }

        logger.debug("Starting parent: " + parent + " for: " + nodeSettings);

        NodeContext pctx = getParentContext(parent);

        if (null == pctx) {
            logger.debug("Parent does not exist, instantiating");

            try {
                NodeSettings parentNodeSettings = nodeManager.instantiate(parent, policyId).getNodeSettings();
                pctx = nodeManager.nodeContext(parentNodeSettings);
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
        for (NodeSettings nSettings : nodeManager.nodeInstances(parent)) {
            Long policyId = nSettings.getPolicyId();
            if (policyId == null || policyId.equals(nodeSettings.getPolicyId())) {
                return nodeManager.nodeContext(nSettings);
            }

        }

        return null;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "NodeContext nodeSettings: " + nodeSettings + " (" + nodeDesc.getName() + ")";
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
