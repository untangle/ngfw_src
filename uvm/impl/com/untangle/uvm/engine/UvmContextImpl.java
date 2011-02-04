/* $HeadURL$ */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalTomcatManager;
import com.untangle.uvm.Period;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.RemoteOemManager;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.argon.Argon;
import com.untangle.uvm.argon.ArgonManagerImpl;
import com.untangle.uvm.license.LicenseManagerFactory;
import com.untangle.uvm.license.LicenseManager;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LogMailerImpl;
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.RemoteMessageManager;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.policy.PolicyManagerFactory;
import com.untangle.uvm.RegistrationInfo;
import com.untangle.uvm.servlet.ServletUtils;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;
import com.untangle.uvm.util.TransactionRunner;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.util.JsonClient;

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
    private static final String REGISTRATION_INFO_FILE;
    private static final String UID_FILE;
    private static String uid;

    /* true if running in a development environment */
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";
    private static final String PROPERTY_IS_INSIDE_VM = "com.untangle.isInsideVM";

    private static final String FACTORY_DEFAULT_FLAG = System.getProperty("uvm.conf.dir") + "/factory-defaults";
    
    private final Object startupWaitLock = new Object();
    private final Logger logger = Logger.getLogger(UvmContextImpl.class);
    private final BackupManager backupManager;

    private UvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private RemoteLoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private EventLogger<LogEvent> eventLogger;
    private PolicyManagerFactory policyManagerFactory;
    private MailSenderImpl mailSender;
    private LogMailerImpl logMailer;
    private NetworkManagerImpl networkManager;
    private RemoteReportingManagerImpl reportingManager;
    private RemoteConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private NodeManagerImpl nodeManager;
    private CronManager cronManager;
    private AppServerManagerImpl appServerManager;
    private RemoteAppServerManagerAdaptor remoteAppServerManager;
    private AddressBookFactory addressBookFactory;
    private BrandingManagerImpl brandingManager;
    private SkinManagerImpl skinManager;
    private MessageManagerImpl localMessageManager;
    private RemoteMessageManager messageManager;
    private RemoteLanguageManagerImpl languageManager;
    private LicenseManagerFactory licenseManagerFactory;
    private TomcatManagerImpl tomcatManager;
    private HeapMonitor heapMonitor;
    private UploadManagerImpl uploadManager;
    private SettingsManagerImpl settingsManager;
    private OemManagerImpl oemManager;
    private SessionMonitorImpl sessionMonitor;
    
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
        return this.addressBookFactory.getAddressBook();
    }

    public RemoteAddressBook appRemoteAddressBook()
    {
        return this.addressBookFactory.getRemoteAddressBook();
    }

    public RemoteBrandingManager brandingManager()
    {
        return this.brandingManager;
    }

    public SkinManagerImpl skinManager()
    {
        return this.skinManager;
    }

    public RemoteMessageManager messageManager()
    {
        return this.messageManager;
    }

    public LocalMessageManager localMessageManager()
    {
        return this.localMessageManager;
    }

    public RemoteLanguageManagerImpl languageManager()
    {
        return this.languageManager;
    }

    public AppServerManagerImpl localAppServerManager()
    {
        return this.appServerManager;
    }

    public RemoteAppServerManager appServerManager()
    {
        return this.remoteAppServerManager;
    }
    
    public ToolboxManagerImpl toolboxManager()
    {
        return this.toolboxManager;
    }

    public NodeManager nodeManager()
    {
        return this.nodeManager;
    }

    public RemoteLoggingManagerImpl loggingManager()
    {
        return this.loggingManager;
    }

    public SyslogManagerImpl syslogManager()
    {
        return this.syslogManager;
    }

    public PolicyManager policyManager()
    {
        return this.policyManagerFactory.policyManager();
    }

    public MailSenderImpl mailSender()
    {
        return this.mailSender;
    }

    public AdminManagerImpl adminManager()
    {
        return this.adminManager;
    }

    @Override
    public NetworkManager networkManager()
    {
        return this.networkManager;
    }

    public RemoteReportingManagerImpl reportingManager()
    {
        return this.reportingManager;
    }

    public RemoteConnectivityTesterImpl getRemoteConnectivityTester()
    {
        return this.connectivityTester;
    }

    public ArgonManagerImpl argonManager()
    {
        return this.argonManager;
    }

    public LicenseManager licenseManager()
    {
        return this.licenseManagerFactory.getLicenseManager();
    }

    public PipelineFoundryImpl pipelineFoundry()
    {
        return this.pipelineFoundry;
    }

    public SessionMonitor sessionMonitor()
    {
        return this.sessionMonitor;
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

    public boolean runTransaction(TransactionWork<?> tw)
    {
        return transactionRunner.runTransaction(tw);
    }

    /* For autonumbering anonymous threads. */
    private static class ThreadNumber
    {
        private static int threadInitNumber = 1;

        public static synchronized int nextThreadNum()
        {
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

    public void syncConfigFiles()
    {
        // Here it would be nice if we had a list of managers.  Then we could
        // just go through the list, testing 'instanceof HasConfigFiles'. XXX 
        adminManager.syncConfigFiles();
        mailSender.syncConfigFiles();
    }

    public byte[] createBackup() throws IOException
    {
        return backupManager.createBackup();
    }

    public void restoreBackup(byte[] backupBytes)
    throws IOException, IllegalArgumentException
    {
        backupManager.restoreBackup(backupBytes);
    }

    public void restoreBackup(String fileName)
    throws IOException, IllegalArgumentException
    {
        backupManager.restoreBackup(fileName);
    }

    public boolean isActivated()
    {
        File keyFile = new File(UID_FILE);
        return keyFile.exists();
    }

    public boolean isRegistered()
    {
        File regFile = new File(REGISTRATION_INFO_FILE);
        return regFile.exists();
    }

    public boolean isDevel()
    {
        return Boolean.getBoolean(PROPERTY_IS_DEVEL);
    }

    public boolean isInsideVM()
    {
        return Boolean.getBoolean(PROPERTY_IS_INSIDE_VM);
    }

    public boolean activate(String uid, RegistrationInfo regInfo)
    {
        if (uid != null) {
            // Be nice to the poor user:
            if (uid.length() == 16)
                uid = uid.substring(0, 4) + "-" + uid.substring(4, 8) + "-" +
                uid.substring(8, 12) + "-" + uid.substring(12,16);
            // Fix for bug 1310: Make sure all the hex chars are lower cased.
            uid = uid.toLowerCase();
            if (uid.length() != 19) {
                // Don't even bother if the uid isn't the right length.
                // Could do other sanity checking here as well. XX
                logger.error("Unable to activate with wrong length uid: " + uid);
                return false;
            }
        }
        try {
            Process p;
            if (uid == null)
                p = exec(new String[] { ACTIVATE_SCRIPT });
            else
                p = exec(new String[] { ACTIVATE_SCRIPT, uid });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                logger.error("Unable to activate (" + exitValue
                        + ") with uid: " + uid);
                return false;
            } else {
                logger.info("Product activated with uid: " + uid);
            }
        } catch (InterruptedException exn) {
            logger.error("Interrupted during activation with uid: " + uid);
            return false;
        } catch (IOException exn) {
            logger.error("Exception during activation with uid: " + uid, exn);
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
        
        this.settingsManager = new SettingsManagerImpl();
        
        this.sessionMonitor = new SessionMonitorImpl();
        
        JSONSerializer serializer = new JSONSerializer();
        serializer.setFixupDuplicates(false);
        try {
            ServletUtils.getInstance().registerSerializers(serializer);
            settingsManager.setSerializer(serializer);
            sessionMonitor.setSerializer(serializer);
        } catch (Exception e) {
            throw new IllegalStateException("register serializers should never fail!", e);
        }
        
        uploadManager.registerHandler(new RestoreUploadHandler());

        this.cronManager = new CronManager();

        this.syslogManager = SyslogManagerImpl.manager();
        
        UvmRepositorySelector repositorySelector = UvmRepositorySelector.selector();

        if (!testHibernateConnection()) {
            fatalError("Can not connect to database. Is postgres running?", null);
        }

        this.loggingManager = new RemoteLoggingManagerImpl(repositorySelector);
        loggingManager.initSchema("uvm");
        loggingManager.start();

        this.eventLogger = EventLoggerFactory.factory().getEventLogger();

        InheritableThreadLocal<HttpServletRequest> threadRequest = new InheritableThreadLocal<HttpServletRequest>();

        this.tomcatManager = new TomcatManagerImpl(this, threadRequest,
                                                   System.getProperty("uvm.home"),
                                                   System.getProperty("uvm.web.dir"),
                                                   System.getProperty("uvm.log.dir"));

        // start services:
        this.adminManager = new AdminManagerImpl(this, threadRequest);

        // initialize the network Manager
        this.networkManager = NetworkManagerImpl.getInstance();

        this.mailSender = MailSenderImpl.mailSender();

        this.logMailer = new LogMailerImpl();

        repositorySelector.setLogMailer(logMailer);

        // Fire up the policy manager.
        this.policyManagerFactory = PolicyManagerFactory.makeInstance();

        this.toolboxManager = ToolboxManagerImpl.toolboxManager();
        this.toolboxManager.start();

        this.pipelineFoundry = PipelineFoundryImpl.foundry();

        //Start AddressBookImpl
        this.addressBookFactory = AddressBookFactory.makeInstance();

        this.brandingManager = new BrandingManagerImpl();

        //Skins and Language managers
        this.skinManager = new SkinManagerImpl(this);
        this.languageManager = new RemoteLanguageManagerImpl(this);

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
        Argon.getInstance().run( networkManager );

        // Start statistic gathering
        localMessageManager.start();

        this.heapMonitor = new HeapMonitor();
        String useHeapMonitor = System.getProperty(HeapMonitor.KEY_ENABLE_MONITOR);
        if (null != useHeapMonitor && Boolean.valueOf(useHeapMonitor)) {
            this.heapMonitor.start();
        }

        if (isFactoryDefaults())
            initializeWizard();
        
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
        try {
            Argon.getInstance().destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy Argon", exn);
        }


        // stop nodes:
        try {
            nodeManager.destroy();
        } catch (Exception exn) {
            logger.warn("could not destroy NodeManager", exn);
        }
        nodeManager = null;

        // XXX destroy needed
        addressBookFactory = null;

        // XXX destroy methods for:
        // - pipelineFoundry
        // - networkingManager
        // - reportingManager
        // - connectivityTester (Doesn't really need one)
        // - argonManager

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

    boolean refreshToolbox()
    {
        boolean result = main.refreshToolbox();
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

    public void refreshPolicyManager()
    {
        synchronized (this) {
            this.policyManagerFactory.refresh();
            this.addressBookFactory.refresh();
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

    public String getServerUID()
    {
        if (this.uid == null) {
            try {
                File keyFile = new File(UID_FILE);
                if (keyFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                    this.uid = reader.readLine();
                    return this.uid;
                }
            } catch (IOException x) {
                logger.error("Unable to get pop id: ", x);
            }
        }
        return this.uid;
    }
    
    @Override
    public UploadManager uploadManager()
    {
        return this.uploadManager;
    }
    
    @Override 
    public SettingsManager settingsManager()
    {
        return this.settingsManager;
    }

    @Override
    public RemoteOemManager oemManager()
    {
        return this.oemManager;
    }

    // private methods --------------------------------------------------------

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

    private boolean isFactoryDefaults()
    {
        return (new File(FACTORY_DEFAULT_FLAG)).exists(); 
    }

    private void initializeWizard()
    {
        /**
         * Tell alpaca to initialize wizard settings
         */
        try {
            JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "wizard_start", null );
        } catch (Exception exc) {
            logger.error("Failed to initialize Factory Defaults",exc);
        }

        /**
         * Remove the flag after setting initial settings
         */
        File f = new File(FACTORY_DEFAULT_FLAG);
        if (f.exists()) {
            f.delete();
        }
            
    }
    
    // static initializer -----------------------------------------------------

    static {
        ACTIVATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-activate";
        REGISTRATION_INFO_FILE = System.getProperty("uvm.conf.dir") + "/registration.info";
        UID_FILE = System.getProperty("uvm.conf.dir") + "/uid";
    }
}
