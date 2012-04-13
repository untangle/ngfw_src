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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
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

    private final NodeProperties nodeProperties;
    private final NodeSettings nodeSettings;
    private final boolean isNew;

    private NodeBase node;
    private String packageName;

    public NodeContextImpl(NodeProperties nodeProperties, NodeSettings nodeSettings, String packageName, boolean isNew) throws DeployException
    {
        LoggingManagerImpl lm = UvmContextImpl.getInstance().loggingManager();
        if (null != nodeProperties.getNodeBase()) {
            lm.initSchema(nodeProperties.getNodeBase());
        }
        lm.initSchema(nodeProperties.getName());

        this.nodeProperties = nodeProperties;
        this.nodeSettings = nodeSettings;
        this.packageName = packageName;
        this.isNew = isNew;

        try {
            checkInstanceCount(nodeProperties);
        } catch (TooManyInstancesException exn) {
            throw new DeployException(exn);
        }

        logger.info("Creating node context for: " + nodeSettings + " (" + nodeProperties.getName() + ")");
    }

    void init() throws DeployException
    {
        Set<NodeContext>parentCtxs = new HashSet<NodeContext>();
        List<String> parents = nodeProperties.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent, nodeSettings.getPolicyId()));
        }

        UvmContextImpl uctx = UvmContextImpl.getInstance();

        final UvmContext mctx = UvmContextFactory.context();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeSettings.getId());

            String nodeSettingsName = nodeSettings.getNodeName();
            logger.debug("setting node " + nodeSettingsName + " log4j repository");

            String className = nodeProperties.getClassName();
            java.lang.reflect.Constructor constructor = Class.forName(className).getConstructor(new Class[]{NodeSettings.class, NodeProperties.class});
            node = (NodeBase)constructor.newInstance(getNodeSettings(), getNodeProperties());
            //node = (NodeBase)Class.forName(className).newInstance(getNodeSettings(), getNodeProperties());
            node.setNodeContext(this);
            node.setNodeProperties(this.getNodeProperties());
            node.setNodeSettings(this.getNodeSettings());
                
            
            for (NodeContext parentCtx : parentCtxs) {
                node.addParent((NodeBase)parentCtx.node());
            }

            if (isNew) {
                node.initializeSettings();
                node.init();
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
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    // NodeContext -------------------------------------------------------

    public NodeSettings getNodeSettings()
    {
        return nodeSettings;
    }

    public NodeProperties getNodeProperties()
    {
        return nodeProperties;
    }

    public PackageDesc getPackageDesc()
    {
        return UvmContextImpl.getInstance().toolboxManager().packageDesc(packageName);
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
        String baseNodeName = nodeProperties.getNodeBase();
        return resourceExistsInt(res, getPackageDesc(), baseNodeName);
    }

    public InputStream getResourceAsStream(String res)
    {
        String baseNodeName = nodeProperties.getNodeBase();
        return getResourceAsStreamInt(res, getPackageDesc(), baseNodeName);
    }

    private boolean resourceExistsInt(String res, PackageDesc packageDesc, String baseNodeName)
    {
        boolean exists;
        try {
            URL url = new URL(UvmContextImpl.getInstance().toolboxManager().getResourceDir(packageDesc), res);
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
                PackageDesc baseDesc = UvmContextImpl.getInstance().toolboxManager().packageDesc(baseNodeName);
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
            URL url = new URL(UvmContextImpl.getInstance().toolboxManager().getResourceDir(packageDesc), res);
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
                PackageDesc baseDesc = UvmContextImpl.getInstance().toolboxManager().packageDesc(baseNodeName);
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
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeSettings.getId());
            if (node.getRunState() == NodeSettings.NodeState.RUNNING) {
                node.stop();
            }
            node.destroy();
            node.destroySettings();
        } catch (Exception exn) {
            throw new Exception(exn);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    protected void unload()
    {
        if (node != null) {
            try {
                UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeSettings.getId());
                node.unload();
            } finally {
                UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            }
        }
    }

    // private methods --------------------------------------------------------

    private void checkInstanceCount(NodeProperties nodeProperties) 
        throws TooManyInstancesException
    {
        if (nodeProperties.isSingleInstance()) {
            String nodeName = nodeProperties.getName();
            Long policyId = nodeSettings.getPolicyId();
            List<NodeSettings> l = UvmContextImpl.getInstance().nodeManager().nodeInstances( nodeName, policyId, false );

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

        PackageDesc md = UvmContextImpl.getInstance().toolboxManager().packageDesc(parent);

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
                NodeSettings parentNodeSettings = UvmContextImpl.getInstance().nodeManager().instantiate(parent, policyId);
                pctx = UvmContextImpl.getInstance().nodeManager().nodeContext(parentNodeSettings);
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
        for (NodeSettings nSettings : UvmContextImpl.getInstance().nodeManager().nodeInstances(parent)) {
            Long policyId = nSettings.getPolicyId();
            if (policyId == null || policyId.equals(nodeSettings.getPolicyId())) {
                return UvmContextImpl.getInstance().nodeManager().nodeContext(nSettings);
            }

        }

        return null;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "NodeContext nodeSettings: " + nodeSettings + " (" + nodeProperties.getName() + ")";
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
