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

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.DeployException;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.UndeployException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

class TransformManagerImpl implements TransformManager
{
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

        Arrays.sort(tidArray);

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
        return instantiate(transformName, newTid(), args, true);
    }

    public Tid instantiate(String transformName)
        throws DeployException
    {
        return instantiate(transformName, newTid(), new String[0], true);
    }

    public void destroy(Tid tid) throws UndeployException
    {
        TransformContextImpl tc = tids.get(tid);
        tc.destroy();
        tids.remove(tid);
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

        for (TransformPersistentState tps : getUnloaded()) {
            Tid tid = tps.getTid();
            logger.info("Restarting: " + tid + " (" + tps.getName() + ")");
            try {
                instantiate(tps.getName(), tid, null, false);
                logger.info("Restarted: " + tid);
            } catch (Exception exn) {
                logger.warn("Could not restart: " + tid, exn);
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

    private Tid instantiate(String transformName, Tid tid, String[] args,
                            boolean isNew)
        throws DeployException
    {
        ToolboxManagerImpl tbm = (ToolboxManagerImpl)MvvmContextFactory
            .context().toolboxManager();

        URL[] urls = tbm.resources(transformName);
        MackageDesc mackageDesc = tbm.mackageDesc(transformName);

        TransformContextImpl tc = new TransformContextImpl(urls, tid, args,
                                                           mackageDesc, isNew);

        tids.put(tid, tc);

        return tid;
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
