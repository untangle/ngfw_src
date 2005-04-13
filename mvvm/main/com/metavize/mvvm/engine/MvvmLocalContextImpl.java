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


import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.argon.Argon;
import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.argon.ArgonManagerImpl;

import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.TransformContext;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.log4j.Logger;

public class MvvmLocalContextImpl extends MvvmContextImpl
    implements MvvmLocalContext
{
    private static final String ARGON_FAKE_KEY = "argon.fake";
    private static final MvvmLocalContextImpl MVVM_LOCAL_CONTEXT
        = new MvvmLocalContextImpl();

    private static final Logger EVENT_LOGGER = Logger.getLogger("eventlog");

    private static final Logger logger = Logger
        .getLogger(MvvmLocalContextImpl.class.getName());

    private final SessionFactory sessionFactory;

    private MPipeManager mPipeManager;
    private PipelineFoundryImpl pipelineFoundry;
    private HttpInvoker httpInvoker;

    // This shouldn't be public but our factory isn't in this package. XXX
    private MvvmLocalContextImpl()
    {
        sessionFactory = Util.makeSessionFactory(getClass().getClassLoader());
    }

    /**
     * MvvmLocalContext singleton, exposed as public so
     * MvvmContextFactory can get it.
     *
     * @return a <code>MvvmLocalContext</code> value
     */
    public static MvvmLocalContext localContext()
    {
        return MVVM_LOCAL_CONTEXT;
    }

    /**
     * For package members to get singleton without casting.
     *
     * @return MvvmLocalContext singleton
     */
    static MvvmLocalContextImpl context()
    {
        return MVVM_LOCAL_CONTEXT;
    }

    public MPipeManager mPipeManager()
    {
        return mPipeManager;
    }

    public PipelineFoundry pipelineFoundry()
    {
        return pipelineFoundry;
    }

    public MvvmLogin mvvmLogin(boolean isLocal)
    {
        return ((AdminManagerImpl)adminManager).mvvmLogin(isLocal);
    }

    public TransformContext transformContext(ClassLoader cl)
    {
        return TransformContextImpl.getTransformContext(cl);
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

    // XXX this should be local only
    public Logger eventLogger()
    {
        return EVENT_LOGGER;
    }

    // Lifecycle methods ------------------------------------------------------

    protected void destroy()
    {
        // stop remote services:
        if (null != httpInvoker) {
            try {
                httpInvoker.destroy();
                httpInvoker = null;
            } catch (Exception exn) {
                logger.warn("could not destroy HttpInvoker", exn);
            }
        }

        // stop vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            try {
                Argon.destroy();
            } catch (Exception exn) {
                logger.warn("could not destroy Argon", exn);
            }
        }

        // stop transforms:
        if (null != transformManager) {
            try {
                transformManager.destroy();
                transformManager = null;
            } catch (Exception exn) {
                logger.warn("could not destroy TransformManager", exn);
            }
        }

        pipelineFoundry = null; // XXX destroy method
        networkingManager = null;
        argonManager = null;

        if (null != mPipeManager) {
            try {
                mPipeManager.destroy();
                mPipeManager = null;
            } catch (Exception exn) {
                logger.warn("could not destroy MPipeManager", exn);
            }
        }

        // stop services:
        if (null != toolboxManager) {
            try {
                toolboxManager.destroy();
                toolboxManager = null;
            } catch (Exception exn) {
                logger.warn("could not destroy ToolboxManager", exn);
            }
        }

        mailSender = null; // XXX destroy method
        adminManager = null; // XXX destroy method

        try {
            sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }
    }

    // protected methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected InvokerBase getInvoker()
    {
        return httpInvoker;
    }

    protected void init()
    {
        // start services:
        adminManager = AdminManagerImpl.adminManager();
        mailSender = MailSenderImpl.mailSender();
        loggingManager = LoggingManagerImpl.loggingManager();
        toolboxManager = ToolboxManagerImpl.toolboxManager();

        mPipeManager = MPipeManager.manager();
        pipelineFoundry = PipelineFoundryImpl.foundry();

        // start transforms:
        transformManager = TransformManagerImpl.manager();

        // Retrieve the networking configuration manager
        networkingManager = NetworkingManagerImpl.getInstance();

        // Retrieve the argon manager
        argonManager = ArgonManagerImpl.getInstance();

        // start vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            Argon.main(new String[0]); // XXX static
        } else {
            logger.info("Argon not activated");
        }

        // allow login:
        httpInvoker = HttpInvoker.invoker();
    }

    protected void postInit()
    {
        logger.debug("restarting transforms");
        transformManager.init();

        logger.debug("starting socket invoker");
        httpInvoker.init();
        logger.debug("postInit complete");
    }
}

