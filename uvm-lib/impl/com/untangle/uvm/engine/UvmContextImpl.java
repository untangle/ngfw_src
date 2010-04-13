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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jabsorb.JSONSerializer;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.untangle.uvm.CronJob;
import com.untangle.uvm.LocalJStoreManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalTomcatManager;
import com.untangle.uvm.Period;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.RemoteOemManager;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.RemoteNetworkManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.argon.Argon;
import com.untangle.uvm.argon.ArgonManagerImpl;
import com.untangle.uvm.benchmark.RemoteBenchmarkManager;
import com.untangle.uvm.benchmark.LocalBenchmarkManager;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.license.LicenseManagerFactory;
import com.untangle.uvm.license.LocalLicenseManager;
import com.untangle.uvm.license.RemoteLicenseManager;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LogMailerImpl;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.RemoteMessageManager;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.PolicyManagerFactory;
import com.untangle.uvm.policy.RemotePolicyManager;
import com.untangle.uvm.portal.BasePortalManager;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.servlet.ServletUtils;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;
import com.untangle.uvm.util.TransactionRunner;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implements LocalUvmContext.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmContextImpl extends UvmContextBase implements LocalUvmContext
{
    private static final UvmContextImpl CONTEXT = new UvmContextImpl();

    private static final String REBOOT_SCRIPT = "/sbin/reboot";
    private static final String SHUTDOWN_SCRIPT = "/sbin/shutdown";
    private static final String BDB_HOME = System.getProperty("uvm.db.dir");

    private static final String ACTIVATE_SCRIPT;
    private static final String ACTIVATION_KEY_FILE;
    private static final String REGISTRATION_INFO_FILE;
    private static final String POP_ID_FILE;
    private static final String ARGON_FAKE_KEY;

    /* true if running in a development environment */
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";
    private static final String PROPERTY_IS_UNTANGLE_APPLIANCE = "com.untangle.isUntangleAppliance";
    private static final String PROPERTY_IS_INSIDE_VM = "com.untangle.isInsideVM";
    private static final String PROPERTY_INSTALLTION_TYPE = "com.untangle.installationType";

    private final Object startupWaitLock = new Object();
    private final Logger logger = Logger.getLogger(UvmContextImpl.class);
    private final BackupManager backupManager;

    private UvmState state;
    private RemoteAdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private RemoteIntfManagerImpl remoteIntfManager;
    private RemoteLoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private EventLogger<LogEvent> eventLogger;
    private PolicyManagerFactory policyManagerFactory;
    private MPipeManagerImpl mPipeManager;
    private MailSenderImpl mailSender;
    private LogMailerImpl logMailer;
    private NetworkManagerImpl networkManager;
    private RemoteReportingManagerImpl reportingManager;
    private RemoteConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private UpstreamManagerImpl upstreamManager;
    private NodeManagerImpl nodeManager;
    private CronManager cronManager;
    private AppServerManagerImpl appServerManager;
    private RemoteAppServerManagerAdaptor remoteAppServerManager;
    private AddressBookFactory addressBookFactory;
    private BrandingManagerImpl brandingManager;
    private RemoteSkinManagerImpl skinManager;
    private MessageManagerImpl localMessageManager;
    private RemoteMessageManager messageManager;
    private RemoteLanguageManagerImpl languageManager;
    private BasePortalManager portalManager;
    private LicenseManagerFactory licenseManagerFactory;
    private TomcatManagerImpl tomcatManager;
    private HeapMonitor heapMonitor;
    private UploadManagerImpl uploadManager;
    private LocalJStoreManagerImpl jStoreManager;
    private LocalBenchmarkManagerImpl benchmarkManager;
    private OemManagerImpl oemManager;
    
    private Environment bdbEnvironment;

    private volatile SessionFactory sessionFactory;
    private volatile TransactionRunner transactionRunner;

    // constructor ------------------------------------------------------------

    private UvmContextImpl()
    {
        refreshSessionFactory();

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
        return this.brandingManager;
    }

    public RemoteSkinManagerImpl skinManager()
    {
        return skinManager;
    }

    public RemoteMessageManager messageManager()
    {
        return messageManager;
    }

    public LocalMessageManager localMessageManager()
    {
        return localMessageManager;
    }

    public RemoteLanguageManagerImpl languageManager()
    {
        return languageManager;
    }

    public BasePortalManager portalManager()
    {
        return portalManager;
    }

    public AppServerManagerImpl localAppServerManager()
    {
        return appServerManager;
    }

    public RemoteAppServerManager appServerManager()
    {
        return remoteAppServerManager;
    }
    
    public ToolboxManagerImpl toolboxManager()
    {
        return toolboxManager;
    }

    public UpstreamManagerImpl upstreamManager()
    {
        return upstreamManager;
    }

    public NodeManagerImpl localNodeManager()
    {
        return nodeManager;
    }

    public RemoteNodeManager nodeManager()
    {
        return nodeManager;
    }

    public RemoteLoggingManagerImpl loggingManager()
    {
        return loggingManager;
    }

    public SyslogManagerImpl syslogManager()
    {
        return syslogManager;
    }

    public LocalPolicyManager localPolicyManager()
    {
        return policyManagerFactory.policyManager();
    }

    public RemotePolicyManager policyManager()
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

    @Override
    public LocalNetworkManager localNetworkManager()
    {
        return networkManager;
    }

    @Override
    public RemoteNetworkManager networkManager()
    {
        return networkManager;
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

    public LocalIntfManager localIntfManager()
    {
        return argonManager.getIntfManager();
    }

    public RemoteLicenseManager licenseManager()
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

    public Environment getBdbEnvironment()
    {
        if (null == bdbEnvironment) {
            synchronized (this) {
                if (null == bdbEnvironment) {
                    EnvironmentConfig envCfg = new EnvironmentConfig();
                    envCfg.setAllowCreate(true);

                    Integer maxMegs = Integer.getInteger("je.maxMemory");
                    int maxMem;
                    if (maxMegs == null) {
                        maxMem = 16 * 1024 * 1024;
                        logger.warn("No je.maxMemory property, using 16MB");
                    } else {
                        maxMem = maxMegs * 1024 * 1024;
                        logger.info("Setting max bdb memory to " + maxMegs + "MB");
                    }
                    envCfg.setCacheSize(maxMem);

                    File dbHome = new File(BDB_HOME);

                    int tries = 0;
                    while (null == bdbEnvironment && 3 > tries++) {
                        try {
                            bdbEnvironment = new Environment(dbHome, envCfg);
                        } catch (DatabaseException exn) {
                            logger.warn("couldn't load environment, try: " + tries, exn);
                            for (File f : dbHome.listFiles()) {
                                f.delete();
                            }
                        }
                    }
                }
            }
        }

        return bdbEnvironment;
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
        if (logger.isInfoEnabled()) {
            String cmdStr = new String();
            for (int i = 0 ; i < cmd.length; i++) {
                cmdStr = cmdStr.concat(cmd[i] + " ");
            }
            logger.info("System.exec(" + cmdStr + ")");
        }
        try {
            return Runtime.getRuntime().exec(cmd, envp, dir);
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
                        Thread.sleep(200);
                    } catch (InterruptedException exn) {
                        
                    }
                    logger.info("thank you for choosing uvm");
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
            logger.error("Exception during reboot");
        }
    }

    public void shutdownBox() {
        try {
            Process p = exec(new String[] { SHUTDOWN_SCRIPT , "-h" , "now" });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to shutdown (" + exitValue + ")");
            } else {
                logger.info("Shutdown at admin request");
            }
        } catch (InterruptedException exn) {
            logger.error("Interrupted during shutdown");
        } catch (IOException exn) {
            logger.error("Exception during shutdown");
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

    public void syncConfigFiles()
    {
        // Here it would be nice if we had a list of managers.  Then we could
        // just go through the list, testing 'instanceof HasConfigFiles'. XXX 
        adminManager.syncConfigFiles();
        mailSender.syncConfigFiles();
    }

    public byte[] createBackup() throws IOException {
        return backupManager.createBackup();
    }

    public void restoreBackup(byte[] backupBytes)
    throws IOException, IllegalArgumentException {
        backupManager.restoreBackup(backupBytes);
    }

    public void restoreBackup(String fileName)
    throws IOException, IllegalArgumentException {
        backupManager.restoreBackup(fileName);
    }

    public boolean loadRup()
    {
        return loadRup(true);
    }

    public void loadPortalManager()
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
        }

        this.portalManager = null == bpm ? new DefaultPortalManager() : bpm;
        logger.info("using PortalManager: " + this.portalManager.getClass());
    }

    public boolean isActivated() {
        // This is ez since we aren't concerned about local box
        // security -- the key is ultimately checked on the release
        // webserver, which is what matters.
        File keyFile = new File(ACTIVATION_KEY_FILE);
        return keyFile.exists();
    }

    public boolean isRegistered() {
        File regFile = new File(REGISTRATION_INFO_FILE);
        return regFile.exists();
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

    public String installationType()
    {
        return System.getProperty(PROPERTY_INSTALLTION_TYPE, "iso");
    }

    public boolean activate(String key, RegistrationInfo regInfo) {
        if (key != null) {
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
        }
        try {
            Process p;
            if (key == null)
                p = exec(new String[] { ACTIVATE_SCRIPT });
            else
                p = exec(new String[] { ACTIVATE_SCRIPT, key });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to activate (" + exitValue
                        + ") with key: " + key);
                return false;
            } else {
                logger.info("Product activated with key: " + key);
            }
        } catch (InterruptedException exn) {
            logger.error("Interrupted during activation with key: " + key);
            return false;
        } catch (IOException exn) {
            logger.error("Exception during activation with key: " + key, exn);
            return false;
        }

        // Only register if activation succeeded
        try {
            /* If possible, update the admin email */
            adminManager.setAdminEmail(regInfo);
            adminManager.setRegistrationInfo(regInfo);
        } catch (Exception x) {
            // Shouldn't happen
            logger.error("unable to set reg info", x);
        }
        return true;
    }

    public void doFullGC()
    {
        // XXX check access permission
        System.gc();
    }

    public EventLogger<LogEvent> eventLogger()
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

    public String setProperty(String key, String value)
    {
        return System.setProperty(key, value);
    }

    public Map<String, String> getTranslations(String module)
    {
        return languageManager.getTranslations(module);
    }
    
    public String getCompanyName(){
        return this.brandingManager.getCompanyName();
    }

    // UvmContextBase methods --------------------------------------------------

    @Override
    protected void init()
    {
        this.oemManager = new OemManagerImpl();

        this.uploadManager = new UploadManagerImpl();
        
        this.jStoreManager = new LocalJStoreManagerImpl();
        
        this.benchmarkManager = new LocalBenchmarkManagerImpl();
        
        JSONSerializer serializer = new JSONSerializer();
        try {
            ServletUtils.getInstance().registerSerializers(serializer);
            jStoreManager.setSerializer(serializer);
        } catch (Exception e) {
            throw new IllegalStateException("register serializers should never fail!", e);
        }
        
        String jStorePath = System.getProperty( "uvm.conf.dir" ) + "/jStore";
        jStoreManager.setBasePath(jStorePath);
        
        uploadManager.registerHandler(new RestoreUploadHandler());

        this.cronManager = new CronManager();

        this.syslogManager = SyslogManagerImpl.manager();
        
        UvmRepositorySelector repositorySelector = UvmRepositorySelector.selector();

        if (!testHibernateConnection()) {
            fatalError("Can not connect to database. Is postgres running?", null);
        }

        this.loggingManager = new RemoteLoggingManagerImpl(repositorySelector);
        loggingManager.initSchema("uvm");
        loadRup(false);
        loggingManager.start();

        this.eventLogger = EventLoggerFactory.factory().getEventLogger();

        InheritableThreadLocal<HttpServletRequest> threadRequest = new InheritableThreadLocal<HttpServletRequest>();

        this.tomcatManager = new TomcatManagerImpl(this, threadRequest,
                                                   System.getProperty("uvm.home"),
                                                   System.getProperty("uvm.web.dir"),
                                                   System.getProperty("uvm.log.dir"));

        // start services:
        this.adminManager = new RemoteAdminManagerImpl(this, threadRequest);

        this.mailSender = MailSenderImpl.mailSender();

        this.logMailer = new LogMailerImpl();

        repositorySelector.setLogMailer(logMailer);

        // Fire up the policy manager.
        this.policyManagerFactory = PolicyManagerFactory.makeInstance();

        this.toolboxManager = ToolboxManagerImpl.toolboxManager();

        this.upstreamManager = UpstreamManagerImpl.upstreamManager();

        // Now that upstreamManager is alive, we can get the upgrade settings and
        // start the cron job
        this.toolboxManager.start();

        this.mPipeManager = MPipeManagerImpl.manager();
        this.pipelineFoundry = PipelineFoundryImpl.foundry();

        // Retrieve the network settings manager.  (Kind of busted,
        // but NAT may register a listener, and thus the network
        // manager should exist.
        this.networkManager = NetworkManagerImpl.getInstance();

        //Start AddressBookImpl
        this.addressBookFactory = AddressBookFactory.makeInstance();

        this.brandingManager = new BrandingManagerImpl();

        //Skins and Language managers
        this.skinManager = new RemoteSkinManagerImpl(this);
        this.languageManager = new RemoteLanguageManagerImpl(this);

        loadPortalManager();

        // start nodes:
        this.nodeManager = new NodeManagerImpl(repositorySelector);

        this.localMessageManager = new MessageManagerImpl();
        this.messageManager = new RemoteMessageManagerAdaptor(localMessageManager);

        // Retrieve the reporting configuration manager
        this.reportingManager = RemoteReportingManagerImpl.reportingManager();

        // Retrieve the connectivity tester
        this.connectivityTester = RemoteConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        this.argonManager = ArgonManagerImpl.getInstance();

        this.appServerManager = new AppServerManagerImpl(this);
        this.remoteAppServerManager = new RemoteAppServerManagerAdaptor(appServerManager);
        
        this.licenseManagerFactory = LicenseManagerFactory.makeInstance();
        
        // start vectoring:
        String argonFake = System.getProperty(ARGON_FAKE_KEY);
        if (null == argonFake || !argonFake.equalsIgnoreCase("yes")) {
            Argon.getInstance().run( networkManager );
        }

        // Start statistic gathering
        localMessageManager.start();

        this.heapMonitor = new HeapMonitor();
        String useHeapMonitor = System.getProperty(HeapMonitor.KEY_ENABLE_MONITOR);
        if (null != useHeapMonitor && Boolean.valueOf(useHeapMonitor)) {
            this.heapMonitor.start();
        }

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
        pipelineFoundry.clearChains();

        logger.debug("postInit complete");
        synchronized (startupWaitLock) {
            state = UvmState.RUNNING;
            startupWaitLock.notifyAll();
        }
        
        /* Reload Apache */
        tomcatManager.setRootWelcome(tomcatManager.getRootWelcome());

        //Inform the AppServer manager that everything
        //else is started.
        appServerManager.postInit();
    }

    @Override
    protected void destroy()
    {
        state = UvmState.DESTROYED;

        if (localMessageManager != null)
            localMessageManager.stop();

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

    RemoteAppServerManagerAdaptor remoteAppServerManager()
    {
        return remoteAppServerManager;
    }

    public RemoteIntfManager intfManager()
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

    boolean refreshToolbox()
    {
        boolean result = main.refreshToolbox();
        upstreamManager.refresh();
        return result;
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

    public LocalTomcatManager tomcatManager()
    {
        return tomcatManager;
    }

    boolean loadUvmResource(String name)
    {
        return main.loadUvmResource(name);
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

    public String getPopID()
    {
        try {
            File keyFile = new File(POP_ID_FILE);
            if (keyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                return reader.readLine();
            }
        } catch (IOException x) {
            logger.error("Unable to get pop id: ", x);
        }
        return null;
    }
    
    @Override
    public UploadManager uploadManager()
    {
        return this.uploadManager;
    }
    
    @Override 
    public LocalJStoreManager jStoreManager()
    {
        return this.jStoreManager;
    }

    @Override
    public LocalBenchmarkManager localBenchmarkManager()
    {
        return this.benchmarkManager;
    }

    @Override
    public RemoteBenchmarkManager benchmarkManager()
    {
        return this.benchmarkManager;
    }
    
    @Override
    public RemoteOemManager oemManager()
    {
        return this.oemManager;
    }

    // private methods --------------------------------------------------------

    private boolean loadRup(boolean refreshManagers)
    {
        main.loadRup();

        refreshSessionFactory();

        if ( refreshManagers) {
            // Do these in same order as boot time.
            policyManagerFactory.refresh();
            addressBookFactory.refresh();
        }

        return true;
    }

    private boolean testHibernateConnection()
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    org.hibernate.SQLQuery q = s.createSQLQuery("select 1");
                    Object o = q.uniqueResult();

                    if (null != o)
                        return true;
                    else
                        return false;
                }
            };

        return context().runTransaction(tw);
    }
    
    private class RestoreUploadHandler implements UploadHandler
    {

        @Override
        public String getName() {
            return "restore";
        }

        @Override
        public String handleFile(FileItem fileItem) throws Exception {
            byte[] backupFileBytes=fileItem.get();
            restoreBackup(backupFileBytes);
            return "restored backup file.";
        }
        
    }

    // static initializer -----------------------------------------------------

    static {
        ACTIVATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/utactivate";
        ACTIVATION_KEY_FILE = System.getProperty("uvm.home") + "/activation.key";
        REGISTRATION_INFO_FILE = System.getProperty("uvm.home") + "/registration.info";
        POP_ID_FILE = System.getProperty("uvm.home") + "/popid";
        ARGON_FAKE_KEY = "argon.fake";
    }
}
