/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.logging.UvmLoggingContext;
import com.untangle.uvm.logging.UvmLoggingContextFactory;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeInstantiated;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyRule;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implements NodeManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class NodeManagerImpl implements NodeManager, UvmLoggingContextFactory
{
    private static final String DESC_PATH = "META-INF/uvm-node.xml";

    private final Logger logger = Logger.getLogger(getClass());

    private final NodeManagerState nodeManagerState;
    private final Map<NodeId, NodeContextImpl> nodeIds = new ConcurrentHashMap<NodeId, NodeContextImpl>();
    private final ThreadLocal<NodeContext> threadContexts = new InheritableThreadLocal<NodeContext>();
    private final UvmRepositorySelector repositorySelector;

    private final Pulse cleanerPulse = new Pulse("session-cleaner", true, new SessionExpirationWorker());
    
    private Map<String,Set<String>> enabledNodes = new HashMap<String,Set<String>>();

    private boolean live = true;

    /*
     * Update this value to a new long whenever clearing enabled nodes.  This way
     * it is possible to quickly determine if an enabled nodes lookup should be cached
     * without synchronizing the entire operation.
     */
    private long enabledNodesCleared = 0;

    NodeManagerImpl(UvmRepositorySelector repositorySelector)
    {
        this.repositorySelector = repositorySelector;

        TransactionWork<NodeManagerState> tw = new TransactionWork<NodeManagerState>()
            {
                private NodeManagerState tms;

                public boolean doWork(Session s) throws SQLException
                {
                    Query q = s.createQuery("from NodeManagerState tms");
                    tms = (NodeManagerState)q.uniqueResult();
                    if (null == tms) {
                        tms = new NodeManagerState();
                        s.save(tms);
                    }
                    return true;
                }

                public NodeManagerState getResult() { return tms; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
        this.nodeManagerState = tw.getResult();
    }

    // NodeManager ------------------------------------------------------

    public List<NodeId> nodeInstances()
    {
        List<NodeId> l = new ArrayList<NodeId>(nodeIds.keySet());

        // only reports requires sorting
        // XXX the client should do its own sorting
        Collections.sort(l, new Comparator<NodeId>() {
            public int compare(NodeId t1, NodeId t2) {
                NodeContextImpl tci1 = nodeIds.get(t1);
                NodeContextImpl tci2 = nodeIds.get(t2);
                int rpi1 = tci1.getPackageDesc().getViewPosition();
                int rpi2 = tci2.getPackageDesc().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getPackageDesc().getName().compareToIgnoreCase(tci2.getPackageDesc().getName());
                } else if (rpi1 < rpi2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return l;
    }

    public List<NodeId> nodeInstances(String packageName)
    {
        List<NodeId> l = new LinkedList<NodeId>();

        for (NodeId nodeId : nodeIds.keySet()) {
            NodeContext tc = nodeIds.get(nodeId);
            if (null != tc) {
                if (tc.getNodeDesc().getName().equals(packageName)) {
                    l.add(nodeId);
                }
            }
        }

        return l;
    }

    public List<NodeId> nodeInstances(String name, Policy policy)
    {
        return nodeInstances(name,policy,true);
    }

    public List<NodeId> nodeInstances(String name, Policy policy, boolean parents)
    {
        List<NodeId> l = new ArrayList<NodeId>(nodeIds.size());

        for (NodeId nodeId : getNodesForPolicy(policy, parents)) {
            NodeContext tc = nodeIds.get(nodeId);
            if (null != tc) {
                String n = tc.getNodeDesc().getName();

                if (n.equals(name)) {
                    l.add(nodeId);
                }
            }
        }

        return l;
    }

    public List<NodeId> nodeInstances(Policy policy)
    {
        return getNodesForPolicy(policy);
    }

    public List<NodeDesc> visibleNodes(Policy policy)
    {
        List<NodeId> nodeIds = nodeInstances();
        List<NodeDesc> l = new ArrayList<NodeDesc>(nodeIds.size());

        for (NodeId nodeId : getNodesForPolicy(policy)) {
            NodeContext nc = nodeContext(nodeId);
            PackageDesc md = nc.getPackageDesc();

            if (!md.isInvisible()) {
                NodeDesc nd = nc.getNodeDesc();
                l.add(nd);
            }
        }

        for (NodeId nodeId : nodeIds) {
            NodeContext nc = nodeContext(nodeId);
            PackageDesc md = nc.getPackageDesc();

            PackageDesc.Type type = md.getType();
            if (!md.isInvisible() && PackageDesc.Type.SERVICE == type) {
                NodeDesc nd = nc.getNodeDesc();
                l.add(nd);
            }
        }

        return l;
    }

    public NodeContextImpl nodeContext(NodeId nodeId)
    {
        return nodeIds.get(nodeId);
    }

    public Node node(String name)
    {
        Node node = null;
        List<NodeId> nodeInstances = nodeInstances(name);
        if(nodeInstances.size()>0){
            NodeContext nodeContext = nodeContext(nodeInstances.get(0));
            node = nodeContext.node();
        }
        return node;
    }

    public NodeDesc instantiate(String nodeName) throws DeployException
    {
        Policy policy = getDefaultPolicyForNode(nodeName);
        return instantiate(nodeName, policy, new String[0]);
    }

    public NodeDesc instantiate(String nodeName, String[] args) throws DeployException
    {
        Policy policy = getDefaultPolicyForNode(nodeName);
        return instantiate(nodeName, policy, args);
    }

    public NodeDesc instantiate(String nodeName, Policy policy) throws DeployException
    {
        return instantiate(nodeName, policy, new String[0]);
    }

    public NodeDesc instantiate(String nodeName, Policy p, String[] args) throws DeployException
    {
        UvmContextImpl uvmContext = UvmContextImpl.getInstance();
        ToolboxManagerImpl tbm = uvmContext.toolboxManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);

        if (PackageDesc.Type.SERVICE == packageDesc.getType()) {
            p = null;
        }

        /**
         * Check if this type of node already exists in this rack
         * If so, throw an exception (more info in Bug #7801)
         */
        for (NodeId nodeId : getNodesForPolicy(p,false)) {
            NodeContext nc = nodeContext(nodeId);
            PackageDesc md = nc.getPackageDesc();

            if (md.getName().equals(packageDesc.getName())) {
                throw new DeployException("Node " + packageDesc.getName() + " already exists in Policy " + p + ".");
            }
        }

        
        NodeContextImpl tc;
        NodeDesc nodeDesc;
        synchronized (this) {
            //test if not duplicated
            List<NodeId> instancesList=this.nodeInstances(nodeName,p,false);
            if(instancesList.size()>0) {
                logger.warn("A node instance already exists for " + nodeName + " under " + p + " policy; will not instantiate another one.");
                return null; //return if the node is already installed
            }
            NodeId nodeId = newNodeId(p, nodeName);

            URL[] resUrls = new URL[] { tbm.getResourceDir(packageDesc) };

            logger.info("initializing node desc for: " + nodeName);
            nodeDesc = initNodeDesc(packageDesc, resUrls, nodeId);

            if (!live) {
                throw new DeployException("NodeManager is shut down");
            }

            /* load annotated classes */
            if (nodeDesc != null) {
                List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
                boolean classAdded = false;
                if (annotatedClasses != null) {
                    for (String clz : annotatedClasses) {
                        classAdded |= UvmContextImpl.getInstance().addAnnotatedClass(clz);
                    }
                }
                if (classAdded)
                    UvmContextImpl.getInstance().refreshSessionFactory();
            }

            tc = new NodeContextImpl((URLClassLoader)getClass().getClassLoader(), nodeDesc, packageDesc.getName(), true);
            nodeIds.put(nodeId, tc);
            try {
                tc.init(args);
            } finally {
                if (null == tc.node()) {
                    nodeIds.remove(nodeId);
                }
            }
        }

        Node node = tc.node();
        PackageDesc.Type type = packageDesc.getType();

        if (null != node && !packageDesc.isInvisible() && (PackageDesc.Type.NODE == type || PackageDesc.Type.SERVICE == type)) {
            MessageManager lmm = uvmContext.messageManager();
            Counters c = lmm.getCounters(node.getNodeId());

            NodeInstantiated ne = new NodeInstantiated(nodeDesc, c.getStatDescs(), uvmContext.licenseManager().getLicense(packageDesc.getName()));
            MessageManager mm = uvmContext.messageManager();
            mm.submitMessage(ne);
        }

        clearEnabledNodes();
        
        return nodeDesc;
    }

    public NodeDesc instantiateAndStart(String nodeName, Policy p) throws DeployException
    {
        NodeDesc nd = instantiate(nodeName, p);
        if (nd.getAutoStart()) {
            NodeContext nc = nodeContext(nd.getNodeId());
            try {
                nc.node().start();
            } catch (Exception e) {
                throw new DeployException(e);
            }
                
        }
        return nd;
    }

    public void destroy(final NodeId nodeId) throws Exception
    {
        final NodeContextImpl tc;

        synchronized (this) {
            tc = nodeIds.get(nodeId);
            if (null == tc) {
                logger.error("Destroy Failed: " + nodeId + " not found");
                throw new Exception("Node " + nodeId + " not found");
            }
            tc.destroy();

            nodeIds.remove(nodeId);
        }

        tc.destroyPersistentState();
        
        clearEnabledNodes();

        return;
    }

    public Map<NodeId, NodeState> allNodeStates()
    {
        HashMap<NodeId, NodeState> result = new HashMap<NodeId, NodeState>();
        for (Iterator<NodeId> iter = nodeIds.keySet().iterator(); iter.hasNext();) {
            NodeId nodeId = iter.next();
            NodeContextImpl tci = nodeIds.get(nodeId);
            result.put(nodeId, tci.getRunState());
        }

        return result;
    }

    /**
     * Get a map of nodes that are enabled for a policy, this takes into account
     * parent / child relationships
     */
    @Override
    public Set<String> getEnabledNodes(Policy policy)
    {
        if ( policy == null ) {
            return Collections.emptySet();
        }
        
        Set<String> policyNodes = null;
        String policyName = policy.getName();
        long enabledNodesCleared = 0;
        
        /* With the lock, check if there is an entry and return it if exists.
         * Otherwise, create an
         */
        synchronized ( this.enabledNodes ) {
            policyNodes = this.enabledNodes.get(policyName);
            enabledNodesCleared = this.enabledNodesCleared ;
        }

        if ( policyNodes == null ) {
            policyNodes = new HashSet<String>();
            List<NodeId> policyNodeIds = getNodesForPolicy(policy, true);

            for ( NodeId nodeId : policyNodeIds ) {
                NodeContext nodeContext = nodeIds.get(nodeId);
                if ( nodeContext == null ) {
                    logger.warn( "Node context is null for nodeId: " + nodeId );
                    continue;
                }

                if ( nodeContext.getRunState() == NodeState.RUNNING ) {
                    policyNodes.add(nodeId.getNodeName());
                }
            }
            
            synchronized( this.enabledNodes ) {
                if ( enabledNodesCleared == this.enabledNodesCleared ) {
                    this.enabledNodes.put(policyName,policyNodes);
                }
            }
                

        }
        
        return policyNodes;
    }
    
    @Override
    public void flushNodeStateCache()
    {
        this.clearEnabledNodes();
    }

    public boolean isInstantiated(String nodeName)
    {
        return (this.node(nodeName) != null);
    }

    // Manager lifetime -------------------------------------------------------

    void init()
    {
        restartUnloaded();
        
        clearEnabledNodes();

        cleanerPulse.start(60000);
    }

    void destroy()
    {
        cleanerPulse.stop();

        synchronized (this) {
            live = false;

            Set<NodeId> s = new HashSet<NodeId>(nodeIds.keySet());

            for ( NodeId nodeId : s ) {
                if (null != nodeId) {
                    unload(nodeId);
                }
            }

            if (nodeIds.size() > 0) {
                logger.warn("node instances not destroyed: " + nodeIds.size());
            }
        }

        logger.info("NodeManager destroyed");
    }

    // NodeManager methods -----------------------------------------------

    public NodeContext threadContext()
    {
        return threadContexts.get();
    }

    public void registerThreadContext(NodeContext ctx)
    {
        threadContexts.set(ctx);
        repositorySelector.setContextFactory(this);
    }

    public void deregisterThreadContext()
    {
        threadContexts.remove();
        repositorySelector.uvmContext();        
    }

    // UvmLoggingContextFactory methods ---------------------------------------

    public UvmLoggingContext get()
    {
        final NodeContext nodeContext = threadContexts.get();
        if (null == nodeContext) {
            LogLog.warn("null node context in threadContexts");
        }

        return new NodeManagerLoggingContext(nodeContext);
    }

    // package protected methods ----------------------------------------------

    void unload(NodeId nodeId)
    {
        synchronized (this) {
            NodeContextImpl tc = nodeIds.get(nodeId);
            logger.info("Unloading: " + nodeId + " (" + tc.getNodeDesc().getName() + ")");

            tc.unload();
            nodeIds.remove(nodeId);
        }
        
        clearEnabledNodes();
    }

    void restart(String name)
    {
        ToolboxManager tbm = LocalUvmContextFactory.context().toolboxManager();

        PackageDesc pd = tbm.packageDesc(name);
        if (pd == null) {
            logger.warn("Failed to restart: Unable to find package \"" + name + "\"");
            return;
        }
        String availVer = pd.getInstalledVersion();

        synchronized (this) {
            List<NodeId> nNodeIds = nodeInstances(name);
            if (0 < nNodeIds.size()) {
                NodeId t = nNodeIds.get(0);
                NodeContext tc = nodeIds.get(t);

                for (NodeId nodeId : nNodeIds) {
                    PackageDesc md = nodeIds.get(nodeId).getPackageDesc();
                    if (!md.getInstalledVersion().equals(availVer)) {
                        logger.info("Restarting \"" + name + "\" - new version available. (" + availVer + " > " + md.getInstalledVersion() + ")");
                        unload(nodeId);
                    } else {
                        logger.info("Skipping Restart \"" + name + "\" - no new version available. (" + availVer + " = " + md.getInstalledVersion() + ")");
                    }
                }
                restartUnloaded();
            }
        }
        
        clearEnabledNodes();
    }

    void startAutoStart(PackageDesc extraPkg)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)LocalUvmContextFactory.context().toolboxManager();

        List<PackageDesc> mds = new ArrayList<PackageDesc>();

        for (PackageDesc md : tbm.installed()) {
            if (md.isAutoStart()) {
                mds.add(md);
            }
        }

        if (null != extraPkg && extraPkg.isAutoStart()) {
            mds.add(extraPkg);
        }
        for (PackageDesc md : mds) {
            List<NodeId> l = nodeInstances(md.getName());

            NodeId t = null;

            if (0 == l.size()) {
                try {
                    logger.info("Auto-starting new node: " + md.getName());
                    t = instantiate(md.getName()).getNodeId();
                } catch (DeployException exn) {
                    logger.warn("could not deploy: " + md.getName(), exn);
                    continue;
                }
            } else {
                t = l.get(0);
            }

            NodeContext nc = nodeContext(t);
            if (null == nc) {
                logger.warn("No node context for router nodeId: " + t);
            } else {
                Node n = nc.node();
                NodeState ns = n.getRunState();
                switch (ns) {
                case INITIALIZED:
                    try {
                        n.start();
                    } catch (Exception exn) {
                        logger.warn("could not load: " + md.getName(), exn);
                        continue;
                    }
                    break;
                case RUNNING:
                    // nothing left to do.
                    break;
                default:
                    logger.warn(md.getName() + " unexpected state: " + ns);
                    break;
                }
            }
        }
        
        clearEnabledNodes();
    }

    // private methods --------------------------------------------------------

    private void restartUnloaded()
    {
        long t0 = System.currentTimeMillis();

        if (!live) {
            throw new RuntimeException("NodeManager is shut down");
        }

        logger.info("Restarting unloaded nodes...");

        List<NodePersistentState> unloaded = getUnloaded();
        Map<NodeId, NodeDesc> nodeDescs = loadNodeDescs(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        while (0 < unloaded.size()) {
            List<NodePersistentState> startQueue = getLoadable(unloaded, nodeDescs, loadedParents);
            logger.info("loadable in this pass: " + startQueue);
            if (0 == startQueue.size()) {
                logger.info("not all parents loaded, proceeding");
                for (NodePersistentState n : unloaded) {
                    List<NodePersistentState> l = Collections.singletonList(n);
                    startUnloaded(l, nodeDescs, loadedParents);
                }
                break;
            }

            startUnloaded(startQueue, nodeDescs, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart nodes: " + (t1 - t0) + " millis");

        startAutoStart(null);
        
        clearEnabledNodes();
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodePersistentState> startQueue, Map<NodeId, NodeDesc> nodeDescs, Set<String> loadedParents)
    {
        ToolboxManager tbm = LocalUvmContextFactory.context().toolboxManager();

        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (NodePersistentState tps : startQueue) {
            final NodeDesc nodeDesc = nodeDescs.get(tps.getNodeId());
            final NodeId nodeId = tps.getNodeId();
            final String name = tps.getName();
            loadedParents.add(name);
            final String[] args = tps.getArgArray();
            final PackageDesc packageDesc = tbm.packageDesc(name);

            if (nodeDesc != null) {
                List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
                if (annotatedClasses != null) {
                    for (String clz : annotatedClasses) {
                        UvmContextImpl.getInstance().addAnnotatedClass(clz);
                    }
                }
                UvmContextImpl.getInstance().refreshSessionFactory();
            }

            if (packageDesc != null) {
                Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            logger.info("Restarting: " + nodeId + " (" + name + ")");
                            NodeContextImpl tc = null;
                            try {
                                tc = new NodeContextImpl((URLClassLoader)getClass().getClassLoader(),nodeDesc,packageDesc.getName(),false);
                                nodeIds.put(nodeId, tc);
                                tc.init(args);
                                logger.info("Restarted: " + nodeId);
                            } catch (Exception exn) {
                                logger.error("Could not restart: " + nodeId, exn);
                            } catch (LinkageError err) {
                                logger.error("Could not restart: " + nodeId, err);
                            }
                            if (null != tc && null == tc.node()) {
                                nodeIds.remove(nodeId);
                            }
                        }
                    };
                restarters.add(r);
            } else {
                logger.error("Unable to find node \"" + name + "\" - Skipping");
            }
        }

        Set<Thread> threads = new HashSet<Thread>(restarters.size());
        int loadLimit = Runtime.getRuntime().availableProcessors() << 1;
        try {
            for (Iterator<Runnable> riter = restarters.iterator(); riter.hasNext();) {
                while (getRunnableCount(threads) < loadLimit && riter.hasNext()) {
                    Thread t = LocalUvmContextFactory.context().
                        newThread(riter.next(), "START_" + startThreadNum++);
                    threads.add(t);
                    t.start();
                }
                if (riter.hasNext())
                    Thread.sleep(200);
            }
            // Must wait for them to start before we can go on to next wave.
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException exn) {
            logger.error("Interrupted while starting transforms"); // Give up
        }
        
        clearEnabledNodes();
    }

    private int getRunnableCount(Set<Thread> threads) {
        int result = 0;
        for (Iterator<Thread> iter = threads.iterator(); iter.hasNext();) {
            Thread t = iter.next();
            if (!t.isAlive()) {
                // logger.info("Thread " + t.getName() + " is dead, removing.");
                iter.remove();
            } else {
                Thread.State state = t.getState();
                // logger.info("Thread " + t.getName() + " is in state " + t.getState());
                if (state == Thread.State.RUNNABLE)
                    result++;
            }
        }
        return result;
    }

    private List<NodePersistentState> getLoadable(List<NodePersistentState> unloaded, Map<NodeId, NodeDesc> nodeDescs, Set<String> loadedParents)
    {
        List<NodePersistentState> l = new ArrayList<NodePersistentState>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<NodePersistentState> i = unloaded.iterator(); i.hasNext(); ) {
            NodePersistentState tps = i.next();
            NodeId nodeId = tps.getNodeId();
            NodeDesc nodeDesc = nodeDescs.get(nodeId);
            if (null == nodeDesc) {
                logger.warn("Missing NodeDesc for: " + nodeId);
                continue;
            }

            List<String> parents = nodeDesc.getParents();

            boolean parentsLoaded = true;
            for (String parent : parents) {
                if (!loadedParents.contains(parent)) {
                    parentsLoaded = false;
                }
                if (false == parentsLoaded) { break; }
            }

            String name = nodeDesc.getName();

            // all parents loaded and another instance of this
            // node not loading this pass or already loaded in
            // previous pass (prevents classloader race).
            if (parentsLoaded && (!thisPass.contains(name) || loadedParents.contains(name))) {
                i.remove();
                l.add(tps);
                thisPass.add(name);
            }
        }

        return l;
    }

    private Map<NodeId, NodeDesc> loadNodeDescs(List<NodePersistentState> unloaded)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)LocalUvmContextFactory.context().toolboxManager();

        Map<NodeId, NodeDesc> nodeDescs = new HashMap<NodeId, NodeDesc>(unloaded.size());

        for (NodePersistentState tps : unloaded) {
            String name = tps.getName();
            logger.info("Getting mackage desc for: " + name);
            PackageDesc md = tbm.packageDesc(name);
            if (null == md) {
                logger.warn("could not get mackage desc for: " + name);
                continue;
            }

            URL[] urls = new URL[] { tbm.getResourceDir(md) };
            NodeId nodeId = tps.getNodeId();
            nodeId.setNodeName(name);

            try {
                logger.info("initializing node desc for: " + name);
                NodeDesc nodeDesc = initNodeDesc(md, urls, nodeId);
                nodeDescs.put(nodeId, nodeDesc);
            } catch (DeployException exn) {
                logger.warn("NodeDesc could not be parsed", exn);
            }
        }

        return nodeDescs;
    }

    @SuppressWarnings("unchecked") //Query
    private List<NodePersistentState> getUnloaded()
    {
        final List<NodePersistentState> unloaded = new LinkedList<NodePersistentState>();

        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from NodePersistentState tps");
                    List<NodePersistentState> result = q.list();

                    for (NodePersistentState persistentState : result) {
                        if (!nodeIds.containsKey(persistentState.getNodeId())) {
                            unloaded.add(persistentState);
                        }
                    }
                    return true;
                }

                public Void getResult() {
                    return null;
                }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        return unloaded;
    }

    /**
     * Initialize NodeDesc
     *
     * @param urls urls to find node descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private NodeDesc initNodeDesc(PackageDesc packageDesc, URL[] urls, NodeId nodeId) throws DeployException
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        NodeDesc nodeDesc = null;
        try {
            String fileName = System.getProperty("uvm.lib.dir") + "/" + packageDesc.getName() + "/" + "nodeDesc";
            nodeDesc = settingsManager.load( NodeDesc.class, fileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        if (nodeDesc != null) {
            nodeDesc.setNodeId(nodeId);

            //             List<String> annotatedClasses = nodeDesc.getAnnotatedClasses();
            //             if (annotatedClasses != null) {
            //                 for (String clz : annotatedClasses) {
            //                     UvmContextImpl.getInstance().addAnnotatedClass(clz);
            //                 }
            //             }
            //UvmContextImpl.getInstance().refreshSessionFactory();

            return nodeDesc;
        }

        return null;
    }

    private Policy getDefaultPolicyForNode(String nodeName) throws DeployException
    {
        ToolboxManager tbm = LocalUvmContextFactory.context().toolboxManager();
        PackageDesc packageDesc = tbm.packageDesc(nodeName);
        if (packageDesc == null)
            throw new DeployException("Node named " + nodeName + " not found");
        if (PackageDesc.Type.SERVICE != packageDesc.getType()) {
            return null;
        } else {
            return LocalUvmContextFactory.context().policyManager().getDefaultPolicy();
        }
    }

    private NodeId newNodeId(Policy policy, String nodeName) throws DeployException
    {
        final NodeId nodeId;
        synchronized (nodeManagerState) {
            nodeId = nodeManagerState.nextNodeId(policy, nodeName);
        }

        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    s.merge(nodeManagerState);
                    s.save(nodeId);
                    return true;
                }

                public Object getResult() { return null; }
            };
        if (!LocalUvmContextFactory.context().runTransaction(tw))
            // We cannot return the new nodeId if updating the database failed,
            // as that would break the invariant of multiple nodes not having
            // the same nodeId.
            throw new DeployException("Unable to allocate new nodeId");

        return nodeId;
    }

    private List<Policy> getAllPolicies(Policy p)
    {
        PolicyManager lpi = LocalUvmContextFactory.context().policyManager();

        List<Policy> l = new ArrayList<Policy>();
        while (null != p) {
            l.add(p);
            p = lpi.getParent(p);
        }

        return l;
    }

    private List<NodeId> getNodesForPolicy(Policy policy)
    {
        return getNodesForPolicy(policy,true);
    }

    private List<NodeId> getNodesForPolicy(Policy policy,boolean parents)
    {
        List<Policy> policies = null;

        if (parents) {
            if (policy == null) {
                policies = new ArrayList<Policy>(1);
                policies.add(null);
            } else {
                policies = getAllPolicies(policy);
            }
        } else {
            policies = new ArrayList<Policy>(1);
            policies.add(policy);
        }

        /*
         * This is a list of nodeIds.  Each index of the first list corresponds to its
         * policy in the policies array.  Each index in the second list is a nodeId of the nodes
         * in the policy
         * ll[0] == list of nodeIds in policies[0]
         * ll[1] == list of nodeIds in policies[1]
         * ...
         * ll[n] == list of nodeIds in policies[n]
         * Policies are ordered ll[0] is the current policy, ll[1] is its parent.
         */
        List<List<NodeId>> ll = new ArrayList<List<NodeId>>(policies.size());
        for (int i = 0; i < policies.size(); i++) {
            ll.add(new ArrayList<NodeId>());
        }

        /*
         * Fill in the inner list, at the end each of these is the list of 
         * nodes in the policy.
         */
        for (NodeId nodeId : nodeIds.keySet()) {
            NodeContext tc = nodeIds.get(nodeId);

            if (null != tc) {
                Policy p = nodeId.getPolicy();

                int i = policies.indexOf(p);
                if (0 <= i) {
                    List<NodeId> tl = ll.get(i);
                    tl.add(nodeId);
                }
            }
        }

        /*
         * Strip out duplicates.  By iterating the list in order, this
         * will only add the first entry (which will be most specific node.
         */
        List<NodeId> l = new ArrayList<NodeId>(nodeIds.size());
        Set<String> names = new HashSet<String>();

        for (List<NodeId> tl : ll) {
            if (null != tl) {
                for (NodeId t : tl) {
                    String n = t.getNodeName();
                    if (!names.contains(n)) {
                        names.add(n);
                        l.add(t);
                    }
                }
            }
        }

        return l;
    }
    
    /**
     * Used to empty the cache of the enabled nodes.  This cache is used to build
     * the pipeline, so it must be updated whenever the node state changes.
     */
    private void clearEnabledNodes()
    {
        logger.debug( "clearing the cache of enabled nodes." );
        synchronized ( this.enabledNodes ) {
            this.enabledNodes.clear();
            this.enabledNodesCleared = System.nanoTime();
        }
    }

    // private static classes -------------------------------------------------

    private static class NodeManagerLoggingContext implements UvmLoggingContext
    {
        private final NodeContext nodeContext;

        // constructors -------------------------------------------------------

        NodeManagerLoggingContext(NodeContext nodeContext)
        {
            this.nodeContext = nodeContext;
        }

        // UvmLoggingContext methods -----------------------------------------

        public String getConfigName()
        {
            return "log4j-node.xml";
        }

        public String getFileName()
        {
            if (null == nodeContext) {
                return "0";
            } else {
                return nodeContext.getNodeId().getName();
            }
        }

        public String getName()
        {
            if (null == nodeContext) {
                return "0";
            } else {
                return nodeContext.getNodeId().getName();
            }
        }

        // Object methods -----------------------------------------------------

        public boolean equals(Object o)
        {
            if (o instanceof NodeManagerLoggingContext) {
                NodeManagerLoggingContext tmc
                    = (NodeManagerLoggingContext)o;
                return nodeContext.equals(tmc.nodeContext);
            } else {
                return false;
            }
        }

        public int hashCode()
        {
            return nodeContext.hashCode();
        }
    }

    private static class SessionExpirationWorker implements Runnable
    {
        ExpiredPolicyMatcher matcher = new ExpiredPolicyMatcher();

        public void run()
        {
            ArgonManager am = LocalUvmContextFactory.context().argonManager();
            am.shutdownMatches(matcher);
        }
    }

    private static class ExpiredPolicyMatcher implements SessionMatcher
    {
        private final PipelineFoundryImpl foundry
            = PipelineFoundryImpl.foundry();

        public boolean isMatch(Policy policy, IPSessionDesc clientSide,
                               IPSessionDesc serverSide)
        {
            PolicyRule pr = foundry.selectPolicy(clientSide); // XXX?
            Policy sp = pr.getPolicy();

            /** If either policy is null, just check if they are equal */
            if (policy == null || sp == null ) {
                return sp !=  policy;   
            } else if (policy == sp) {
                return false;
            } else if (policy.equals(sp)) {
                return false;
            } else {
                return true;
            }
        }
    }
}
