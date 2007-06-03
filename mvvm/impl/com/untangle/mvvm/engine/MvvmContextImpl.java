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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import com.untangle.mvvm.BrandingManager;
import com.untangle.mvvm.CronJob;
import com.untangle.mvvm.LocalBrandingManager;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.MvvmState;
import com.untangle.mvvm.Period;
import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.api.RemoteIntfManager;
import com.untangle.mvvm.api.RemoteShieldManager;
import com.untangle.mvvm.argon.Argon;
import com.untangle.mvvm.argon.ArgonManagerImpl;
import com.untangle.mvvm.client.MvvmRemoteContext;
import com.untangle.mvvm.engine.addrbook.AddressBookFactory;
import com.untangle.mvvm.localapi.LocalIntfManager;
import com.untangle.mvvm.localapi.LocalShieldManager;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.LogMailerImpl;
import com.untangle.mvvm.logging.MvvmRepositorySelector;
import com.untangle.mvvm.networking.NetworkManagerImpl;
import com.untangle.mvvm.networking.RemoteNetworkManagerImpl;
import com.untangle.mvvm.networking.ping.PingManagerImpl;
import com.untangle.mvvm.policy.LocalPolicyManager;
import com.untangle.mvvm.policy.PolicyManager;
import com.untangle.mvvm.tapi.MPipeManager;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformManager;
import com.untangle.mvvm.user.LocalPhoneBook;
import com.untangle.mvvm.user.PhoneBookFactory;
import com.untangle.mvvm.user.RemotePhoneBook;
import com.untangle.mvvm.util.TransactionRunner;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

public class MvvmContextImpl extends MvvmContextBase
    implements MvvmLocalContext
{
    private static final MvvmContextImpl CONTEXT = new MvvmContextImpl();

    private static final String REBOOT_SCRIPT = "/sbin/reboot";

    private static final String ACTIVATE_SCRIPT;
    private static final String ACTIVATION_KEY_FILE;
    private static final String ARGON_FAKE_KEY;

    /* true if running in a development environment */
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";

    private final Object startupWaitLock = new Object();
    private final Logger logger = Logger.getLogger(MvvmContextImpl.class);
    private final BackupManager backupManager;

    private MvvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private RemoteIntfManagerImpl remoteIntfManager;
    private LocalShieldManager localShieldManager;
    private RemoteShieldManager remoteShieldManager;
    private HttpInvokerImpl httpInvoker;
    private LoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private EventLogger eventLogger;
    private LocalPolicyManager policyManager;
    private PolicyManager remotePolicyManager;
    private MPipeManagerImpl mPipeManager;
    private MailSenderImpl mailSender;
    private LogMailerImpl logMailer;
    private NetworkManagerImpl networkManager;
    private PingManagerImpl pingManager;
    private RemoteNetworkManagerImpl remoteNetworkManager;
    private ReportingManagerImpl reportingManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private TransformManagerImpl transformManager;
    private RemoteTransformManagerImpl remoteTransformManager;
    private MvvmRemoteContext remoteContext;
    private CronManager cronManager;
    private AppServerManagerImpl appServerManager;
    private RemoteAppServerManagerImpl remoteAppServerManager;
    private AddressBookFactory addressBookFactory;
    private BrandingManager brandingManager;
    private LocalBrandingManager localBrandingManager;
    private PhoneBookFactory phoneBookFactory;
    private PortalManagerImpl portalManager;
    private RemotePortalManagerImpl remotePortalManager;
    private TomcatManager tomcatManager;
    private HeapMonitor heapMonitor;

    private volatile SessionFactory sessionFactory;
    private volatile TransactionRunner transactionRunner;

    // constructor ------------------------------------------------------------

    private MvvmContextImpl()
    {
        refreshSessionFactory();

        // XXX can we load all transform cl's first?
        state = MvvmState.LOADED;
        backupManager = new BackupManager();
    }

    // static factory ---------------------------------------------------------

    static MvvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    public static MvvmLocalContext context()
    {
        return CONTEXT;
    }

    // singletons -------------------------------------------------------------

    public AddressBook appAddressBook()
    {
        return addressBookFactory.getAddressBook();
    }

    public AddressBook appRemoteAddressBook()
    {
        return addressBookFactory.getRemoteAddressBook();
    }

    public BrandingManager brandingManager()
    {
        return brandingManager;
    }

    public LocalBrandingManager localBrandingManager()
    {
        return localBrandingManager;
    }

    public RemotePhoneBook remotePhoneBook()
    {
        return phoneBookFactory.getRemote();
    }

    public LocalPhoneBook localPhoneBook()
    {
        return phoneBookFactory.getLocal();
    }

    public PortalManagerImpl portalManager()
    {
        return portalManager;
    }

    RemotePortalManagerImpl remotePortalManager()
    {
        return remotePortalManager;
    }

    public AppServerManagerImpl appServerManager()
    {
        return appServerManager;
    }

    RemoteAppServerManagerImpl remoteAppServerManager()
    {
        return remoteAppServerManager;
    }

    public ToolboxManagerImpl toolboxManager()
    {
        return toolboxManager;
    }

    public TransformManagerImpl transformManager()
    {
        return transformManager;
    }

    public TransformManager remoteTransformManager()
    {
        return remoteTransformManager;
    }

    public LoggingManagerImpl loggingManager()
    {
        return loggingManager;
    }

    public SyslogManagerImpl syslogManager()
    {
        return syslogManager;
    }

    public LocalPolicyManager policyManager()
    {
        return policyManager;
    }

    public PolicyManager remotePolicyManager()
    {
        return remotePolicyManager;
    }

    public MailSenderImpl mailSender()
    {
        return mailSender;
    }

    public AdminManagerImpl adminManager()
    {
        return adminManager;
    }

    public NetworkManagerImpl networkManager()
    {
        return networkManager;
    }

    public PingManagerImpl pingManager()
    {
        return pingManager;
    }

    RemoteNetworkManagerImpl remoteNetworkManager()
    {
        return remoteNetworkManager;
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

    RemoteIntfManager remoteIntfManager()
    {
        /* This doesn't have to be synchronized, because it doesn't
         * matter if two are created */
        if (remoteIntfManager == null) {
            // Create the remote interface manager
            LocalIntfManager lim = argonManager.getIntfManager();
            if (null == lim) {
                logger.warn("ArgonManager.getIntfManager() is not initialized");
                return null;
            }
            remoteIntfManager = new RemoteIntfManagerImpl(lim);
        }

        return remoteIntfManager;
    }

    public LocalIntfManager localIntfManager()
    {
        return argonManager.getIntfManager();
    }

    public LocalShieldManager localShieldManager()
    {
        return localShieldManager;
    }

    RemoteShieldManager remoteShieldManager()
    {
        return this.remoteShieldManager;
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

    public void waitForStartup()
    {
        synchronized (startupWaitLock) {
            while (state == MvvmState.LOADED || state == MvvmState.INITIALIZED) {
                try {
                    startupWaitLock.wait();
                } catch (InterruptedException exn) {
                    // reevaluate exit condition
                }
            }
        }
    }

    // service methods --------------------------------------------------------

    public boolean runTransaction(TransactionWork tw)
    {
        return transactionRunner.runTransaction(tw);
    }


    /* For autonumbering anonymous threads. */
    private static class ThreadNumber {
        private static int threadInitNumber = 1;
        public static synchronized int nextThreadNum() {
            return threadInitNumber++;
        }
    }

    public Thread newThread(final Runnable runnable)
    {
        return newThread(runnable, "MVThread-" + ThreadNumber.nextThreadNum());
    }

    public Thread newThread(final Runnable runnable, final String name)
    {
        Runnable task = new Runnable()
            {
                public void run()
                {
                    TransformContext tctx = null == transformManager
                        ? null : transformManager.threadContext();

                    if (null != tctx) {
                        transformManager.registerThreadContext(tctx);
                    }
                    try {
                        runnable.run();
                    } catch (OutOfMemoryError exn) {
                        MvvmContextImpl.getInstance().fatalError("MvvmContextImpl", exn);
                    } catch (Exception exn) {
                        logger.error("Exception running: " + runnable, exn);
                    } finally {
                        if (null != tctx) {
                            transformManager.deregisterThreadContext();
                        }
                    }
                }
            };
        return new Thread(task, name);
    }

    public Process exec(String cmd) throws IOException
    {
        StringTokenizer st = new StringTokenizer(cmd);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = st.nextToken();
        }

        return exec(cmdArray, null, null);
    }

    public Process exec(String[] cmd) throws IOException
    {
        return exec(cmd, null, null);
    }

    public Process exec(String[] cmd, String[] envp) throws IOException
    {
        return exec(cmd, envp, null);
    }

    public Process exec(String[] cmd, String[] envp, File dir) throws IOException
    {
        String[] newCmd = new String[cmd.length + 1];
        newCmd[0] = "mvnice";
        System.arraycopy(cmd, 0, newCmd, 1, cmd.length);
        try {
            return Runtime.getRuntime().exec(newCmd, envp, dir);
        } catch (IOException x) {
            // Check and see if we've run out of virtual memory.  This is very ugly
            // but there's not another apparent way.  XXXXXXXXXX
            String msg = x.getMessage();
            if (msg.contains("Cannot allocate memory")) {
                logger.error("Virtual memory exhausted in Process.exec()");
                MvvmContextImpl.getInstance().fatalError("MvvmContextImpl.exec", x);
                // There's no return from fatalError, but we have to
                // keep the compiler happy.
                return null;
            } else {
                throw x;
            }
        }
    }

    public void shutdown()
    {
        // XXX check access permission
        Thread t = newThread(new Runnable()
            {
                public void run()
                {
                    try {
                        Thread.currentThread().sleep(200);
                    } catch (InterruptedException exn) { }
                    logger.info("thank you for choosing bunnicula");
                    System.exit(0);
                }
            });
        t.setDaemon(true);
        t.start();
    }

    public void rebootBox() {
        try {
            Process p = exec(new String[] { REBOOT_SCRIPT });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to reboot (" + exitValue + ")");
            } else {
                logger.info("Rebooted at admin request");
            }
        } catch (InterruptedException exn) {
            logger.error("Interrupted during reboot");
        } catch (IOException exn) {
            logger.error("Exception during rebooot");
        }
    }

    public MvvmState state()
    {
        return state;
    }

    public String version()
    {
        return com.untangle.mvvm.Version.getVersion();
    }

    public void localBackup() throws IOException
    {
        backupManager.localBackup();
    }

    public void usbBackup() throws IOException
    {
        backupManager.usbBackup();
    }

    public byte[] createBackup() throws IOException {
        return backupManager.createBackup();
    }

    public void restoreBackup(byte[] backupBytes)
        throws IOException, IllegalArgumentException {
        backupManager.restoreBackup(backupBytes);
    }

    public boolean isActivated() {
        // This is ez since we aren't concerned about local box
        // security -- the key is ultimately checked on the release
        // webserver, which is what matters.
        File keyFile = new File(ACTIVATION_KEY_FILE);
        return keyFile.exists();
    }

    public boolean isDevel()
    {
        return Boolean.getBoolean(PROPERTY_IS_DEVEL);
    }

    public boolean activate(String key) {
        // Be nice to the poor user:
        if (key.length() == 16)
            key = key.substring(0, 4) + "-" + key.substring(4, 8) + "-" +
                key.substring(8, 12) + "-" + key.substring(12,16);
        // Fix for bug 1310: Make sure all the hex chars are lower cased.
        key = key.toLowerCase();
        if (key.length() != 19) {
            // Don't even bother if the key isn't the right length.
            // Could do other sanity checking here as well. XX
            logger.error("Unable to activate with wrong length key: " + key);
            return false;
        }

        try {
            Process p = exec(new String[] { ACTIVATE_SCRIPT, key });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to activate (" + exitValue
                             + ") with key: " + key);
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

    public void doFullGC()
    {
        // XXX check access permission
        System.gc();
    }

    public EventLogger eventLogger()
    {
        return eventLogger;
    }

    public CronJob makeCronJob(Period p, Runnable r)
    {
        return cronManager.makeCronJob(p, r);
    }

    public void loadLibrary(String libname) {
        System.loadLibrary(libname);
    }

    // MvvmContextBase methods ------------------------------------------------

    @Override
    protected void init()
    {
        cronManager = new CronManager();
        syslogManager = SyslogManagerImpl.manager();
        MvvmRepositorySelector repositorySelector = MvvmRepositorySelector.selector();
        loggingManager = new LoggingManagerImpl(repositorySelector);
        loggingManager.initSchema("mvvm");
        loggingManager.start();
        eventLogger = EventLoggerFactory.factory().getEventLogger();

        // Create the tomcat manager *before* the MVVM, so we can
        // "register" webapps to be started before Tomcat exists.
        tomcatManager = new TomcatManager(this,
                                          System.getProperty("bunnicula.home"),
                                          System.getProperty("bunnicula.web.dir"),
                                          System.getProperty("bunnicula.log.dir"));

        // start services:
        adminManager = new AdminManagerImpl(this);
        mailSender = MailSenderImpl.mailSender();

        logMailer = new LogMailerImpl();
        repositorySelector.setLogMailer(logMailer);

        // Fire up the policy manager.
        String pmClass = System.getProperty("mvvm.policy.manager");
        if (null == pmClass) {
            pmClass = "com.untangle.mvvm.policy.CharonPolicyManager";
        }

        LocalPolicyManager pm = null;
        try {
            pm = (LocalPolicyManager)Class.forName(pmClass).newInstance();
        } catch (Exception exn) {
            logger.info("could not load PolicyManager: " + pmClass);
        }
        policyManager = null == pm ? new DefaultPolicyManager() : pm;
        logger.info("using PolicyManager: " + policyManager.getClass());
        remotePolicyManager = new RemotePolicyManagerAdaptor(policyManager);

        toolboxManager = ToolboxManagerImpl.toolboxManager();

        mPipeManager = MPipeManagerImpl.manager();
        pipelineFoundry = PipelineFoundryImpl.foundry();

        // Retrieve the network settings manager.  (Kind of busted,
        // but NAT may register a listener, and thus the network
        // manager should exist.
        networkManager = NetworkManagerImpl.getInstance();
        remoteNetworkManager = new RemoteNetworkManagerImpl(networkManager);

        pingManager = PingManagerImpl.getInstance();

        //Start AddressBookImpl
        addressBookFactory = AddressBookFactory.makeInstance();

        localBrandingManager = new BrandingManagerImpl();
        brandingManager = new RemoteBrandingManagerImpl(localBrandingManager);
        
        phoneBookFactory = PhoneBookFactory.makeInstance();

        portalManager = new PortalManagerImpl(this);
        remotePortalManager = new RemotePortalManagerImpl(portalManager);

        // start transforms:
        transformManager = new TransformManagerImpl(repositorySelector);
        remoteTransformManager = new RemoteTransformManagerImpl(transformManager);

        // Retrieve the reporting configuration manager
        reportingManager = ReportingManagerImpl.reportingManager();

        // Retrieve the connectivity tester
        connectivityTester = ConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        argonManager = ArgonManagerImpl.getInstance();

        // Create the shield managers
        localShieldManager = new LocalShieldManagerImpl();
        remoteShieldManager = new RemoteShieldManagerImpl(localShieldManager);

        appServerManager = new AppServerManagerImpl(this);
        remoteAppServerManager = new RemoteAppServerManagerImpl(appServerManager);

        // start vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            Argon.getInstance().run( policyManager, networkManager );
        } else {
            logger.info( "Argon not activated, using fake interfaces in the "
                         + "policy and networking manager." );
            byte interfaces[] = new byte[] { 0, 1 };
            policyManager.reconfigure(interfaces);
            // this is done by the policy manager, but leave it here
            // just in case.
            // XXXX no longer needed.
            // networkingManager.buildIntfEnum();
        }

        this.heapMonitor = new HeapMonitor();
        String useHeapMonitor = System.getProperty(HeapMonitor.KEY_ENABLE_MONITOR);
        if (null != useHeapMonitor && Boolean.valueOf(useHeapMonitor)) {
            this.heapMonitor.start();
        }

        /* initalize everything and start it up */
        phoneBookFactory.init();

        httpInvoker = HttpInvokerImpl.invoker();

        remoteContext = new MvvmRemoteContextImpl(this);
        state = MvvmState.INITIALIZED;
    }

    @Override
    protected void postInit()
    {
        syslogManager.postInit();

        // Mailsender can now query the hostname
        mailSender.postInit();

        logger.debug("restarting transforms");
        transformManager.init();

        logger.debug("starting HttpInvoker");
        httpInvoker.init();
        logger.debug("postInit complete");
        synchronized (startupWaitLock) {
            state = MvvmState.RUNNING;
            startupWaitLock.notifyAll();
        }

        //Inform the AppServer manager that everything
        //else is started.
        appServerManager.postInit(httpInvoker);
    }

    @Override
    protected void destroy()
    {
        state = MvvmState.DESTROYED;

        // stop remote services:
        try {
            if (httpInvoker != null)
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

        // Stop portal
        try {
            portalManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy PortalManager", exn);
        }
        portalManager = null;

        // destroy the phonebook:
        try {
            if (phoneBookFactory != null)
                phoneBookFactory.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy LocalPhoneBook", exn);
        }

        // XXX destroy needed
        addressBookFactory = null;

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
            tomcatManager.stopTomcat();
        } catch (Exception exn) {
            logger.warn("could not stop tomcat", exn);
        }

        eventLogger = null;

        try {
            loggingManager.stop();
        } catch (Exception exn) {
            logger.error("could not stop LoggingManager", exn);
        }

        try {
            sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }

        try {
            cronManager.destroy();
            cronManager = null;
        } catch (Exception exn) {
            logger.warn("could not stop CronManager", exn);
        }

        try {
            if (null != this.heapMonitor) {
                this.heapMonitor.stop();
            }
        } catch (Exception exn) {
            logger.warn("unable to stop the heap monitor",exn);
        }
    }

    // package protected methods ----------------------------------------------

    boolean refreshToolbox()
    {
        return main.refreshToolbox();
    }

    SchemaUtil schemaUtil()
    {
        return main.schemaUtil();
    }

    void fatalError(String throwingLocation, Throwable x)
    {
        main.fatalError(throwingLocation, x);
    }

    void refreshSessionFactory()
    {
        synchronized (this) {
            sessionFactory = Util.makeSessionFactory(getClass().getClassLoader());
            transactionRunner = new TransactionRunner(sessionFactory);
        }
    }

    TomcatManager tomcatManager()
    {
        return tomcatManager;
    }

    MvvmRemoteContext remoteContext()
    {
        return remoteContext;
    }

    public String getActivationKey()
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

    // private methods --------------------------------------------------------

    // static initializer -----------------------------------------------------

    static {
        ACTIVATE_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mvactivate";
        ACTIVATION_KEY_FILE = System.getProperty("bunnicula.home")
            + "/activation.key";
        ARGON_FAKE_KEY = "argon.fake";
    }
}
