/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.LogMailer;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.DeployException;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.UndeployException;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class TransformManagerImpl implements TransformManager
{
    private static final String DESC_PATH = "META-INF/mvvm-transform.xml";

    private static final Object LOCK = new Object();

    private static final Logger logger = Logger
        .getLogger(TransformManagerImpl.class);

    private static TransformManagerImpl TRANSFORM_MANAGER;

    private final TransformManagerState transformManagerState;
    private final Map<Tid, TransformContextImpl> tids
        = new ConcurrentHashMap<Tid, TransformContextImpl>();
    private final ThreadLocal<TransformContext> threadContexts
        = new InheritableThreadLocal<TransformContext>();

    // XXX create new cl on reload all
    private final CasingClassLoader casingClassLoader
        = new CasingClassLoader(getClass().getClassLoader());

    private boolean live = true;

    private TransformManagerImpl()
    {
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from TransformManagerState tms");
            TransformManagerState tms = (TransformManagerState)q.uniqueResult();
            if (null == tms) {
                tms = new TransformManagerState();
                s.save(tms);
            }
            transformManagerState = tms;

            tx.commit();
        } catch (HibernateException exn) {
            throw new RuntimeException("couldn't start TransformManager", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }
    }

    static TransformManagerImpl manager()
    {
        synchronized (LOCK) {
            if (null == TRANSFORM_MANAGER) {
                TRANSFORM_MANAGER = new TransformManagerImpl();
            }
        }
        return TRANSFORM_MANAGER;
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
                int rpi1 = tci1.getMackageDesc().getRackPosition();
                int rpi2 = tci2.getMackageDesc().getRackPosition();
                if (rpi1 == rpi2)
                    return 0;
                else if (rpi1 < rpi2)
                    return -1;
                else
                    return 1;
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
                String n = tc.getTransformDesc().getName();

                Policy p = tid.getPolicy();

                if ((policy == null && p == null) || (policy != null && policy.equals(p))) {
                    l.add(tid);
                }
            }
        }

        return l;
    }

    public TransformContext transformContext(Tid tid)
    {
        return tids.get(tid);
    }

    public TransformContext threadContext()
    {
        return threadContexts.get();
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
        return instantiate(transformName, newTid(null, transformName), args);
    }

    public Tid instantiate(String transformName, Policy policy)
        throws DeployException
    {
        return instantiate(transformName, newTid(policy, transformName), new String[0]);
    }

    public Tid instantiate(String transformName, Policy policy, String[] args)
        throws DeployException
    {
        return instantiate(transformName, newTid(policy, transformName), args);
    }

    public void destroy(Tid tid) throws UndeployException
    {
        TransformContextImpl tc;

        synchronized (this) {
            tc = tids.get(tid);
            if (null == tc) {
                logger.error("Destroy Failed: " + tid + " not found");
                throw new UndeployException("Transform " + tid + " not found");
            }
            tc.destroy();
            tids.remove(tid);
        }

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.delete(tc.getPersistentState());
            s.delete(tc.getTransformPreferences());

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get TransformManagerState", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        // Free up our logger.  This kind of stuff should be in a hook. XXX
        LogMailer lm = LogMailer.get();
        lm.unregister(tid);
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

    // package protected methods ----------------------------------------------

    void registerThreadContext(TransformContext ctx)
    {
        threadContexts.set(ctx);
    }

    void deregisterThreadContext()
    {
        threadContexts.remove();
    }

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

    void restart(String mackageName)
    {
        synchronized (this) {
            List<Tid> mkgTids = transformInstances(mackageName);
            if (0 < mkgTids.size()) {
                Tid t = mkgTids.get(0);
                TransformContext tc = tids.get(t);
                if (0 < tc.getTransformDesc().getExports().size()) {
                    // exported resources, must restart everything
                    for (Tid tid : tids.keySet()) {
                        unload(tid);
                    }
                } else {
                    for (Tid tid : mkgTids) {
                        unload(tid);
                    }
                }
                restartUnloaded();
            }
        }
    }

    synchronized void restartUnloaded()
    {
        if (!live) {
            throw new RuntimeException("TransformManager is shut down");
        }

        logger.info("Restarting unloaded transforms...");

        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();

        List<TransformPersistentState> unloaded = getUnloaded();
        Map<Tid, TransformDesc> tDescs = new HashMap<Tid, TransformDesc>();

        for (Iterator<TransformPersistentState> i = unloaded.iterator();
             i.hasNext(); ) {
            TransformPersistentState tps = i.next();
            URL[] urls = tbm.resources(tps.getName());
            Tid tid = tps.getTid();
            tid.setTransformName(tps.getName());

            try {
                logger.info("initializing transform desc for: " + tps.getName());
                TransformDesc tDesc = initTransformDesc(urls, tid);
                tDescs.put(tid, tDesc);
            } catch (DeployException exn) {
                logger.warn("TransformDesc could not be parsed", exn);
                i.remove();
            }
        }

        boolean removed = true;
        while (0 < unloaded.size()) {
            if (removed) {
                removed = false;
            } else {
                logger.warn("Did not start all transforms.");
                break;
            }

            for (Iterator<TransformPersistentState> i = unloaded.iterator();
                 i.hasNext(); ) {
                TransformPersistentState tps = i.next();
                Tid tid = tps.getTid();

                String name = tps.getName();
                URL[] urls = tbm.resources(name);

                TransformDesc tDesc = tDescs.get(tid);

                List<String> parents = tDesc.getParents();
                boolean parentsLoaded = true;
                for (String parent : parents) {
                    for (TransformPersistentState utps : unloaded) {
                        if (parent.equals(utps.getName())) {
                            parentsLoaded = false;
                        }
                    }
                    if (false == parentsLoaded) { break; }
                }

                if (parentsLoaded) {
                    removed = true;
                    i.remove();

                    MackageDesc mackageDesc = tbm.mackageDesc(name);
                    String[] args = tps.getArgArray();
                    logger.info("Restarting: " + tid + " (" + name + ")");

                    URLClassLoader cl = getClassLoader(tDesc, urls);

                    try {
                        TransformContextImpl tc = new TransformContextImpl
                            (cl, tDesc, mackageDesc, false);
                        tids.put(tid, tc);
                        tc.init(args);
                        logger.info("Restarted: " + tid);
                    } catch (Exception exn) {
                        logger.warn("Could not restart: " + tid, exn);
                    } catch (LinkageError err) {
                        logger.warn("Could not restart: " + tid, err);
                    }
                }
            }
        }
    }

    // private classes --------------------------------------------------------

    private static class UrlComparator implements Comparator<URL>
    {
        public static final UrlComparator COMPARATOR = new UrlComparator();

        private UrlComparator() { }

        public int compare(URL o1, URL o2)
        {
            return o1.toString().compareTo(o2.toString());
        }
    }

    // private methods --------------------------------------------------------

    private List<TransformPersistentState> getUnloaded()
    {
        List<TransformPersistentState> unloaded
            = new LinkedList<TransformPersistentState>();

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from TransformPersistentState tps");
            List<TransformPersistentState> result = q.list();

            for (TransformPersistentState persistentState : result) {
                if (!tids.containsKey(persistentState.getTid())) {
                    unloaded.add(persistentState);
                }
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get TransformDesc", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        return unloaded;
    }

    private Tid instantiate(String transformName, Tid tid, String[] args)
        throws DeployException
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();

        URL[] resUrls = tbm.resources(transformName);

        MackageDesc mackageDesc = tbm.mackageDesc(transformName);
        if (mackageDesc.isService() && tid.getPolicy() != null)
            throw new DeployException("Cannot specify a policy for a service: " + transformName);
        if (!mackageDesc.isService() && tid.getPolicy() == null)
            throw new DeployException("Cannot have null policy for a non-service: " + transformName);

        logger.info("initializing transform desc for: " + transformName);
        TransformDesc tDesc = initTransformDesc(resUrls, tid);

        synchronized (this) {
            if (!live) {
                throw new DeployException("TransformManager is shut down");
            }

            URLClassLoader cl = getClassLoader(tDesc, resUrls);

            TransformContextImpl tc = new TransformContextImpl
                (cl, tDesc, mackageDesc, true);
            tids.put(tid, tc);
            tc.init(args);
        }

        return tid;
    }

    private URLClassLoader getClassLoader(TransformDesc tDesc, URL[] resUrls)
    {
        String name = tDesc.getName();

        Arrays.sort(resUrls, UrlComparator.COMPARATOR);

        for (TransformContextImpl tc : tids.values()) {
            if (name.equals(tc.getTransformDesc().getName())) {
                URLClassLoader cl = tc.getClassLoader();
                URL[] clUrls = cl.getURLs();
                Arrays.sort(clUrls, UrlComparator.COMPARATOR);
                if (Arrays.equals(resUrls, clUrls)) {
                    logger.debug(name + " reusing classLoader: " + cl);
                    return cl;
                } else {
                    logger.warn("transform: " + name
                                + " with different resources");
                }
            }
        }

        logger.debug("creating new ClassLoader for: " + name);
        for (String export : tDesc.getExports()) {
            try {
                logger.debug("exporting: " + export);
                URL url = new URL(ToolboxManagerImpl.TOOLBOX_URL, export);
                casingClassLoader.addResource(url);
            } catch (MalformedURLException exn) {
                logger.warn("could not add resource: " + export);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("CasingClassLoader urls");
            for (URL url : casingClassLoader.getURLs()) {
                logger.debug("  " + url);
            }

            logger.debug("new URLClassLoader:");
            for (URL url : resUrls) {
                logger.debug("  " + url);
            }
        }

        if (null != tDesc.getTransformBase()) {
            SchemaUtil.initSchema(tDesc.getTransformBase());
        }
        SchemaUtil.initSchema(name);

        return new URLClassLoader(resUrls, casingClassLoader);
    }

    /**
     * Initialize transform from 'META-INF/mvvm-transform.xml' in one
     * of the urls.
     *
     * @param urls urls to find transform descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private TransformDesc initTransformDesc(URL[] urls, Tid tid)
        throws DeployException
    {
        // XXX assumes no parent cl has this file.
        InputStream is = new URLClassLoader(urls)
            .getResourceAsStream(DESC_PATH);
        if (null == is) {
            throw new DeployException(DESC_PATH + " not found");
        }

        MvvmTransformHandler mth = new MvvmTransformHandler();

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
        if (mackageDesc.isService())
            return null;
        else
            return MvvmContextFactory.context().policyManager().getDefaultPolicy();
    }

    private Tid newTid(Policy policy, String transformName)
    {
        Tid tid;
        synchronized (transformManagerState) {
            tid = transformManagerState.nextTid(policy, transformName);
        }

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(transformManagerState);
            s.save(tid);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get TransformManagerState", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        return tid;
    }
}
