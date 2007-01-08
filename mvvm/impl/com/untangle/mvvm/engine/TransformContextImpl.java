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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.IPSessionDesc;
import com.untangle.mvvm.tapi.TransformBase;
import com.untangle.mvvm.tapi.TransformListener;
import com.untangle.mvvm.tapi.TransformStateChangeEvent;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.tran.DeployException;
import com.untangle.mvvm.tran.TooManyInstancesException;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformDesc;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformPreferences;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.tran.UndeployException;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

// XXX decouple from TransformBase
class TransformContextImpl implements TransformContext
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final URL[] URL_PROTO = new URL[0];

    private final TransformDesc transformDesc;
    private final Tid tid;
    private final TransformPreferences transformPreferences;
    private final TransformPersistentState persistentState;
    private final URLClassLoader classLoader;
    private final boolean isNew;

    private TransformBase transform;
    private String mackageName;

    private final TransformManagerImpl transformManager = TransformManagerImpl
        .manager();
    private final ToolboxManagerImpl toolboxManager = ToolboxManagerImpl
        .toolboxManager();

    TransformContextImpl(URLClassLoader classLoader, TransformDesc tDesc,
                         String mackageName, boolean isNew)
        throws DeployException
    {
        MvvmContextImpl mctx = MvvmContextImpl.getInstance();
        LoggingManagerImpl lm = mctx.loggingManager();
        if (null != tDesc.getTransformBase()) {
            lm.initSchema(tDesc.getTransformBase());
        }
        lm.initSchema(tDesc.getName());

        this.classLoader = classLoader;

        this.transformDesc = tDesc;
        this.tid = transformDesc.getTid();
        this.mackageName = mackageName;
        this.isNew = isNew;

        checkInstanceCount(transformDesc);

        if (isNew) {
            // XXX this isn't supposed to be meaningful:
            byte[] pKey = new byte[]
                { (byte)(tid.getId() & 0xFF),
                  (byte)((tid.getId() >> 8) & 0xFF) };


            persistentState = new TransformPersistentState
                (tid, mackageName, pKey);

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
            mctx.runTransaction(tw);
        } else {
            LoadSettings ls = new LoadSettings(tid);
            mctx.runTransaction(ls);
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

        final MvvmLocalContext mctx = MvvmContextFactory.context();
        try {
            transformManager.registerThreadContext(this);

            String tidName = tid.getName();
            logger.debug("setting tran " + tidName + " log4j repository");

            String className = transformDesc.getClassName();
            transform = (TransformBase)classLoader.loadClass(className)
                .newInstance();

            for (TransformContext parentCtx : parentCtxs) {
                transform.addParent((TransformBase)parentCtx.transform());
            }

            transform.addTransformListener(new TransformListener()
                {
                    public void stateChange(TransformStateChangeEvent te) {
                        {
                            final TransformState ts = te.getTransformState();

                            TransactionWork tw = new TransactionWork()
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

                            mctx.eventLogger().log(new TransformStateChange(tid, ts));
                        }
                    }
                });

            if (isNew) {
                transform.initializeSettings();
                transform.init(args);
                boolean enabled = toolboxManager.isEnabled(mackageName);
                if (!enabled) {
                    transform.disable();
                }
            } else {
                transform.resumeState(persistentState.getTargetState(), args);
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
                mctx.runTransaction(tw);
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
        return toolboxManager.mackageDesc(mackageName);
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

    // XXX should be LocalTransformContext ------------------------------------

    public URLClassLoader getClassLoader()
    {
        return classLoader;
    }

    // XXX remove this method...
    @Deprecated
    public boolean runTransaction(TransactionWork tw)
    {
        return MvvmContextFactory.context().runTransaction(tw);
    }

    public InputStream getResourceAsStream(String res)
    {
        try {
            URL url = new URL(toolboxManager.getResourceDir(mackageName), res);
            File f = new File(url.toURI());
            return new FileInputStream(f);
        } catch (MalformedURLException exn) {
            logger.warn("could not not be found: " + res, exn);
            return null;
        } catch (URISyntaxException exn) {
            logger.warn("could not not be found: " + res, exn);
            return null;
        } catch (FileNotFoundException exn) {
            logger.warn("could not not be found: " + res, exn);
            return null;
        }
    }


    // package private methods ------------------------------------------------

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
    }

    void unload()
    {
        if (transform != null) {
            Thread ct = Thread.currentThread();
            ClassLoader oldCl = ct.getContextClassLoader();
            // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            ct.setContextClassLoader(classLoader);
            try {
                transformManager.registerThreadContext(this);
                transform.unload();
            } finally {
                transformManager.deregisterThreadContext();
                Thread.currentThread().setContextClassLoader(oldCl);
                // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }
        }
    }

    void destroyPersistentState()
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    tid.setPolicy(null);
                    s.update(tid);
                    s.delete(persistentState);
                    s.delete(getTransformPreferences());
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);
    }

    // private classes --------------------------------------------------------

    private class LoadSettings extends TransactionWork
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

            if (!toolboxManager.isEnabled(mackageName)) {
                persistentState.setTargetState(TransformState.DISABLED);
                s.merge(persistentState);
            } else if (TransformState.DISABLED == persistentState.getTargetState()) {
                persistentState.setTargetState(TransformState.INITIALIZED);
                s.merge(persistentState);
            }

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
