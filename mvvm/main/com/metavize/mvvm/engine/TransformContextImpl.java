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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.MvvmContextFactory;
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
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

class TransformContextImpl implements TransformContext
{
    private static final Logger logger = Logger.getLogger(TransformContextImpl.class);

    private static final URL[] URL_PROTO = new URL[0];

    private final TransformDesc transformDesc;
    private final Tid tid;
    private final TransformPreferences transformPreferences;
    private final TransformPersistentState persistentState;
    private final MackageDesc mackageDesc;
    private final ClassLoader classLoader;
    private final boolean isNew;

    private TransformBase transform;
    private SessionFactory sessionFactory;

    private final TransformManagerImpl transformManager = TransformManagerImpl
        .manager();

    TransformContextImpl(URL[] resources, TransformDesc transformDesc,
                         MackageDesc mackageDesc, boolean isNew)
        throws DeployException
    {
        if (null != transformDesc.getTransformBase()) {
            SchemaUtil.initSchema(transformDesc.getTransformBase());
        }
        SchemaUtil.initSchema(transformDesc.getName());

        this.transformDesc = transformDesc;
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

            Session s = MvvmContextFactory.context().openSession();
            try {
                Transaction tx = s.beginTransaction();

                s.save(persistentState);
                s.save(transformPreferences);

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn(exn, exn);
                throw new DeployException(exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close Session", exn);
                }
            }
        } else {
            Session s = MvvmContextFactory.context().openSession();
            try {
                Transaction tx = s.beginTransaction();

                Query q = s.createQuery
                    ("from TransformPersistentState tps where tps.tid = :tid");
                q.setParameter("tid", tid);
                persistentState = (TransformPersistentState)q.uniqueResult();

                q = s.createQuery
                    ("from TransformPreferences tp where tp.tid = :tid");
                q.setParameter("tid", tid);
                transformPreferences = (TransformPreferences)q.uniqueResult();

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn(exn, exn);
                throw new DeployException(exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close Session", exn);
                }
            }
        }

        logger.info("Creating transform context for: " + tid
                    + " (" + transformDesc.getName() + ")");

        CasingClassLoader parentCl = transformManager.getCasingClassLoader();
        classLoader = new URLClassLoader(resources, parentCl);
    }

    void init(String[] args) throws DeployException
    {
        List<String> exports = transformDesc.getExports();
        URL[] urls = new URL[exports.size()];
        int i = 0;
        for (String export : exports) {
            try {
                URL url = new URL(ToolboxManagerImpl.TOOLBOX_URL, export);
                urls[i++] = url;
            } catch (MalformedURLException exn) {
                throw new DeployException("bad export: " + export, exn);
            }
        }
        transformManager.getCasingClassLoader().addResources(urls);

        Set<TransformContext>parentCtxs = new HashSet<TransformContext>();
        List<String> parents = transformDesc.getParents();
        for (String parent : parents) {
            parentCtxs.add(startParent(parent));
        }

        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // entering transform ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            transformManager.registerThreadContext(this);
            sessionFactory = Util.makeSessionFactory(classLoader);

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

    public Session openSession()
    {
        Session s = null;

        try {
            s = sessionFactory.openSession();
        } catch (HibernateException exn) {
            logger.warn("Could not create Hibernate Session", exn);
        }

        return s;
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
        return null == transform ? TransformState.NOT_LOADED
            : transform.getRunState();
    }

    public TransformStats getStats()
    {
        return transform.getStats();
    }

    // XXX make private when/if we move all impls to engine
    public ClassLoader getClassLoader()
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

    // private methods --------------------------------------------------------

    private void checkInstanceCount(TransformDesc transformDesc)
        throws TooManyInstancesException
    {
        if (transformDesc.isSingleInstance()) {
            Tid[] tids = transformManager
                .transformInstances(transformDesc.getName());

            if (1 == tids.length) {
                if (!tid.equals(tids[0])) {
                    throw new TooManyInstancesException
                        ("too many instances: " + transformDesc.getName());
                }
            } else if (1 < tids.length) {
                throw new TooManyInstancesException
                    ("too many instances: " + transformDesc.getName());
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

    private TransformContext startParent(String parentTransform)
        throws DeployException
    {
        if (null == parentTransform) {
            return null;
        }

        logger.debug("Starting parent: " + parentTransform + " for: " + tid);

        TransformContext ctx = null;

        Tid[] tids = transformManager.transformInstances(parentTransform);
        if (0 == tids.length) {
            logger.debug("Parent does not exist, instantiating");
            try {
                Tid parentTid = transformManager.instantiate(parentTransform);
                ctx = transformManager.transformContext(parentTid);
            } catch (TooManyInstancesException exn) {
                tids = transformManager.transformInstances(parentTransform);
                if (1 != tids.length) {
                    logger.warn("Too many instances name: " + parentTransform
                                + " instances: " + tids.length);
                    throw new TooManyInstancesException("could not create");
                }
                ctx = transformManager.transformContext(tids[0]);
            }
        } else if (1 == tids.length) {
            logger.debug("Parent exists, using parent context");
            ctx = transformManager.transformContext(tids[0]);
        } else if (1 < tids.length) {
            logger.warn(parentTransform + " has multiple instances");
            throw new TooManyInstancesException
                ("too many instances: " + parentTransform);
        }

        return ctx;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "TransformContext tid: " + tid
            + " (" + transformDesc.getName() + ")";
    }
}
