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

import java.io.IOException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.argon.Argon;
import com.metavize.mvvm.argon.ArgonManagerImpl;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.log4j.Logger;


public class MvvmContextImpl extends MvvmContextBase
    implements MvvmLocalContext
{
    private static final MvvmContextImpl CONTEXT = new MvvmContextImpl();

    private static final String BACKUP_SCRIPT;
    private static final String LOCAL_ARG;
    private static final String USB_ARG;
    private static final String ARGON_FAKE_KEY;

    private final SessionFactory sessionFactory;
    private final Logger logger = Logger.getLogger(MvvmContextImpl.class);
    private final Logger eventLogger = Logger.getLogger("eventlog");

    private AdminManagerImpl adminManager;
    private ArgonManager argonManager;
    private HttpInvoker httpInvoker;
    private LoggingManagerImpl loggingManager;
    private MPipeManager mPipeManager;
    private MailSenderImpl mailSender;
    private NetworkingManager networkingManager;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private TransformManagerImpl transformManager;
    private MvvmRemoteContext remoteContext;

    // constructor ------------------------------------------------------------

    private MvvmContextImpl()
    {
        sessionFactory = Util.makeSessionFactory(getClass().getClassLoader());
    }

    // static factory ---------------------------------------------------------

    public static MvvmLocalContext context()
    {
        return CONTEXT;
    }

    static MvvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    // singletons -------------------------------------------------------------

    public ToolboxManager toolboxManager()
    {
        return toolboxManager;
    }

    public TransformManager transformManager()
    {
        return transformManager;
    }

    public LoggingManager loggingManager()
    {
        return loggingManager;
    }

    public MailSender mailSender()
    {
        return mailSender;
    }

    public AdminManager adminManager()
    {
        return adminManager;
    }

    public NetworkingManager networkingManager()
    {
        return networkingManager;
    }

    public ArgonManager argonManager()
    {
        return argonManager;
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

    // service methods --------------------------------------------------------

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

    public void shutdown()
    {
        // XXX check access permission
        new Thread(new Runnable()
            {
                public void run()
                {
                    try {
                        Thread.currentThread().sleep(200);
                    } catch (InterruptedException exn) { }
                    logger.info("thank you for choosing bunnicula");
                    System.exit(0);
                }
            }).start();
    }

    public void localBackup() throws IOException
    {
        backup(true);
    }

    public void usbBackup() throws IOException
    {
        backup(false);
    }

    public void doFullGC()
    {
        // XXX check access permission
        System.gc();
    }

    public Logger eventLogger()
    {
        return eventLogger;
    }

    // MvvmContextBase methods ------------------------------------------------

    @Override
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

        httpInvoker = HttpInvoker.invoker();

        remoteContext = new MvvmRemoteContextImpl(this);
    }

    @Override
    protected void postInit()
    {
        logger.debug("restarting transforms");
        transformManager.init();

        logger.debug("starting HttpInvoker");
        httpInvoker.init();
        logger.debug("postInit complete");
    }

    @Override
    protected void destroy()
    {
        // stop remote services:
        try {
            httpInvoker.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy HttpInvoker", exn);
        }
        httpInvoker = null;

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
        try {
            transformManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy TransformManager", exn);
        }
        transformManager = null;

        // XXX destroy methods for:
        // - pipelineFoundry
        // - networkingManager
        // - argonManager

        try {
            mPipeManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy MPipeManager", exn);
        }
        mPipeManager = null;

        // stop services:
        try {
            toolboxManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy ToolboxManager", exn);
        }
        toolboxManager = null;

        // XXX destroy methods for:
        // - mailSender
        // - adminManager

        try {
            sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }
    }

    @Override
    protected InvokerBase getInvoker()
    {
        return httpInvoker;
    }

    // package protected methods ----------------------------------------------

    MvvmRemoteContext remoteContext()
    {
        return remoteContext;
    }

    // private methods --------------------------------------------------------

    private void backup(boolean local) throws IOException
    {
        Process p = Runtime.getRuntime().exec(new String[]
            { BACKUP_SCRIPT, local ? LOCAL_ARG : USB_ARG });
        for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );


        while (true) {
            try {
                int exitValue = p.waitFor();
                if (0 != exitValue) {
                    throw new IOException("dump not successful");
                } else {
                    return;
                }
            } catch (InterruptedException exn) { }
        }
    }

    // static initializer -----------------------------------------------------

    static {
        BACKUP_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mvvmdb-backup";;
        LOCAL_ARG = "local";
        USB_ARG = "usb";
        ARGON_FAKE_KEY = "argon.fake";
    }
}
