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

import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.argon.Argon;
import com.metavize.mvvm.argon.ArgonManagerImpl;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

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

    private MvvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private HttpInvoker httpInvoker;
    private LoggingManagerImpl loggingManager;
    private PolicyManagerImpl policyManager;
    private MPipeManagerImpl mPipeManager;
    private MailSenderImpl mailSender;
    private NetworkingManagerImpl networkingManager;
    private ReportingManagerImpl reportingManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private TransformManagerImpl transformManager;
    private MvvmRemoteContext remoteContext;

    // constructor ------------------------------------------------------------

    private MvvmContextImpl()
    {
        sessionFactory = Util.makeSessionFactory(getClass().getClassLoader());
        state = MvvmState.LOADED;
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

    public MvvmState state()
    {
        return state;
    }

    // singletons -------------------------------------------------------------

    public ToolboxManagerImpl toolboxManager()
    {
        return toolboxManager;
    }

    public TransformManagerImpl transformManager()
    {
        return transformManager;
    }

    public LoggingManagerImpl loggingManager()
    {
        return loggingManager;
    }

    public PolicyManagerImpl policyManager()
    {
        return policyManager;
    }

    public MailSenderImpl mailSender()
    {
        return mailSender;
    }

    public AdminManagerImpl adminManager()
    {
        return adminManager;
    }

    public NetworkingManagerImpl networkingManager()
    {
        return networkingManager;
    }

    public ReportingManagerImpl reportingManager()
    {
        return reportingManager;
    }

    public ConnectivityTesterImpl getConnectivityTester()
    {
        return connectivityTester;
    }

    public ArgonManagerImpl argonManager()
    {
        return argonManager;
    }

    public MPipeManagerImpl mPipeManager()
    {
        return mPipeManager;
    }

    public PipelineFoundryImpl pipelineFoundry()
    {
        return pipelineFoundry;
    }

    public MvvmLoginImpl mvvmLogin()
    {
        return adminManager.mvvmLogin();
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

    public Thread newThread(final Runnable runnable)
    {
        return new Thread(new Runnable()
            {
                TransformContext tctx = transformManager.threadContext();

                public void run()
                {
                    transformManager.registerThreadContext(tctx);
                    try {
                        runnable.run();
                    } catch (Exception exn) {
                        logger.error("Exception running: " + runnable, exn);
                    } finally {
                        transformManager.deregisterThreadContext();
                    }
                }
            });
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

    public String version()
    {
        return com.metavize.mvvm.engine.Version.getVersion();
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

        // Fire up the policy manager.
        policyManager = PolicyManagerImpl.policyManager();

        toolboxManager = ToolboxManagerImpl.toolboxManager();

        mPipeManager = MPipeManagerImpl.manager();
        pipelineFoundry = PipelineFoundryImpl.foundry();

        // start transforms:
        transformManager = TransformManagerImpl.manager();

        // Retrieve the networking configuration manager
        networkingManager = NetworkingManagerImpl.getInstance();

        // Retrieve the reporting configuration manager
        reportingManager = ReportingManagerImpl.reportingManager();

        // Retrieve the connectivity tester
        connectivityTester = ConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        argonManager = ArgonManagerImpl.getInstance();

        // start vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            Argon.getInstance().run( policyManager );
        } else {
            logger.info( "Argon not activated, using fake interfaces in the policy manager" +
                         " and networking manager." );
            byte interfaces[] = new byte[] { 0, 1 };
            policyManager.reconfigure(interfaces);
            // this is done by the policy manager, but leave it here just in case.
            networkingManager.buildIntfEnum();
        }

        httpInvoker = HttpInvoker.invoker();

        remoteContext = new MvvmRemoteContextImpl(this);
        state = MvvmState.INITIALIZED;
    }

    @Override
    protected void postInit()
    {
        logger.debug("restarting transforms");
        transformManager.init();

        logger.debug("starting HttpInvoker");
        httpInvoker.init();
        logger.debug("postInit complete");
        state = MvvmState.RUNNING;
    }

    @Override
    protected void destroy()
    {
        state = MvvmState.DESTROYED;

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
                Argon.getInstance().destroy();
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
        // - reportingManager
        // - connectivityTester (Doesn't really need one)
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
