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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
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
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.DeployException;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.UndeployException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class TransformManagerImpl implements TransformManager
{
    private static final String DESC_PATH = "META-INF/mvvm-transform.xml";
    private static final Tid[] TID_PROTO = new Tid[0];

    private static final Object LOCK = new Object();

    private static final Logger logger = Logger
        .getLogger(TransformManagerImpl.class);

    private static TransformManagerImpl TRANSFORM_MANAGER;

    private final Map<Tid, TransformContextImpl> tids
        = new ConcurrentHashMap<Tid, TransformContextImpl>();

    // XXX create new cl on reload all
    private final CasingClassLoader casingClassLoader
        = new CasingClassLoader(getClass().getClassLoader());

    private TransformManagerImpl() { }

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

    public Tid[] transformInstances()
    {
        Tid[] tidArray = tids.keySet().toArray(TID_PROTO);

        try {
            // Sort the returned list by rack position.  This allows for nice fixed report ordering, etc.
            Arrays.sort(tidArray, new Comparator<Tid>() {
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
        } catch (Exception x) {
            logger.error("Unexpected expection: " + x);
        }

        return tidArray;
    }

    public Tid[] transformInstances(String mackageName)
    {
        List<Tid> l = new LinkedList<Tid>();

        for (Iterator i = tids.keySet().iterator(); i.hasNext(); ) {
            Tid tid = (Tid)i.next();
            TransformContext tc = tids.get(tid);
            if (tc.getTransformDesc().getName().equals(mackageName)) {
                l.add(tid);
            }
        }

        return l.toArray(TID_PROTO);
    }

    public TransformContext transformContext(Tid tid)
    {
        return tids.get(tid);
    }

    public Tid instantiate(String transformName, String[] args)
        throws DeployException
    {
        return instantiate(transformName, newTid(), args);
    }

    public Tid instantiate(String transformName)
        throws DeployException
    {
        return instantiate(transformName, newTid(), new String[0]);
    }

    public void destroy(Tid tid) throws UndeployException
    {
        TransformContextImpl tc = tids.get(tid);
        tc.destroy();
        tids.remove(tid);

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            synchronized (tidLock) {
                s.delete(tc.getPersistentState());
                s.delete(tc.getTransformPreferences());
            }

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

        logger.info("TransformManager destroyed");
    }

    // package protected methods ----------------------------------------------

    CasingClassLoader getCasingClassLoader()
    {
        return casingClassLoader;
    }

    void unload(Tid tid)
    {
        TransformContextImpl tc = tids.get(tid);
        logger.info("Unloading: " + tid
                    + " (" + tc.getTransformDesc().getName() + ")");

        tc.unload();
        tids.remove(tid);
    }

    void restartUnloaded()
    {
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

            try {
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

                Set<String> parents = tDesc.getParents();
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
                    try {
                        TransformContextImpl tc = new TransformContextImpl
                            (urls, tDesc, args, mackageDesc, false);
                        tids.put(tid, tc);

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

        URL[] urls = tbm.resources(transformName);
        MackageDesc mackageDesc = tbm.mackageDesc(transformName);
        TransformDesc tDesc = initTransformDesc(urls, tid);

        synchronized (this) {
            TransformContextImpl tc = new TransformContextImpl
                (urls, tDesc, args, mackageDesc, true);

            tids.put(tid, tc);
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

    private Object tidLock = new Object();

    private Tid newTid()
    {
        Tid tid = null;

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            synchronized (tidLock) {
                Query q = s.createQuery("from TransformManagerState tms");
                TransformManagerState tms = (TransformManagerState)q
                    .uniqueResult();
                if (null == tms) {
                    tms = new TransformManagerState();
                    s.save(tms);
                }

                tid = tms.nextTid();
            }

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
