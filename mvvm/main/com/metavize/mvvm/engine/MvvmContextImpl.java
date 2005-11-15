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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.argon.Argon;
import com.metavize.mvvm.argon.ArgonManagerImpl;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.util.TransactionRunner;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;


public class MvvmContextImpl extends MvvmContextBase
    implements MvvmLocalContext
{
    private static final MvvmContextImpl CONTEXT = new MvvmContextImpl();

    private static final String BACKUP_SCRIPT;
    private static final String ACTIVATE_SCRIPT;
    private static final String ACTIVATION_KEY_FILE;
    private static final String LOCAL_ARG;
    private static final String USB_ARG;
    private static final String ARGON_FAKE_KEY;

    private final SessionFactory sessionFactory;
    private final TransactionRunner transactionRunner;
    private final Logger logger = Logger.getLogger(MvvmContextImpl.class);

    private MvvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private HttpInvoker httpInvoker;
    private LoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private EventLogger eventLogger;
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
        transactionRunner = new TransactionRunner(sessionFactory);
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

    public SyslogManagerImpl syslogManager()
    {
        return syslogManager;
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

    //Aaron/John - I wasn't ready to
    // (a) expose "main" to the transforms or
    // (b) create a "WebServerManagerImpl" class
    //
    //So for now, I've stuck these two methods directly
    //on "MvvmLocalContext".  Use of them is so
    //limiited right now that refactoring shouldn't
    //be an issue


    public boolean loadWebApp(String urlBase,
      String rootDir) {
      return getMain().loadWebApp(urlBase, rootDir);
    }
    public boolean unloadWebApp(String contextRoot) {
      return getMain().unloadWebApp(contextRoot);
    }


    // service methods --------------------------------------------------------

    public boolean runTransaction(TransactionWork tw)
    {
        return transactionRunner.runTransaction(tw);
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

    public boolean isActivated() {
        // This is ez since we aren't concerned about local box security -- the key is ultimately
        // checked on the release webserver, which is what matters.
        File keyFile = new File(ACTIVATION_KEY_FILE);
        return keyFile.exists();
    }

    public boolean activate(String key) {
        // Be nice to the poor user:
        if (key.length() == 16)
            key = key.substring(0, 4) + "-" + key.substring(4, 8) + "-" +
                key.substring(8, 12) + "-" + key.substring(12,16);
        if (key.length() != 19) {
            // Don't even bother if the key isn't the right length.  Could do other
            // sanity checking here as well. XX
            logger.error("Unable to activate with wrong length key: " + key);
            return false;
        }

        try {
            Process p = Runtime.getRuntime().exec(new String[] { ACTIVATE_SCRIPT, key });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to activate (" + exitValue + ") with key: " + key);
                return false;
            } else {
                logger.info("Product activated with key: " + key);
                return true;
            }
        } catch (InterruptedException exn) {
            logger.error("Interrupted during activation with key: " + key);
            return false;
        } catch (IOException exn) {
            logger.error("Exception during activation with key: " + key, exn);
            return false;
        }
    }

    String getActivationKey()
    {
        try {
            File keyFile = new File(ACTIVATION_KEY_FILE);
            if (keyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                return reader.readLine();
            }
        } catch (IOException x) {
            logger.error("Unable to get activation key: ", x);
        }
        return null;
    }

    public void doFullGC()
    {
        // XXX check access permission
        System.gc();
    }

    public EventLogger eventLogger()
    {
        return eventLogger;
    }

    // MvvmContextBase methods ------------------------------------------------

    @Override
    protected void init()
    {
        syslogManager = SyslogManagerImpl.manager();
        loggingManager = LoggingManagerImpl.loggingManager();
        eventLogger = new EventLogger();
        eventLogger.start();

        // start services:
        adminManager = AdminManagerImpl.adminManager();
        mailSender = MailSenderImpl.mailSender();

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
            eventLogger.stop();
            eventLogger = null;
        } catch (Exception exn) {
            logger.error("could not stop EventLogger", exn);
        }

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
            + "/../../bin/mvvmdb-backup";
        ACTIVATE_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mvactivate";
        ACTIVATION_KEY_FILE = System.getProperty("bunnicula.home")
            + "/activation.key";
        LOCAL_ARG = "local";
        USB_ARG = "usb";
        ARGON_FAKE_KEY = "argon.fake";
    }
}
