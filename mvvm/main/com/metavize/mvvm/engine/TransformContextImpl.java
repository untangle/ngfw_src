/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.toolbox.MackageDesc;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tran.DeployException;
import com.metavize.mvvm.tran.TooManyInstancesException;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformPreferences;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.UndeployException;
import com.metavize.mvvm.util.TransactionRunner;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

class TransformContextImpl implements TransformContext
{
    private static final Logger logger = Logger.getLogger(TransformContextImpl.class);

    private static final URL[] URL_PROTO = new URL[0];

    private final TransformDesc transformDesc;
    private final Tid tid;
    private final TransformPreferences transformPreferences;
    private final TransformPersistentState persistentState;
    private final MackageDesc mackageDesc;
    private final URLClassLoader classLoader;
    private final boolean isNew;

    private TransformBase transform;
    private SessionFactory sessionFactory;
    private TransactionRunner transactionRunner;

    private final TransformManagerImpl transformManager = TransformManagerImpl
        .manager();
    private final ToolboxManagerImpl toolboxManager = ToolboxManagerImpl
        .toolboxManager();

    TransformContextImpl(URLClassLoader classLoader, TransformDesc tDesc,
                         MackageDesc mackageDesc, boolean isNew)
        throws DeployException
    {
        if (null != tDesc.getTransformBase()) {
            EventLogger.initSchema(tDesc.getTransformBase());
        }
        EventLogger.initSchema(tDesc.getName());

        this.classLoader = classLoader;

        this.transformDesc = tDesc;
        this.tid = transformDesc.getTid();
        this.mackageDesc = mackageDesc;
        this.isNew = isNew;

        checkInstanceCount(transformDesc);

        if (isNew) {
            // XXX this isn't supposed to be meaningful:
            byte[] pKey = new byte[]
                { (byte)(tid.getId() & 0xFF),
                  (byte)((tid.getId() >> 8) & 0xFF) };

            persistentState = new TransformPersistentState
                (tid, mackageDesc.getName(), pKey);

            transformPreferences = new TransformPreferences(tid);

            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        s.save(persistentState);
                        s.save(transformPreferences);
                        return true;
                    }

                    public Object getResult() { return null; }
                };
            MvvmContextFactory.context().runTransaction(tw);
        } else {
            LoadSettings ls = new LoadSettings(tid);
            MvvmContextFactory.context().runTransaction(ls);
            this.persistentState = ls.getPersistentState();
            this.transformPreferences = ls.getTransformPreferences();
        }

        logger.info("Creating transform context for: " + tid
                    + " (" + transformDesc.getName() + ")");
    }

    void init(String[] args) throws DeployException
    {
        Set<TransformContext>parentCtxs = new HashSet<TransformContext>();
        List<String> parents = transformDesc.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent, tid.getPolicy()));
        }

        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // entering transform ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            transformManager.registerThreadContext(this);
            sessionFactory = Util.makeSessionFactory(classLoader);
            transactionRunner = new TransactionRunner(sessionFactory);

            String tidName = tid.getName();
            logger.debug("setting tran " + tidName + " log4j repository");
            MvvmRepositorySelector.get().init("tran", tidName,
                                              tidName + "-user");

            String className = transformDesc.getClassName();
            transform = (TransformBase)classLoader.loadClass(className)
                .newInstance();

            for (TransformContext parentCtx : parentCtxs) {
                transform.addParent((TransformBase)parentCtx.transform());
            }

            if (isNew) {
                transform.initializeSettings();
                transform.init(args);
            } else {
                transform.resumeState();
            }
        } catch (ClassNotFoundException exn) {
            throw new DeployException(exn);
        } catch (InstantiationException exn) {
            throw new DeployException(exn);
        } catch (IllegalAccessException exn) {
            throw new DeployException(exn);
        } catch (TransformException exn) {
            throw new DeployException(exn);
        } finally {
            transformManager.deregisterThreadContext();
            ct.setContextClassLoader(oldCl);
            // left transform ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            if (null == transform) {
                TransactionWork tw = new TransactionWork()
                    {
                        public boolean doWork(Session s)
                        {
                            s.delete(persistentState);
                            return true;
                        }

                        public Object getResult() { return null; }
                    };
                MvvmContextFactory.context().runTransaction(tw);
            }

        }
    }

    // TransformContext -------------------------------------------------------

    public Tid getTid()
    {
        return tid;
    }

    public TransformDesc getTransformDesc()
    {
        return transformDesc;
    }

    public TransformPreferences getTransformPreferences()
    {
        return transformPreferences;
    }

    public MackageDesc getMackageDesc()
    {
        return mackageDesc;
    }

    public boolean runTransaction(TransactionWork tw)
    {
        return transactionRunner.runTransaction(tw);
    }

    public Transform transform()
    {
        return transform;
    }

    // transform call-through methods -----------------------------------------

    public IPSessionDesc[] liveSessionDescs()
    {
        return transform.liveSessionDescs();
    }

    public TransformState getRunState()
    {
        return null == transform ? TransformState.LOADED
            : transform.getRunState();
    }

    public TransformStats getStats()
    {
        return transform.getStats();
    }

    public URLClassLoader getClassLoader()
    {
        return classLoader;
    }

    // package private methods ------------------------------------------------

    TransformPersistentState getPersistentState()
    {
        return persistentState;
    }

    void destroy() throws UndeployException
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            transformManager.registerThreadContext(this);
            if (transform.getRunState() == TransformState.RUNNING) {
                transform.stop();
            }
            transform.destroy();
        } catch (TransformException exn) {
            throw new UndeployException(exn);
        } finally {
            transformManager.deregisterThreadContext();
            ct.setContextClassLoader(classLoader);
            // left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        LogFactory.release(classLoader);

        try {
            sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }
    }

    void unload()
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            transformManager.registerThreadContext(this);
            transform.unload();
        } finally {
            transformManager.deregisterThreadContext();
            Thread.currentThread().setContextClassLoader(oldCl);
            // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    // private classes --------------------------------------------------------

    private static class LoadSettings extends TransactionWork
    {
        private final Tid tid;

        private TransformPersistentState persistentState;
        private TransformPreferences transformPreferences;

        public LoadSettings(Tid tid)
        {
            this.tid = tid;
        }

        public boolean doWork(Session s)
        {
            Query q = s.createQuery
                ("from TransformPersistentState tps where tps.tid = :tid");
            q.setParameter("tid", tid);
            persistentState = (TransformPersistentState)q.uniqueResult();

            q = s.createQuery
                ("from TransformPreferences tp where tp.tid = :tid");
            q.setParameter("tid", tid);
            transformPreferences = (TransformPreferences)q.uniqueResult();
            return true;
        }

        public Object getResult() { return null; }

        public TransformPersistentState getPersistentState()
        {
            return persistentState;
        }

        public TransformPreferences getTransformPreferences()
        {
            return transformPreferences;
        }
    }

    // private methods --------------------------------------------------------

    private void checkInstanceCount(TransformDesc transformDesc)
        throws TooManyInstancesException
    {
        if (transformDesc.isSingleInstance()) {
            String n = transformDesc.getName();
            Policy p = transformDesc.getTid().getPolicy();
            List<Tid> l = transformManager.transformInstances(n, p);

            if (1 == l.size()) {
                if (!tid.equals(l.get(0))) {
                    throw new TooManyInstancesException("too many instances: " + n);
                }
            } else if (1 < l.size()) {
                throw new TooManyInstancesException("too many instances: " + n);
            }
        }
    }

    private void addTid(Object o)
    {
        try {
            Method m = o.getClass().getMethod("setTid", Tid.class);
            m.invoke(o, tid);
        } catch (NoSuchMethodException exn) {
            /* no setTid(Tid) method, nothing to do */
            return;
        } catch (SecurityException exn) {
            logger.warn(exn); /* shouldn't happen */
        } catch (IllegalAccessException exn) {
            logger.warn(exn); /* shouldn't happen */
        } catch (IllegalArgumentException exn) {
            logger.warn(exn); /* shouldn't happen */
        } catch (InvocationTargetException exn) {
            logger.warn(exn); /* shouldn't happen */
        }
    }

    private TransformContext startParent(String parent, Policy policy)
        throws DeployException
    {
        if (null == parent) {
            return null;
        }

        MackageDesc md = toolboxManager.mackageDesc(parent);
        if (md.isService()) {
            policy = null;
        }

        logger.debug("Starting parent: " + parent + " for: " + tid);

        TransformContext pctx = getParentContext(parent);

        if (null == pctx) {
            logger.debug("Parent does not exist, instantiating");

            try {
                Tid parentTid = transformManager.instantiate(parent, policy);
                pctx = transformManager.transformContext(parentTid);
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

    private TransformContext getParentContext(String parent)
    {
        for (Tid t : transformManager.transformInstances(parent)) {
            Policy p = t.getPolicy();
            if (null == p || p.equals(tid.getPolicy())) {
                return transformManager.transformContext(t);
            }

        }

        return null;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "TransformContext tid: " + tid
            + " (" + transformDesc.getName() + ")";
    }
}
