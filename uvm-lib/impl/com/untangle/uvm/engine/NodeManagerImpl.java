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
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.UvmLoggingContext;
import com.untangle.uvm.logging.UvmLoggingContextFactory;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStats;
import com.untangle.uvm.node.UndeployException;
import com.untangle.uvm.node.UvmNodeHandler;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.MackageInstallException;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Implements LocalNodeManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class NodeManagerImpl implements LocalNodeManager, UvmLoggingContextFactory
{
    private static final String DESC_PATH = "META-INF/uvm-node.xml";

    private final Logger logger = Logger.getLogger(getClass());

    private final NodeManagerState nodeManagerState;
    private final Map<Tid, NodeContextImpl> tids
        = new ConcurrentHashMap<Tid, NodeContextImpl>();
    private final ThreadLocal<NodeContext> threadContexts
        = new InheritableThreadLocal<NodeContext>();
    private final UvmRepositorySelector repositorySelector;

    private boolean live = true;

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

    // RemoteNodeManager ------------------------------------------------------

    public List<Tid> nodeInstances()
    {
        List<Tid> l = new ArrayList<Tid>(tids.keySet());

        // only reports requires sorting
        // XXX the client should do its own sorting
        Collections.sort(l, new Comparator<Tid>() {
            public int compare(Tid t1, Tid t2) {
                NodeContextImpl tci1 = tids.get(t1);
                NodeContextImpl tci2 = tids.get(t2);
                int rpi1 = tci1.getMackageDesc().getViewPosition();
                int rpi2 = tci2.getMackageDesc().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getMackageDesc().getName().compareToIgnoreCase(tci2.getMackageDesc().getName());
                } else if (rpi1 < rpi2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return l;
    }

    public List<Tid> nodeInstances(String mackageName)
    {
        List<Tid> l = new LinkedList<Tid>();

        for (Tid tid : tids.keySet()) {
            NodeContext tc = tids.get(tid);
            if (null != tc) {
                if (tc.getNodeDesc().getName().equals(mackageName)) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> nodeInstances(String name, Policy policy)
    {
        List<Tid> l = new ArrayList<Tid>(tids.size());

        for (Tid tid : tids.keySet()) {
            NodeContext tc = tids.get(tid);
            if (null != tc) {
                String n = tc.getNodeDesc().getName();

                Policy p = tid.getPolicy();

                if (n.equals(name) &&
                    ((policy == null && p == null) || (policy != null && policy.equals(p)))) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> nodeInstances(Policy policy)
    {
        List<Tid> l = new ArrayList<Tid>(tids.size());

        for (Tid tid : tids.keySet()) {
            NodeContext tc = tids.get(tid);

            if (null != tc) {
                Policy p = tid.getPolicy();

                if ((policy == null && p == null) || (policy != null && policy.equals(p))) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> nodeInstancesVisible(Policy policy)
    {
        List<Tid> nodeInstances = nodeInstances(policy);
        Vector<Tid> visibleVector = new Vector<Tid>();
        for( Tid tid : nodeInstances ){
            if( nodeContext(tid).getMackageDesc().getViewPosition() >= 0 )
                visibleVector.add(tid);
        }
        return (List<Tid>) visibleVector;
    }

    public NodeContextImpl nodeContext(Tid tid)
    {
        return tids.get(tid);
    }

    public Tid instantiate(String nodeName)
        throws DeployException
    {
        Policy policy = getDefaultPolicyForNode(nodeName);
        return instantiate(nodeName, newTid(null, nodeName), new String[0]);
    }

    public Tid instantiate(String nodeName, String[] args)
        throws DeployException
    {
        Policy policy = getDefaultPolicyForNode(nodeName);
        return instantiate(nodeName, newTid(policy, nodeName), args);
    }

    public Tid instantiate(String nodeName, Policy policy)
        throws DeployException
    {
        return instantiate(nodeName, newTid(policy, nodeName),
                           new String[0]);
    }

    public Tid instantiate(String nodeName, Policy policy, String[] args)
        throws DeployException
    {
        return instantiate(nodeName, newTid(policy, nodeName), args);
    }

    public void destroy(final Tid tid) throws UndeployException
    {
        final NodeContextImpl tc;

        synchronized (this) {
            tc = tids.get(tid);
            if (null == tc) {
                logger.error("Destroy Failed: " + tid + " not found");
                throw new UndeployException("Node " + tid + " not found");
            }
            tc.destroy();

            tids.remove(tid);
        }

        tc.destroyPersistentState();
    }

    public Map<Tid, NodeStats> allNodeStats()
    {
        HashMap<Tid, NodeStats> result = new HashMap<Tid, NodeStats>();
        for (Iterator<Tid> iter = tids.keySet().iterator(); iter.hasNext();) {
            Tid tid = iter.next();
            NodeContextImpl tci = tids.get(tid);
            if (tci.getRunState() == NodeState.RUNNING)
                result.put(tid, tci.getStats());
        }
        return result;
    }

    // Manager lifetime -------------------------------------------------------

    void init()
    {
        restartUnloaded();
    }

    // destroy the node manager
    void destroy()
    {
        synchronized (this) {
            live = false;

            Set s = new HashSet(tids.keySet());

            for (Iterator i = s.iterator(); i.hasNext(); ) {
                Tid tid = (Tid)i.next();
                if (null != tid) {
                    unload(tid);
                }
            }

            if (tids.size() > 0) {
                logger.warn("node instances not destroyed: " + tids.size());
            }
        }

        logger.info("NodeManager destroyed");
    }

    // LocalNodeManager methods -----------------------------------------------

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
        final NodeContext tctx = threadContexts.get();
        if (null == tctx) {
            LogLog.warn("null node context in threadContexts");
        }

        return new NodeManagerLoggingContext(tctx);
    }

    // package protected methods ----------------------------------------------

    void unload(Tid tid)
    {
        synchronized (this) {
            NodeContextImpl tc = tids.get(tid);
            logger.info("Unloading: " + tid
                        + " (" + tc.getNodeDesc().getName() + ")");

            tc.unload();
            tids.remove(tid);
        }
    }

    void restart(String name)
    {
        RemoteToolboxManager tbm = LocalUvmContextFactory
            .context().toolboxManager();

        String availVer = tbm.mackageDesc(name).getInstalledVersion();

        synchronized (this) {
            List<Tid> mkgTids = nodeInstances(name);
            if (0 < mkgTids.size()) {
                Tid t = mkgTids.get(0);
                NodeContext tc = tids.get(t);

                if (0 < tc.getNodeDesc().getExports().size()) {
                    // exported resources, must restart everything
                    for (Tid tid : tids.keySet()) {
                        NodeDesc td = tids.get(tid).getNodeDesc();
                        MackageDesc md = tids.get(tid).getMackageDesc();
                        if (!md.getInstalledVersion().equals(availVer)) {
                            logger.info("new version available: " + name);
                            unload(tid);
                        } else {
                            logger.info("have latest version: " + name);
                        }
                    }
                } else {
                    for (Tid tid : mkgTids) {
                        NodeDesc td = tids.get(tid).getNodeDesc();
                        MackageDesc md = tids.get(tid).getMackageDesc();
                        if (!md.getInstalledVersion().equals(availVer)) {
                            logger.info("new version available: " + name);
                            unload(tid);
                        } else {
                            logger.info("have latest version: " + name);
                        }
                    }
                }
                restartUnloaded();
            }
        }
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
        Map<Tid, NodeDesc> tDescs = loadNodeDescs(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        UvmContextImpl mctx = UvmContextImpl.getInstance();

        RemoteToolboxManager tbm = mctx.toolboxManager();

        while (0 < unloaded.size()) {
            List<NodePersistentState> startQueue = getLoadable(unloaded,
                                                               tDescs,
                                                               loadedParents);
            if (0 == startQueue.size()) {
                logger.info("not all parents loaded, proceeding");
                startUnloaded(unloaded, tDescs, loadedParents);
                break;
            }

            startUnloaded(startQueue, tDescs, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("time to restart nodes: " + (t1 - t0));

        try {
            ensureRouterStarted();
        } catch (MackageInstallException exn) {
            logger.warn("could not install router", exn);
        } catch (DeployException exn) {
            logger.warn("could not instantiate router", exn);
        } catch (NodeStartException exn) {
            logger.warn("could not start router", exn);
        }
    }

    private void ensureRouterStarted()
        throws MackageInstallException, DeployException, NodeStartException
    {
        Tid t = null;

        List<Tid> l = nodeInstances("untangle-node-router");

        if (0 == l.size()) {
            RemoteToolboxManager tbm = LocalUvmContextFactory
                .context().toolboxManager();
            if (!tbm.isInstalled("untangle-node-router")) {
                tbm.installSynchronously("untangle-node-router");
            }
            t = instantiate("untangle-node-router");
        } else {
            t = l.get(0);
        }

        NodeContext nc = nodeContext(t);
        nc.node().start();
    }

    private static int startThreadNum = 0;

    private void startUnloaded(List<NodePersistentState> startQueue,
                               Map<Tid, NodeDesc> tDescs,
                               Set<String> loadedParents)
    {
        RemoteToolboxManager tbm = LocalUvmContextFactory
            .context().toolboxManager();


        List<Runnable> restarters = new ArrayList<Runnable>(startQueue.size());

        for (NodePersistentState tps : startQueue) {
            final NodeDesc tDesc = tDescs.get(tps.getTid());
            final Tid tid = tps.getTid();
            final String name = tps.getName();
            loadedParents.add(name);
            final String[] args = tps.getArgArray();
            final MackageDesc mackageDesc = tbm.mackageDesc(name);

            Runnable r = new Runnable()
                {
                    public void run()
                    {
                        logger.info("Restarting: " + tid + " (" + name + ")");
                        NodeContextImpl tc = null;
                        try {
                            tc = new NodeContextImpl((URLClassLoader)getClass().getClassLoader(), tDesc,
                                                     mackageDesc.getName(),
                                                     false);
                            tids.put(tid, tc);
                            tc.init(args);
                            logger.info("Restarted: " + tid);
                        } catch (Exception exn) {
                            logger.error("Could not restart: " + tid, exn);
                        } catch (LinkageError err) {
                            logger.error("Could not restart: " + tid, err);
                        }
                        if (null != tc && null == tc.node()) {
                            tids.remove(tid);
                        }
                    }
                };
            restarters.add(r);
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

    private List<NodePersistentState> getLoadable(List<NodePersistentState> unloaded,
                                                  Map<Tid, NodeDesc> tDescs,
                                                  Set<String> loadedParents)
    {
        List<NodePersistentState> l = new ArrayList<NodePersistentState>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<NodePersistentState> i = unloaded.iterator(); i.hasNext(); ) {
            NodePersistentState tps = i.next();
            Tid tid = tps.getTid();
            NodeDesc tDesc = tDescs.get(tid);
            if (null == tDesc) {
                logger.warn("no NodeDesc for: " + tid);
                continue;
            }

            List<String> parents = tDesc.getParents();

            boolean parentsLoaded = true;
            for (String parent : parents) {
                if (!loadedParents.contains(parent)) {
                    parentsLoaded = false;
                }
                if (false == parentsLoaded) { break; }
            }

            String name = tDesc.getName();

            // all parents loaded and another instance of this
            // node not loading this pass or already loaded in
            // previous pass (prevents classloader race).
            if (parentsLoaded
                && (!thisPass.contains(name) || loadedParents.contains(name))) {
                i.remove();
                l.add(tps);
                thisPass.add(name);
            }
        }

        return l;
    }

    private Map<Tid, NodeDesc> loadNodeDescs(List<NodePersistentState> unloaded)
    {
        RemoteToolboxManagerImpl tbm = (RemoteToolboxManagerImpl)LocalUvmContextFactory
            .context().toolboxManager();

        Map<Tid, NodeDesc> tDescs = new HashMap<Tid, NodeDesc>(unloaded.size());

        for (NodePersistentState tps : unloaded) {
            String name = tps.getName();
            logger.info("Getting mackage desc for: " + name);
            MackageDesc md = tbm.mackageDesc(name);
            if (null == md) {
                logger.warn("could not get mackage desc for: " + name);
                continue;
            }

            URL[] urls = new URL[] { tbm.getResourceDir(md) };
            Tid tid = tps.getTid();
            tid.setNodeName(name);

            try {
                logger.info("initializing node desc for: " + name);
                NodeDesc tDesc = initNodeDesc(md, urls, tid);
                tDescs.put(tid, tDesc);
            } catch (DeployException exn) {
                logger.warn("NodeDesc could not be parsed", exn);
            }
        }

        return tDescs;
    }

    private List<NodePersistentState> getUnloaded()
    {
        final List<NodePersistentState> unloaded
            = new LinkedList<NodePersistentState>();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from NodePersistentState tps");
                    List<NodePersistentState> result = q.list();

                    for (NodePersistentState persistentState : result) {
                        if (!tids.containsKey(persistentState.getTid())) {
                            unloaded.add(persistentState);
                        }
                    }
                    return true;
                }

                public Object getResult() { return null; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        return unloaded;
    }

    private Tid instantiate(String nodeName, Tid tid, String[] args)
        throws DeployException
    {
        UvmContextImpl mctx = UvmContextImpl.getInstance();

        RemoteToolboxManagerImpl tbm = (RemoteToolboxManagerImpl)mctx.toolboxManager();

        MackageDesc mackageDesc = tbm.mackageDesc(nodeName);
        URL[] resUrls = new URL[] { tbm.getResourceDir(mackageDesc) };

        if ((mackageDesc.isService() || mackageDesc.isUtil() || mackageDesc.isCore())
            && tid.getPolicy() != null) {
            throw new DeployException("Cannot specify a policy for a service/util/core: "
                                      + nodeName);
        }

        if (mackageDesc.isSecurity() && tid.getPolicy() == null) {
            throw new DeployException("Cannot have null policy for a security: "
                                      + nodeName);
        }

        logger.info("initializing node desc for: " + nodeName);
        NodeDesc tDesc = initNodeDesc(mackageDesc, resUrls, tid);

        synchronized (this) {
            if (!live) {
                throw new DeployException("NodeManager is shut down");
            }

            NodeContextImpl tc = new NodeContextImpl
                ((URLClassLoader)getClass().getClassLoader(), tDesc, mackageDesc.getName(), true);
            tids.put(tid, tc);
            try {
                tc.init(args);
            } finally {
                if (null == tc.node()) {
                    tids.remove(tid);
                }
            }
        }

        return tid;
    }

    /**
     * Initialize node from 'META-INF/uvm-node.xml' in one
     * of the urls.
     *
     * @param urls urls to find node descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private NodeDesc initNodeDesc(MackageDesc mackageDesc,
                                  URL[] urls, Tid tid)
        throws DeployException
    {
        // XXX assumes no parent cl has this file.
        InputStream is = new URLClassLoader(urls)
            .getResourceAsStream(DESC_PATH);
        if (null == is) {
            throw new DeployException(DESC_PATH + " not found");
        }

        UvmNodeHandler mth = new UvmNodeHandler(mackageDesc);

        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(mth);
            xr.parse(new InputSource(is));
        } catch (SAXException exn) {
            throw new DeployException(exn);
        } catch (IOException exn) {
            throw new DeployException(exn);
        }

        NodeDesc nodeDesc = mth.getNodeDesc(tid);;

        return nodeDesc;
    }

    private Policy getDefaultPolicyForNode(String nodeName)
        throws DeployException
    {
        RemoteToolboxManager tbm = LocalUvmContextFactory
            .context().toolboxManager();
        MackageDesc mackageDesc = tbm.mackageDesc(nodeName);
        if (mackageDesc == null)
            throw new DeployException("Node named " + nodeName + " not found");
        if (!mackageDesc.isSecurity())
            return null;
        else
            return LocalUvmContextFactory.context().policyManager().getDefaultPolicy();
    }

    private Tid newTid(Policy policy, String nodeName)
    {
        final Tid tid;
        synchronized (nodeManagerState) {
            tid = nodeManagerState.nextTid(policy, nodeName);
        }

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(nodeManagerState);
                    s.save(tid);
                    return true;
                }

                public Object getResult() { return null; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        return tid;
    }

    // private static classes -------------------------------------------------

    private static class NodeManagerLoggingContext
        implements UvmLoggingContext
    {
        private final NodeContext tctx;

        // constructors -------------------------------------------------------

        NodeManagerLoggingContext(NodeContext tctx)
        {
            this.tctx = tctx;
        }

        // UvmLoggingContext methods -----------------------------------------

        public String getConfigName()
        {
            return "log4j-node.xml";
        }

        public String getFileName()
        {
            if (null == tctx) {
                return "0";
            } else {
                return tctx.getTid().getName();
            }
        }

        public String getName()
        {
            if (null == tctx) {
                return "0";
            } else {
                return tctx.getTid().getName();
            }
        }

        // Object methods -----------------------------------------------------

        public boolean equals(Object o)
        {
            if (o instanceof NodeManagerLoggingContext) {
                NodeManagerLoggingContext tmc
                    = (NodeManagerLoggingContext)o;
                return tctx.equals(tmc.tctx);
            } else {
                return false;
            }
        }

        public int hashCode()
        {
            return tctx.hashCode();
        }
    }
}
