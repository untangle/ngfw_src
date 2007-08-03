/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.LocalBrandingManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.Period;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.argon.Argon;
import com.untangle.uvm.argon.ArgonManagerImpl;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.license.LicenseManagerFactory;
import com.untangle.uvm.license.LocalLicenseManager;
import com.untangle.uvm.license.RemoteLicenseManager;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.localapi.LocalShieldManager;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.LogMailerImpl;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.networking.RemoteNetworkManagerAdaptor;
import com.untangle.uvm.networking.ping.PingManagerImpl;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.node.RemoteShieldManager;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.PolicyManagerFactory;
import com.untangle.uvm.policy.RemotePolicyManager;
import com.untangle.uvm.portal.BasePortalManager;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.user.LocalPhoneBook;
import com.untangle.uvm.user.PhoneBookFactory;
import com.untangle.uvm.user.RemotePhoneBook;
import com.untangle.uvm.util.TransactionRunner;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.MPipeManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

/**
 * Implements LocalUvmContext.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmContextImpl extends UvmContextBase
    implements LocalUvmContext
{
    private static final UvmContextImpl CONTEXT = new UvmContextImpl();

    private static final String REBOOT_SCRIPT = "/sbin/reboot";

    private static final String ACTIVATE_SCRIPT;
    private static final String ACTIVATION_KEY_FILE;
    private static final String ARGON_FAKE_KEY;

    /* true if running in a development environment */
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";
    private static final String PROPERTY_IS_UNTANGLE_APPLIANCE = "com.untangle.isUntangleAppliance";
    private static final String PROPERTY_IS_INSIDE_VM = "com.untangle.isInsideVM";

    private final Object startupWaitLock = new Object();
    private final Logger logger = Logger.getLogger(UvmContextImpl.class);
    private final BackupManager backupManager;

    private UvmState state;
    private RemoteAdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private RemoteIntfManagerImpl remoteIntfManager;
    private LocalShieldManager localShieldManager;
    private RemoteShieldManager remoteShieldManager;
    private HttpInvokerImpl httpInvoker;
    private RemoteLoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private EventLogger eventLogger;
    private PolicyManagerFactory policyManagerFactory;
    private MPipeManagerImpl mPipeManager;
    private MailSenderImpl mailSender;
    private LogMailerImpl logMailer;
    private NetworkManagerImpl networkManager;
    private PingManagerImpl pingManager;
    private RemoteNetworkManagerAdaptor remoteNetworkManager;
    private RemoteReportingManagerImpl reportingManager;
    private RemoteConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private RemoteToolboxManagerImpl toolboxManager;
    private NodeManagerImpl nodeManager;
    private RemoteNodeManagerAdaptor remoteNodeManager;
    private RemoteUvmContext remoteContext;
    private CronManager cronManager;
    private AppServerManagerImpl appServerManager;
    private RemoteAppServerManagerAdaptor remoteAppServerManager;
    private AddressBookFactory addressBookFactory;
    private RemoteBrandingManager brandingManager;
    private LocalBrandingManager localBrandingManager;
    private PhoneBookFactory phoneBookFactory;
    private BasePortalManager portalManager;
    private LicenseManagerFactory licenseManagerFactory;
    private TomcatManager tomcatManager;
    private HeapMonitor heapMonitor;

    private volatile SessionFactory sessionFactory;
    private volatile TransactionRunner transactionRunner;

    // constructor ------------------------------------------------------------

    private UvmContextImpl()
    {
        refreshSessionFactory();

        // XXX can we load all node cl's first?
        state = UvmState.LOADED;
        backupManager = new BackupManager();
    }

    // static factory ---------------------------------------------------------

    static UvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    public static LocalUvmContext context()
    {
        return CONTEXT;
    }

    // singletons -------------------------------------------------------------

    public RemoteAddressBook appAddressBook()
    {
        return addressBookFactory.getAddressBook();
    }

    public RemoteAddressBook appRemoteAddressBook()
    {
        return addressBookFactory.getRemoteAddressBook();
    }

    public RemoteBrandingManager brandingManager()
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

    public BasePortalManager portalManager()
    {
        return portalManager;
    }

    public AppServerManagerImpl appServerManager()
    {
        return appServerManager;
    }

    RemoteAppServerManagerAdaptor remoteAppServerManager()
    {
        return remoteAppServerManager;
    }

    public RemoteToolboxManagerImpl toolboxManager()
    {
        return toolboxManager;
    }

    public NodeManagerImpl nodeManager()
    {
        return nodeManager;
    }

    public RemoteNodeManager remoteNodeManager()
    {
        return remoteNodeManager;
    }

    public RemoteLoggingManagerImpl loggingManager()
    {
        return loggingManager;
    }

    public SyslogManagerImpl syslogManager()
    {
        return syslogManager;
    }

    public LocalPolicyManager policyManager()
    {
        return policyManagerFactory.policyManager();
    }

    public RemotePolicyManager remotePolicyManager()
    {
        return policyManagerFactory.remotePolicyManager();
    }

    public MailSenderImpl mailSender()
    {
        return mailSender;
    }

    public RemoteAdminManagerImpl adminManager()
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

    RemoteNetworkManagerAdaptor remoteNetworkManager()
    {
        return remoteNetworkManager;
    }

    public RemoteReportingManagerImpl reportingManager()
    {
        return reportingManager;
    }

    public RemoteConnectivityTesterImpl getRemoteConnectivityTester()
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

    public RemoteLicenseManager remoteLicenseManager()
    {
        return this.licenseManagerFactory.getRemoteLicenseManager();
    }

    public LocalLicenseManager localLicenseManager() throws UvmException
    {
        return this.licenseManagerFactory.getLocalLicenseManager();
    }


    public MPipeManagerImpl mPipeManager()
    {
        return mPipeManager;
    }

    public PipelineFoundryImpl pipelineFoundry()
    {
        return pipelineFoundry;
    }

    public UvmLoginImpl uvmLogin()
    {
        return adminManager.uvmLogin();
    }

    public void waitForStartup()
    {
        synchronized (startupWaitLock) {
            while (state == UvmState.LOADED || state == UvmState.INITIALIZED) {
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
        return newThread(runnable, "UTThread-" + ThreadNumber.nextThreadNum());
    }

    public Thread newThread(final Runnable runnable, final String name)
    {
        Runnable task = new Runnable()
            {
                public void run()
                {
                    NodeContext tctx = null == nodeManager
                        ? null : nodeManager.threadContext();

                    if (null != tctx) {
                        nodeManager.registerThreadContext(tctx);
                    }
                    try {
                        runnable.run();
                    } catch (OutOfMemoryError exn) {
                        UvmContextImpl.getInstance().fatalError("UvmContextImpl", exn);
                    } catch (Exception exn) {
                        logger.error("Exception running: " + runnable, exn);
                    } finally {
                        if (null != tctx) {
                            nodeManager.deregisterThreadContext();
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
        newCmd[0] = System.getProperty("bunnicula.bin.dir") + "/utnice";
        System.arraycopy(cmd, 0, newCmd, 1, cmd.length);
        try {
            return Runtime.getRuntime().exec(newCmd, envp, dir);
        } catch (IOException x) {
            // Check and see if we've run out of virtual memory.  This is very ugly
            // but there's not another apparent way.  XXXXXXXXXX
            String msg = x.getMessage();
            if (msg.contains("Cannot allocate memory")) {
                logger.error("Virtual memory exhausted in Process.exec()");
                UvmContextImpl.getInstance().fatalError("UvmContextImpl.exec", x);
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

    public UvmState state()
    {
        return state;
    }

    public String version()
    {
        return com.untangle.uvm.Version.getVersion();
    }

    public String getFullVersion()
    {
        return com.untangle.uvm.Version.getFullVersion();
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

    public boolean loadRup()
    {
        return loadRup(true);
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

    public boolean isUntangleAppliance()
    {
        return Boolean.getBoolean(PROPERTY_IS_UNTANGLE_APPLIANCE);
    }

    public boolean isInsideVM()
    {
        return Boolean.getBoolean(PROPERTY_IS_INSIDE_VM);
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

    // UvmContextBase methods ------------------------------------------------

    @Override
    protected void init()
    {
        cronManager = new CronManager();
        syslogManager = SyslogManagerImpl.manager();
        UvmRepositorySelector repositorySelector = UvmRepositorySelector.selector();
        loggingManager = new RemoteLoggingManagerImpl(repositorySelector);
        loggingManager.initSchema("uvm");
        loadRup(false);
        loggingManager.start();
        eventLogger = EventLoggerFactory.factory().getEventLogger();

        // Create the tomcat manager *before* the UVM, so we can
        // "register" webapps to be started before Tomcat exists.
        tomcatManager = new TomcatManager(this,
                                          System.getProperty("bunnicula.home"),
                                          System.getProperty("bunnicula.web.dir"),
                                          System.getProperty("bunnicula.log.dir"));

        // start services:
        adminManager = new RemoteAdminManagerImpl(this);
        mailSender = MailSenderImpl.mailSender();

        logMailer = new LogMailerImpl();
        repositorySelector.setLogMailer(logMailer);

        // Fire up the policy manager.
        policyManagerFactory = PolicyManagerFactory.makeInstance();

        toolboxManager = RemoteToolboxManagerImpl.toolboxManager();

        mPipeManager = MPipeManagerImpl.manager();
        pipelineFoundry = PipelineFoundryImpl.foundry();

        // Retrieve the network settings manager.  (Kind of busted,
        // but NAT may register a listener, and thus the network
        // manager should exist.
        networkManager = NetworkManagerImpl.getInstance();
        remoteNetworkManager = new RemoteNetworkManagerAdaptor(networkManager);

        pingManager = PingManagerImpl.getInstance();

        //Start AddressBookImpl
        addressBookFactory = AddressBookFactory.makeInstance();

        localBrandingManager = new BrandingManagerImpl();
        brandingManager = new RemoteBrandingManagerAdaptor(localBrandingManager);

        phoneBookFactory = PhoneBookFactory.makeInstance();

        portalManager = findPortalManager();

        // start nodes:
        nodeManager = new NodeManagerImpl(repositorySelector);
        remoteNodeManager = new RemoteNodeManagerAdaptor(nodeManager);

        // Retrieve the reporting configuration manager
        reportingManager = RemoteReportingManagerImpl.reportingManager();

        // Retrieve the connectivity tester
        connectivityTester = RemoteConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        argonManager = ArgonManagerImpl.getInstance();

        // Create the shield managers
        localShieldManager = new LocalShieldManagerImpl();
        remoteShieldManager = new RemoteShieldManagerImpl(localShieldManager);

        appServerManager = new AppServerManagerImpl(this);
        remoteAppServerManager = new RemoteAppServerManagerAdaptor(appServerManager);

        licenseManagerFactory = LicenseManagerFactory.makeInstance();

        // start vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            Argon.getInstance().run( networkManager );
        } else {
            logger.info( "Argon not activated, using fake interfaces in the "
                         + "policy and networking manager." );
            byte interfaces[] = new byte[] { 0, 1 };
            policyManager().reconfigure(interfaces);
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

        remoteContext = new RemoteUvmContextAdaptor(this);
        state = UvmState.INITIALIZED;
    }

    @Override
    protected void postInit()
    {
        syslogManager.postInit();

        // Mailsender can now query the hostname
        mailSender.postInit();

        logger.debug("restarting nodes");
        nodeManager.init();

        logger.debug("starting HttpInvoker");
        httpInvoker.init();
        logger.debug("postInit complete");
        synchronized (startupWaitLock) {
            state = UvmState.RUNNING;
            startupWaitLock.notifyAll();
        }

        //Inform the AppServer manager that everything
        //else is started.
        appServerManager.postInit(httpInvoker);
    }

    @Override
    protected void destroy()
    {
        state = UvmState.DESTROYED;

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

        // stop nodes:
        try {
            nodeManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy NodeManager", exn);
        }
        nodeManager = null;

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
            logger.warn("could not destroy RemoteToolboxManager", exn);
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
            logger.error("could not stop RemoteLoggingManager", exn);
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

    RemoteUvmContext remoteContext()
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

    private BasePortalManager findPortalManager()
    {
        // Fire up the portal manager.
        String bpmClass = System.getProperty("uvm.portal.manager");
        if (null == bpmClass) {
            bpmClass = "com.untangle.uvm.engine.RupPortalManager";
        }
        BasePortalManager bpm = null;
        try {
            bpm = (BasePortalManager)Class.forName(bpmClass).newInstance();
        } catch (Exception exn) {
            logger.info("could not load PortalManager: " + bpmClass);
            logger.debug("unable to load PortalManager: " + bpmClass, exn);
        }

        BasePortalManager pm = null == bpm ? new DefaultPortalManager() : bpm;
        logger.info("using PortalManager: " + pm.getClass());

        return pm;
    }

    private boolean loadRup(boolean refreshManagers)
    {
        if (main.loadRup()) {
            main.schemaUtil().initSchema("settings", "rupuvm");
            loggingManager.initSchema("rupuvm");
            refreshSessionFactory();

            if (refreshManagers) {
                // Do these in same order as boot time.
                policyManagerFactory.refresh();
                addressBookFactory.refresh();
                phoneBookFactory.refresh();
                portalManager = findPortalManager();
            }

            return true;
        } else {
            return false;
        }
    }

    // static initializer -----------------------------------------------------

    static {
        ACTIVATE_SCRIPT = System.getProperty("bunnicula.bin.dir")
            + "/utactivate";
        ACTIVATION_KEY_FILE = System.getProperty("bunnicula.home")
            + "/activation.key";
        ARGON_FAKE_KEY = "argon.fake";
    }
}
