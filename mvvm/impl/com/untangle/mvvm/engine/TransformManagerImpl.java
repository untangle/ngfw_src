/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

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

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.api.MvvmTransformHandler;
import com.untangle.mvvm.logging.MvvmLoggingContext;
import com.untangle.mvvm.logging.MvvmLoggingContextFactory;
import com.untangle.mvvm.logging.MvvmRepositorySelector;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.tran.DeployException;
import com.untangle.mvvm.tran.LocalTransformManager;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformDesc;
import com.untangle.mvvm.tran.TransformManager;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.tran.UndeployException;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class TransformManagerImpl implements LocalTransformManager, MvvmLoggingContextFactory
{
    private static final String DESC_PATH = "META-INF/mvvm-transform.xml";

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(getClass());

    private final TransformManagerState transformManagerState;
    private final Map<Tid, TransformContextImpl> tids
        = new ConcurrentHashMap<Tid, TransformContextImpl>();
    private final ThreadLocal<TransformContext> threadContexts
        = new InheritableThreadLocal<TransformContext>();
    private final MvvmRepositorySelector repositorySelector;

    private boolean live = true;

    TransformManagerImpl(MvvmRepositorySelector repositorySelector)
    {
        this.repositorySelector = repositorySelector;

        TransactionWork<TransformManagerState> tw = new TransactionWork<TransformManagerState>()
            {
                private TransformManagerState tms;

                public boolean doWork(Session s) throws SQLException
                {
                    Query q = s.createQuery("from TransformManagerState tms");
                    tms = (TransformManagerState)q.uniqueResult();
                    if (null == tms) {
                        tms = new TransformManagerState();
                        s.save(tms);
                    }
                    return true;
                }

                public TransformManagerState getResult() { return tms; }
            };
        MvvmContextFactory.context().runTransaction(tw);
        this.transformManagerState = tw.getResult();
    }

    // TransformManager -------------------------------------------------------

    public List<Tid> transformInstances()
    {
        List<Tid> l = new ArrayList<Tid>(tids.keySet());

        // only reports requires sorting
        // XXX the client should do its own sorting
        Collections.sort(l, new Comparator<Tid>() {
            public int compare(Tid t1, Tid t2) {
                TransformContextImpl tci1 = tids.get(t1);
                TransformContextImpl tci2 = tids.get(t2);
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

    public List<Tid> transformInstances(String mackageName)
    {
        List<Tid> l = new LinkedList<Tid>();

        for (Tid tid : tids.keySet()) {
            TransformContext tc = tids.get(tid);
            if (null != tc) {
                if (tc.getTransformDesc().getName().equals(mackageName)) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> transformInstances(String name, Policy policy)
    {
        List<Tid> l = new ArrayList<Tid>(tids.size());

        for (Tid tid : tids.keySet()) {
            TransformContext tc = tids.get(tid);
            if (null != tc) {
                String n = tc.getTransformDesc().getName();

                Policy p = tid.getPolicy();

                if (n.equals(name) &&
                    ((policy == null && p == null) || (policy != null && policy.equals(p)))) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> transformInstances(Policy policy)
    {
        List<Tid> l = new ArrayList<Tid>(tids.size());

        for (Tid tid : tids.keySet()) {
            TransformContext tc = tids.get(tid);

            if (null != tc) {
                Policy p = tid.getPolicy();

                if ((policy == null && p == null) || (policy != null && policy.equals(p))) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public List<Tid> transformInstancesVisible(Policy policy)
    {
    List<Tid> transformInstances = transformInstances(policy);
    Vector<Tid> visibleVector = new Vector<Tid>();
    for( Tid tid : transformInstances ){
        if( transformContext(tid).getMackageDesc().getViewPosition() >= 0 )
        visibleVector.add(tid);
    }
    return (List<Tid>) visibleVector;
    }

    public TransformContextImpl transformContext(Tid tid)
    {
        return tids.get(tid);
    }

    public Tid instantiate(String transformName)
        throws DeployException
    {
        Policy policy = getDefaultPolicyForTransform(transformName);
        return instantiate(transformName, newTid(null, transformName), new String[0]);
    }

    public Tid instantiate(String transformName, String[] args)
        throws DeployException
    {
        Policy policy = getDefaultPolicyForTransform(transformName);
        return instantiate(transformName, newTid(policy, transformName), args);
    }

    public Tid instantiate(String transformName, Policy policy)
        throws DeployException
    {
        return instantiate(transformName, newTid(policy, transformName),
                           new String[0]);
    }

    public Tid instantiate(String transformName, Policy policy, String[] args)
        throws DeployException
    {
        return instantiate(transformName, newTid(policy, transformName), args);
    }

    public void destroy(final Tid tid) throws UndeployException
    {
        final TransformContextImpl tc;

        synchronized (this) {
            tc = tids.get(tid);
            if (null == tc) {
                logger.error("Destroy Failed: " + tid + " not found");
                throw new UndeployException("Transform " + tid + " not found");
            }
            tc.destroy();

            tids.remove(tid);
        }

        tc.destroyPersistentState();
    }

    public Map<Tid, TransformStats> allTransformStats()
    {
        HashMap<Tid, TransformStats> result = new HashMap<Tid, TransformStats>();
        for (Iterator<Tid> iter = tids.keySet().iterator(); iter.hasNext();) {
            Tid tid = iter.next();
            TransformContextImpl tci = tids.get(tid);
            if (tci.getRunState() == TransformState.RUNNING)
                result.put(tid, tci.getStats());
        }
        return result;
    }

    // Manager lifetime -------------------------------------------------------

    void init()
    {
        restartUnloaded();
    }

    // destroy the transform manager
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
                logger.warn("transform instances not destroyed: " + tids.size());
            }
        }

        logger.info("TransformManager destroyed");
    }

    // LocalTransformManager methods ------------------------------------------

    public TransformContext threadContext()
    {
        return threadContexts.get();
    }

    public void registerThreadContext(TransformContext ctx)
    {
        threadContexts.set(ctx);
        repositorySelector.setContextFactory(this);
    }

    public void deregisterThreadContext()
    {
        threadContexts.remove();
        repositorySelector.mvvmContext();
    }

    // MvvmLoggingContextFactory methods --------------------------------------

    public MvvmLoggingContext get()
    {
        final TransformContext tctx = threadContexts.get();
        if (null == tctx) {
            LogLog.warn("null transform context in threadContexts");
        }

        return new MvvmLoggingContext()
            {
                public String getConfigName()
                {
                    return "log4j-tran.xml";
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

                public boolean equals(Object o)
                {
                    return tctx.equals(o);
                }

                public int hashCode()
                {
                    return tctx.hashCode();
                }
            };
    }

    // package protected methods ----------------------------------------------

    void unload(Tid tid)
    {
        synchronized (this) {
            TransformContextImpl tc = tids.get(tid);
            logger.info("Unloading: " + tid
                        + " (" + tc.getTransformDesc().getName() + ")");

            tc.unload();
            tids.remove(tid);
        }
    }

    void restart(String name)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();

        String availVer = tbm.mackageDesc(name).getInstalledVersion();

        synchronized (this) {
            List<Tid> mkgTids = transformInstances(name);
            if (0 < mkgTids.size()) {
                Tid t = mkgTids.get(0);
                TransformContext tc = tids.get(t);

                if (0 < tc.getTransformDesc().getExports().size()) {
                    // exported resources, must restart everything
                    for (Tid tid : tids.keySet()) {
                        TransformDesc td = tids.get(tid).getTransformDesc();
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
                        TransformDesc td = tids.get(tid).getTransformDesc();
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
            throw new RuntimeException("TransformManager is shut down");
        }

        logger.info("Restarting unloaded transforms...");


        List<TransformPersistentState> unloaded = getUnloaded();
        Map<Tid, TransformDesc> tDescs = loadTransformDescs(unloaded);
        Set<String> loadedParents = new HashSet<String>(unloaded.size());

        MvvmContextImpl mctx = MvvmContextImpl.getInstance();

        ToolboxManagerImpl tbm = (ToolboxManagerImpl)mctx.toolboxManager();

        while (0 < unloaded.size()) {
            List<TransformPersistentState> startQueue = getLoadable(unloaded,
                                                                    tDescs,
                                                                    loadedParents);
            if (0 == startQueue.size()) {
                logger.warn("could not restart all transforms");
                break;
            }

            startUnloaded(startQueue, tDescs, loadedParents);
        }

        long t1 = System.currentTimeMillis();
        logger.info("time to restart transforms: " + (t1 - t0));
    }

    private void startUnloaded(List<TransformPersistentState> startQueue,
                               Map<Tid, TransformDesc> tDescs,
                               Set<String> loadedParents)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();


        List<Thread> threads = new ArrayList<Thread>(startQueue.size());

        for (TransformPersistentState tps : startQueue) {
            final TransformDesc tDesc = tDescs.get(tps.getTid());
            final Tid tid = tps.getTid();
            final String name = tps.getName();
            loadedParents.add(name);
            final String[] args = tps.getArgArray();
            final MackageDesc mackageDesc = tbm.mackageDesc(name);

            Thread t = MvvmContextFactory.context().newThread(new Runnable()
                {
                    public void run()
                    {
                        logger.info("Restarting: " + tid + " (" + name + ")");
                        TransformContextImpl tc = null;
                        try {
                            tc = new TransformContextImpl((URLClassLoader)getClass().getClassLoader(), tDesc,
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
                        if (null != tc && null == tc.transform()) {
                            tids.remove(tid);
                        }
                    }
                });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException exn) {
                break; // XXX give up
            }
        }
    }

    private List<TransformPersistentState> getLoadable(List<TransformPersistentState> unloaded,
                                                       Map<Tid, TransformDesc> tDescs,
                                                       Set<String> loadedParents)
    {
        List<TransformPersistentState> l = new ArrayList<TransformPersistentState>(unloaded.size());
        Set<String> thisPass = new HashSet<String>(unloaded.size());

        for (Iterator<TransformPersistentState> i = unloaded.iterator(); i.hasNext(); ) {
            TransformPersistentState tps = i.next();
            Tid tid = tps.getTid();
            TransformDesc tDesc = tDescs.get(tid);
            if (null == tDesc) {
                logger.warn("no TransformDesc for: " + tid);
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
            // transform not loading this pass or already loaded in
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

    private Map<Tid, TransformDesc> loadTransformDescs(List<TransformPersistentState> unloaded)
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();

        Map<Tid, TransformDesc> tDescs = new HashMap<Tid, TransformDesc>(unloaded.size());

        for (TransformPersistentState tps : unloaded) {
            String name = tps.getName();
            URL[] urls = new URL[] { tbm.getResourceDir(name) };
            Tid tid = tps.getTid();
            tid.setTransformName(name);
            MackageDesc md = tbm.mackageDesc(name);

            try {
                logger.info("initializing transform desc for: " + name);
                TransformDesc tDesc = initTransformDesc(md, urls, tid);
                tDescs.put(tid, tDesc);
            } catch (DeployException exn) {
                logger.warn("TransformDesc could not be parsed", exn);
            }
        }

        return tDescs;
    }

    private List<TransformPersistentState> getUnloaded()
    {
        final List<TransformPersistentState> unloaded
            = new LinkedList<TransformPersistentState>();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from TransformPersistentState tps");
                    List<TransformPersistentState> result = q.list();

                    for (TransformPersistentState persistentState : result) {
                        if (!tids.containsKey(persistentState.getTid())) {
                            unloaded.add(persistentState);
                        }
                    }
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return unloaded;
    }

    private Tid instantiate(String transformName, Tid tid, String[] args)
        throws DeployException
    {
        MvvmContextImpl mctx = MvvmContextImpl.getInstance();

        ToolboxManagerImpl tbm = (ToolboxManagerImpl)mctx.toolboxManager();

        URL[] resUrls = new URL[] { tbm.getResourceDir(transformName) };

        MackageDesc mackageDesc = tbm.mackageDesc(transformName);
        if ((mackageDesc.isService() || mackageDesc.isUtil() || mackageDesc.isCore())
            && tid.getPolicy() != null) {
            throw new DeployException("Cannot specify a policy for a service/util/core: "
                                      + transformName);
        }

        if (mackageDesc.isSecurity() && tid.getPolicy() == null) {
            throw new DeployException("Cannot have null policy for a security: "
                                      + transformName);
        }

        logger.info("initializing transform desc for: " + transformName);
        TransformDesc tDesc = initTransformDesc(mackageDesc, resUrls, tid);

        synchronized (this) {
            if (!live) {
                throw new DeployException("TransformManager is shut down");
            }

            if (null != tDesc.getTransformBase()) {
                SchemaUtil.initSchema("settings", tDesc.getTransformBase());
            }
            SchemaUtil.initSchema("settings", tDesc.getName());
            TransformContextImpl tc = new TransformContextImpl
                ((URLClassLoader)getClass().getClassLoader(), tDesc, mackageDesc.getName(), true);
            tids.put(tid, tc);
            try {
                tc.init(args);
            } finally {
                if (null == tc.transform()) {
                    tids.remove(tid);
                }
            }
        }

        return tid;
    }

    /**
     * Initialize transform from 'META-INF/mvvm-transform.xml' in one
     * of the urls.
     *
     * @param urls urls to find transform descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private TransformDesc initTransformDesc(MackageDesc mackageDesc,
                                            URL[] urls, Tid tid)
        throws DeployException
    {
        // XXX assumes no parent cl has this file.
        InputStream is = new URLClassLoader(urls)
            .getResourceAsStream(DESC_PATH);
        if (null == is) {
            throw new DeployException(DESC_PATH + " not found");
        }

        MvvmTransformHandler mth = new MvvmTransformHandler(mackageDesc);

        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(mth);
            xr.parse(new InputSource(is));
        } catch (SAXException exn) {
            throw new DeployException(exn);
        } catch (IOException exn) {
            throw new DeployException(exn);
        }

        TransformDesc transformDesc = mth.getTransformDesc(tid);;

        return transformDesc;
    }

    private Policy getDefaultPolicyForTransform(String transformName)
        throws DeployException
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();
        MackageDesc mackageDesc = tbm.mackageDesc(transformName);
        if (mackageDesc == null)
            throw new DeployException("Transform named " + transformName + " not found");
        if (!mackageDesc.isSecurity())
            return null;
        else
            return MvvmContextFactory.context().policyManager().getDefaultPolicy();
    }

    private Tid newTid(Policy policy, String transformName)
    {
        final Tid tid;
        synchronized (transformManagerState) {
            tid = transformManagerState.nextTid(policy, transformName);
        }

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(transformManagerState);
                    s.save(tid);
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return tid;
    }
}
