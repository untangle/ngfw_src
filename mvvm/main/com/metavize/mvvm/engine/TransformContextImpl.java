/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TransformContextImpl.java,v 1.38 2005/03/25 02:08:09 jdi Exp $
 */

package com.metavize.mvvm.engine;

import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.UndeployException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.apache.commons.logging.LogFactory;

class TransformContextImpl implements TransformContext
{
    private static final String DESC_PATH = "META-INF/mvvm-transform.xml";
    private static final String BEAN_PATH = "META-INF/beans.xml";

    private static final Map CLASS_LOADER_CONTEXTS =
        Collections.synchronizedMap(new HashMap());

    private static final Logger logger = Logger
        .getLogger(TransformContextImpl.class);

    private final Tid tid;
    private final TransformDesc transformDesc;
    private final MackageDesc mackageDesc;
    private final TransformBase transform;
    private final TransformClassLoader classLoader;
    private final SessionFactory sessionFactory;

    private final TransformManager transformManager = TransformManagerImpl
        .manager();


    TransformContextImpl(URL[] resources, Tid tid, String args[],
                         MackageDesc mackageDesc, boolean isNew)
        throws DeployException
    {
        this.tid = tid;
        this.mackageDesc = mackageDesc;
        this.transformDesc = isNew ? initTransformDesc(resources)
            : initTransformDesc();

        logger.info("Creating transform context for: " + tid
                    + " (" + transformDesc.getName() + ")");

        String parentTransform = transformDesc.getParentTransform();

        TransformContext parentCtx = startParent(parentTransform);

        SchemaUtil.initSchema(transformDesc.getName());

        ClassLoader parentCl = null == parentCtx
            ? getClass().getClassLoader() /* the MvvmClassLoader */
            : parentCtx.transform().getClass().getClassLoader();

        classLoader = new TransformClassLoader(tid, resources, parentCl);
        CLASS_LOADER_CONTEXTS.put(classLoader, this);

        Thread ct = Thread.currentThread();

        ClassLoader oldCl = ct.getContextClassLoader();

        // entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            sessionFactory = Util.makeSessionFactory(classLoader);

            if (isNew) {
                initBeans();
            }

            String tidName = tid.getName();
            logger.debug("setting tran " + tidName + " log4j repository");
            MvvmRepositorySelector.get().init("tran", tidName,
                                              tidName + "-user");

            String className = transformDesc.getClassName();
            transform = (TransformBase)classLoader.loadClass(className)
                .newInstance();

            if (null != parentCtx) {
                transform.setParent((TransformBase)parentCtx.transform());
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
            ct.setContextClassLoader(oldCl);
            // left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    static TransformContext getTransformContext(ClassLoader cl)
    {
        return (TransformContext)CLASS_LOADER_CONTEXTS.get(cl);
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
        return transform.getRunState();
    }

    public TransformStats getStats()
    {
        return transform.getStats();
    }

    // package private methods ------------------------------------------------

    static URLClassLoader[] getClassLoaders()
    {
        return (URLClassLoader[])CLASS_LOADER_CONTEXTS.keySet()
            .toArray(new URLClassLoader[CLASS_LOADER_CONTEXTS.size()]);
    }

    URLClassLoader getClassLoader()
    {
        return classLoader;
    }

    void destroy() throws UndeployException
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            if (transform.getRunState() == TransformState.RUNNING) {
                transform.stop();
            }
            transform.destroy();
        } catch (TransformException exn) {
            throw new UndeployException(exn);
        } finally {
            ct.setContextClassLoader(classLoader);
            // left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        LogFactory.release(classLoader);

        try {
            sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.delete(transformDesc);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn, exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        CLASS_LOADER_CONTEXTS.remove(classLoader);
    }

    void unload()
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(classLoader);
        try {
            transform.unload();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        CLASS_LOADER_CONTEXTS.remove(classLoader);
    }

    // private methods --------------------------------------------------------

    /**
     * Initialize transform from the database.
     */
    private TransformDesc initTransformDesc() throws DeployException
    {
        TransformDesc transformDesc;

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from TransformDesc td where td.tid = :tid");
            q.setParameter("tid", tid);
            transformDesc = (TransformDesc)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            throw new DeployException("could not get TransformDesc", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        checkInstanceCount(transformDesc);

        return transformDesc;
    }

    /**
     * Initialize transform from 'META-INF/mvvm-transform.xml' in one
     * of the urls.
     *
     * @param urls urls to find transform descriptor.
     * @exception DeployException the descriptor does not parse or
     * parent cannot be loaded.
     */
    private TransformDesc initTransformDesc(URL[] urls) throws DeployException
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

        TransformDesc transformDesc = mth.transformDesc;

        transformDesc.setTid(tid);
        // XXX this isn't supposed to be meaningful:
        transformDesc.setPublicKey(new byte[]
            { (byte)(tid.getId() & 0xFF),
              (byte)((tid.getId() >> 8) & 0xFF) });
        transformDesc.setTargetState(TransformState.INITIALIZED);

        checkInstanceCount(transformDesc);

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.save(transformDesc);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn, exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
            }
        }

        return transformDesc;
    }

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

    private void initBeans() throws DeployException
    {
        Session s = openSession();
        try {
            Transaction tx = s.beginTransaction();

            // XXX assumes no parent cl has this file
            InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(BEAN_PATH);

            if (null != is) {
                XMLDecoder xd = new XMLDecoder(is);
                try {
                    while (true) {
                        Object o = xd.readObject();
                        addTid(o);
                        s.save(o);
                    }
                } catch (ArrayIndexOutOfBoundsException exn) {
                    /* they throw this when no more Objects are left */
                }
                xd.close();
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn, exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Session", exn);
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

        logger.debug("Starting parent: " + parentTransform
                     + " for: " + tid);

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
                    logger.warn("Too many instances name: "
                                + parentTransform
                                + " instances: " + tids.length);
                    throw new TooManyInstancesException
                        ("could not create 1 instance");
                }
                ctx = transformManager.transformContext(tids[0]);
            }
        } else if (1 == tids.length) {
            logger.debug("Parent exists, using parent context");
            ctx = transformManager.transformContext(tids[0]);
        } else if (1 < tids.length) {
            logger.warn(parentTransform
                        + " has multiple instances, cannot be a parent");
            throw new TooManyInstancesException
                ("too many instances: " + parentTransform);
        }

        return ctx;
    }
}
