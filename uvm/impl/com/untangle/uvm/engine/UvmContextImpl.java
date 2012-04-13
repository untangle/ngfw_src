/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.sql.SQLException;
import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import org.jabsorb.JSONSerializer;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.LocalTomcatManager;
import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.Period;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.OemManager;
import com.untangle.uvm.AlertManager;
import com.untangle.uvm.AppServerManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.argon.Argon;
import com.untangle.uvm.argon.ArgonManagerImpl;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.networking.NetworkManagerImpl;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.servlet.ServletUtils;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;
import com.untangle.uvm.util.TransactionRunner;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.util.JsonClient;

/**
 * Implements UvmContext.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmContextImpl extends UvmContextBase implements UvmContext
{
    private static final UvmContextImpl CONTEXT = new UvmContextImpl();

    private static final String REBOOT_SCRIPT = "/sbin/reboot";
    private static final String SHUTDOWN_SCRIPT = "/sbin/shutdown";
    private static final String TIMESYNC_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-force-time-sync";
    private static final String UPGRADE_PID_FILE = "/var/run/uvm-upgrade.pid";
    private static final String UPGRADE_HTML_FILE = "/var/www/uvm-upgrade.html";
    private static final String UPGRADE_SPLASH_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-show-upgrade-splash";;

    private static final String CREATE_UID_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-createUID";
    private static final String UID_FILE = System.getProperty("uvm.conf.dir") + "/uid";
    private static final String WIZARD_COMPLETE_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/wizard-complete-flag";

    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel"; /* devel Env */

    private static final String FACTORY_DEFAULT_FLAG = System.getProperty("uvm.conf.dir") + "/factory-defaults";
    
    private static final Object startupWaitLock = new Object();

    private final Logger logger = Logger.getLogger(UvmContextImpl.class);

    private static String uid;
    
    private UvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private LoggingManagerImpl loggingManager;
    private SyslogManagerImpl syslogManager;
    private MailSenderImpl mailSender;
    private NetworkManagerImpl networkManager;
    private ReportingManagerImpl reportingManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private NodeManagerImpl nodeManager;
    private CronManager cronManager;
    private AppServerManagerImpl appServerManager;
    private BrandingManagerImpl brandingManager;
    private SkinManagerImpl skinManager;
    private MessageManagerImpl messageManager;
    private LanguageManagerImpl languageManager;
    private DefaultLicenseManagerImpl defaultLicenseManager;
    private TomcatManagerImpl tomcatManager;
    private HeapMonitor heapMonitor;
    private UploadManagerImpl uploadManager;
    private SettingsManagerImpl settingsManager;
    private OemManagerImpl oemManager;
    private AlertManagerImpl alertManager;
    private SessionMonitorImpl sessionMonitor;
    private BackupManager backupManager;
    private LocalDirectoryImpl localDirectory;
    private ExecManagerImpl execManager;
    private JSONSerializer serializer;
    
    private volatile boolean sessionFactoryNeedsRebuild = true;
    private volatile SessionFactory sessionFactory;
    private volatile TransactionRunner transactionRunner;
    private volatile List<String> annotatedClasses = new LinkedList<String>();
    
    // constructor ------------------------------------------------------------

    private UvmContextImpl()
    {
        initializeUvmAnnotatedClasses();
        refreshSessionFactory();

        state = UvmState.LOADED;
    }

    // static factory ---------------------------------------------------------

    static UvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    public static UvmContext context()
    {
        return CONTEXT;
    }

    // singletons -------------------------------------------------------------

    public LocalDirectory localDirectory()
    {
        return this.localDirectory;
    }

    public BrandingManager brandingManager()
    {
        return this.brandingManager;
    }

    public SkinManagerImpl skinManager()
    {
        return this.skinManager;
    }

    public MessageManager messageManager()
    {
        return this.messageManager;
    }

    public LanguageManagerImpl languageManager()
    {
        return this.languageManager;
    }

    public AppServerManagerImpl localAppServerManager()
    {
        return this.appServerManager;
    }

    public AppServerManager appServerManager()
    {
        return this.appServerManager;
    }
    
    public ToolboxManagerImpl toolboxManager()
    {
        return this.toolboxManager;
    }

    public NodeManager nodeManager()
    {
        return this.nodeManager;
    }

    public LoggingManagerImpl loggingManager()
    {
        return this.loggingManager;
    }

    public SyslogManagerImpl syslogManager()
    {
        return this.syslogManager;
    }

    public MailSenderImpl mailSender()
    {
        return this.mailSender;
    }

    public AdminManagerImpl adminManager()
    {
        return this.adminManager;
    }

    public NetworkManager networkManager()
    {
        return this.networkManager;
    }

    public ReportingManagerImpl reportingManager()
    {
        return this.reportingManager;
    }

    public ConnectivityTesterImpl getConnectivityTester()
    {
        return this.connectivityTester;
    }

    public ArgonManagerImpl argonManager()
    {
        return this.argonManager;
    }

    public LicenseManager licenseManager()
    {
        LicenseManager lm = (LicenseManager)this.nodeManager().node("untangle-node-license");

        if (lm == null)
            return this.defaultLicenseManager;
        else
            return lm;
    }

    public PipelineFoundryImpl pipelineFoundry()
    {
        return this.pipelineFoundry;
    }

    public SessionMonitor sessionMonitor()
    {
        return this.sessionMonitor;
    }

    public ExecManager execManager()
    {
        return this.execManager;
    }

    public ExecManager createExecManager()
    {
        ExecManagerImpl execManager = new ExecManagerImpl();
        execManager.setSerializer(serializer);
        return execManager;
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

    public Session makeHibernateSession()
    {
        synchronized(this) {
            if (this.sessionFactoryNeedsRebuild == true) {
                this.sessionFactory = this.makeSessionFactory(getClass().getClassLoader());
                this.transactionRunner = new TransactionRunner(sessionFactory);
                this.sessionFactoryNeedsRebuild = false;
            }
        }

        return this.sessionFactory.openSession();

    }
    
    public boolean runTransaction(TransactionWork<?> tw)
    {
        synchronized(this) {
            if (this.sessionFactoryNeedsRebuild == true) {
                this.sessionFactory = this.makeSessionFactory(getClass().getClassLoader());
                this.transactionRunner = new TransactionRunner(sessionFactory);
                this.sessionFactoryNeedsRebuild = false;
            }
        }
        
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
                    // XXX is this necessary - shouldn't it be inherited?
                    // comment out this region to see
                    // ...
                    
                    //                     NodeContext nodeContext = (null == nodeManager ? null : nodeManager.threadContext());
                    //                     if (nodeContext != null) {
                    //                         LoggingInformation logInfo = new LoggingInformation("log4j-node.xml", nodeContext.getNodeSettings().getId().toString());
                    //                         UvmRepositorySelector.instance().setThreadLoggingInformation(logInfo);
                    //                     }


                    try {
                        runnable.run();
                    } catch (OutOfMemoryError exn) {
                        UvmContextImpl.getInstance().fatalError("UvmContextImpl", exn);
                    } catch (Exception exn) {
                        logger.error("Exception running: " + runnable, exn);
                    } finally {
                        //                         if (nodeContext != null) {
                        //                             LoggingInformation logInfo = new LoggingInformation("log4j-uvm.xml", "uvm" );
                        //                             UvmRepositorySelector.instance().setThreadLoggingInformation(logInfo);
                        //                         }
                    }
                }
            };
        return new Thread(task, name);
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

    public void rebootBox()
    {
        Integer exitValue = this.execManager().execResult( REBOOT_SCRIPT );
        if (0 != exitValue) {
            logger.error("Unable to reboot (" + exitValue + ")");
        } else {
            logger.info("Rebooted at admin request");
        }
    }

    public void shutdownBox()
    {
        Integer exitValue = this.execManager().execResult( SHUTDOWN_SCRIPT + " -h now" );
        if (0 != exitValue) {
            logger.error("Unable to shutdown (" + exitValue + ")");
        } else {
            logger.info("Shutdown at admin request");
        }
    }

    public int forceTimeSync()
    {
        Integer exitValue = this.execManager().execResult( TIMESYNC_SCRIPT );
        if (0 != exitValue) {
            logger.error("Unable to synchronize time (" + exitValue + ")");
        } else {
            logger.info("Synchronized time");
        }
        return exitValue;
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

    public boolean isWizardComplete()
    {
        File keyFile = new File(WIZARD_COMPLETE_FLAG_FILE);
        return keyFile.exists();
    }

    public boolean isDevel()
    {
        return Boolean.getBoolean(PROPERTY_IS_DEVEL);
    }

    public void wizardComplete()
    {
        File wizardCompleteFlagFile = new File(WIZARD_COMPLETE_FLAG_FILE);

        try {
            if (wizardCompleteFlagFile.exists())
                return;
            else
                wizardCompleteFlagFile.createNewFile();
        } catch (Exception e) {
            logger.error("Unable to create wizard complete flag",e);
        }
            
    }

    public boolean createUID()
    {
        Integer exitValue = this.execManager().execResult(CREATE_UID_SCRIPT);
        if (0 != exitValue) {
            logger.error("Unable to activate (" + exitValue + ")");
            return false;
        } else {
            logger.info("UID Created.");
        }

        return true;
    }
    
    public void doFullGC()
    {
        System.gc();
    }

    public CronJob makeCronJob(Period p, Runnable r)
    {
        return cronManager.makeCronJob(p, r);
    }

    public void loadLibrary(String libname)
    {
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

    public boolean addAnnotatedClass(String className)
    {
        if (className == null) {
            logger.warn("Invalid argument: className is null");
            return false;
        }

        for (String cname : this.annotatedClasses) {
            if (className.equals(cname))
                return false; /* already in list */
        }

        logger.info("Adding AnnotatedClass: " + className);
        this.annotatedClasses.add(className);
        return true;
    }
    
    public void logEvent(LogEvent evt)
    {
        this.loggingManager.logEvent(evt);
    }

    // UvmContextBase methods --------------------------------------------------

    @Override
    protected void init()
    {
        this.execManager = new ExecManagerImpl();

        this.oemManager = new OemManagerImpl();

        this.backupManager = new BackupManager();
        
        this.uploadManager = new UploadManagerImpl();
        
        this.settingsManager = new SettingsManagerImpl();
        
        this.sessionMonitor = new SessionMonitorImpl();
        
        this.serializer = new JSONSerializer();
        serializer.setFixupDuplicates(false);
        serializer.setMarshallNullAttributes(false);
        try {
            ServletUtils.getInstance().registerSerializers(serializer);
            settingsManager.setSerializer(serializer);
            sessionMonitor.setSerializer(serializer);
            execManager.setSerializer(serializer);
        } catch (Exception e) {
            throw new IllegalStateException("register serializers should never fail!", e);
        }
        
        uploadManager.registerHandler(new RestoreUploadHandler());

        this.cronManager = new CronManager();

        this.syslogManager = SyslogManagerImpl.manager();
        
        if (!testHibernateConnection()) {
            fatalError("Can not connect to database. Is postgres running?", null);
        }

        this.loggingManager = new LoggingManagerImpl();
        loggingManager.initSchema("uvm");

        InheritableThreadLocal<HttpServletRequest> threadRequest = new InheritableThreadLocal<HttpServletRequest>();

        this.tomcatManager = new TomcatManagerImpl(this, threadRequest,
                                                   System.getProperty("uvm.home"),
                                                   System.getProperty("uvm.web.dir"),
                                                   System.getProperty("uvm.log.dir"));

        // start services:
        this.adminManager = new AdminManagerImpl(this, threadRequest);

        // initialize the network Manager
        this.networkManager = NetworkManagerImpl.getInstance();

        this.defaultLicenseManager = new DefaultLicenseManagerImpl();

        this.mailSender = MailSenderImpl.mailSender();

        this.toolboxManager = ToolboxManagerImpl.toolboxManager();
        this.toolboxManager.start();

        this.pipelineFoundry = PipelineFoundryImpl.foundry();

        this.localDirectory = new LocalDirectoryImpl();
        
        this.brandingManager = new BrandingManagerImpl();

        //Skins and Language managers
        this.skinManager = new SkinManagerImpl(this);
        this.languageManager = new LanguageManagerImpl(this);

        // start nodes:
        this.nodeManager = new NodeManagerImpl();

        this.messageManager = new MessageManagerImpl();

        // Retrieve the reporting configuration manager
        this.reportingManager = ReportingManagerImpl.reportingManager();

        // Retrieve the connectivity tester
        this.connectivityTester = ConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        this.argonManager = ArgonManagerImpl.getInstance();

        this.appServerManager = new AppServerManagerImpl(this);

        this.alertManager = new AlertManagerImpl();
        
        // start vectoring:
        Argon.getInstance().run( networkManager );

        // Start statistic gathering
        messageManager.start();

        this.heapMonitor = new HeapMonitor();
        String useHeapMonitor = System.getProperty(HeapMonitor.KEY_ENABLE_MONITOR);
        if (null != useHeapMonitor && Boolean.valueOf(useHeapMonitor)) {
            this.heapMonitor.start();
        }

        if (isFactoryDefaults())
            initializeWizard();

        hideUpgradeSplash();
        
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

        if (messageManager != null)
            messageManager.stop();

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

        // XXX destroy methods for:
        // - pipelineFoundry
        // - networkingManager
        // - reportingManager
        // - connectivityTester (Doesn't really need one)
        // - argonManager

        // stop services:
        try {
            if (toolboxManager != null)
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

        try {
            if (loggingManager != null)
                loggingManager = null;
        } catch (Exception exn) {
            logger.error("could not stop LoggingManager", exn);
        }

        try {
            if (sessionFactory != null)
                sessionFactory.close();
        } catch (HibernateException exn) {
            logger.warn("could not close Hibernate SessionFactory", exn);
        }

        try {
            if (cronManager != null)
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

    AppServerManager remoteAppServerManager()
    {
        return appServerManager;
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
        /**
         * This no longer immediately rebuilds the sessionFactory and transaction Runner.
         * It sets a flag that the sessionFactory needs to be rebuilt and next time
         * the transactionRunner is used this flag is checked and it is rebuilt at that time if necessary.
         *
         * Rebuilding the session factory is extremely slow and lazily rebuilding it saves many rebuilds
         * and reduces startup time significantly.
         */
        synchronized (this) {
            this.sessionFactoryNeedsRebuild = true;
            //this.sessionFactory = this.makeSessionFactory(getClass().getClassLoader());
            //this.transactionRunner = new TransactionRunner(sessionFactory);
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
    public OemManager oemManager()
    {
        return this.oemManager;
    }

    @Override
    public AlertManager alertManager()
    {
        return this.alertManager;
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        final LinkedList list = new LinkedList();

        logger.info("getEvents( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");

        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s) throws SQLException
                {
                    String queryStr = query;
                    Map<String,Object> params;
                    // if no policyId is specified (or -1), just change all "= :policyID" to "is not null" so it matches all racks
                    // if policyId is specified, set the variable
                    if (policyId == null || policyId == -1) {
                        params = Collections.emptyMap();
                        queryStr = queryStr.replace("= :policyId","is not null");
                        queryStr = queryStr.replace("=:policyId","is not null");
                    } else {
                        params = new HashMap<String,Object>();
                        params.put("policyId", (Object)policyId);
                    } 

                    runQuery(queryStr, s, list, limit, params);

                    return true;
                }
            };

        long startTime = System.currentTimeMillis();
        this.runTransaction(tw);
        long elapsed = System.currentTimeMillis() - startTime;

        logger.info("getEvents( query: " + query + " policyId: " + policyId + " limit: " + limit + " ) took " + elapsed + " ms");
        
        Collections.sort(list);
        return new ArrayList(list);
    }

    public Connection getDBConnection()
    {
        try {
            return DataSourceFactory.factory().getConnection();
        } catch (SQLException e) {
            logger.warn("Failed to create DB Connection.",e);
            return null;
        }
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
            logger.error("Failed to initialize Factory Defaults (net-alpaca returned an error)",exc);
        }

        /**
         * Remove the flag after setting initial settings
         */
        File f = new File(FACTORY_DEFAULT_FLAG);
        if (f.exists()) {
            f.delete();
        }
            
    }

    private SessionFactory makeSessionFactory(ClassLoader cl)
    {
        SessionFactory sessionFactory = null;

        try {
            AnnotationConfiguration cfg = buildAnnotationConfiguration(cl);

            long t0 = System.currentTimeMillis();
            sessionFactory = cfg.buildSessionFactory();
            long t1 = System.currentTimeMillis();

            logger.info("Built new SessionFactory in " + (t1 - t0) + " millis");
        } catch (HibernateException exn) {
            logger.warn("Failed to create SessionFactory", exn);
        }

        return sessionFactory;
    }

    @SuppressWarnings("unchecked")
	private AnnotationConfiguration buildAnnotationConfiguration(ClassLoader cl)
    {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        Thread thisThread = Thread.currentThread();
        ClassLoader oldCl = thisThread.getContextClassLoader();

        try {
            thisThread.setContextClassLoader(cl);

            for (String clz : this.annotatedClasses) {
                Class c = cl.loadClass(clz);
                cfg.addAnnotatedClass(c);
            }
        }
        catch (java.lang.ClassNotFoundException exc) {
            logger.warn("Annotated Class not found", exc);
        }
        finally {
            thisThread.setContextClassLoader(oldCl);
        }

        return cfg;
    }

    private void initializeUvmAnnotatedClasses()
    {
        /* api */
        this.addAnnotatedClass("com.untangle.uvm.LanguageSettings");
        this.addAnnotatedClass("com.untangle.uvm.MailSettings");
        this.addAnnotatedClass("com.untangle.uvm.Period");
        this.addAnnotatedClass("com.untangle.uvm.SkinSettings");
        this.addAnnotatedClass("com.untangle.uvm.AdminSettings");
        this.addAnnotatedClass("com.untangle.uvm.User");
        this.addAnnotatedClass("com.untangle.uvm.logging.LoggingSettings");
        this.addAnnotatedClass("com.untangle.uvm.logging.SessionLogEventFromReports");
        this.addAnnotatedClass("com.untangle.uvm.logging.HttpLogEventFromReports");
        this.addAnnotatedClass("com.untangle.uvm.logging.MailLogEventFromReports");
        this.addAnnotatedClass("com.untangle.uvm.logging.OpenvpnLogEventFromReports");
        this.addAnnotatedClass("com.untangle.uvm.logging.CpdBlockEventsFromReports");
        this.addAnnotatedClass("com.untangle.uvm.logging.CpdLoginEventsFromReports");
        this.addAnnotatedClass("com.untangle.uvm.networking.AccessSettings");
        this.addAnnotatedClass("com.untangle.uvm.networking.AddressSettings");
        this.addAnnotatedClass("com.untangle.uvm.node.IPMaskedAddressDirectory");
        this.addAnnotatedClass("com.untangle.uvm.node.IPMaskedAddressRule");
        this.addAnnotatedClass("com.untangle.uvm.node.MimeTypeRule");
        this.addAnnotatedClass("com.untangle.uvm.node.StringRule");
        this.addAnnotatedClass("com.untangle.uvm.node.NodeSettings");
        this.addAnnotatedClass("com.untangle.uvm.snmp.SnmpSettings");
        this.addAnnotatedClass("com.untangle.uvm.toolbox.UpgradeSettings");
        /* impl */
        this.addAnnotatedClass("com.untangle.uvm.engine.LoginEvent");
    }
    
    /**
     * This changes apache to show the regular screen again if it is currently showing the
     * upgrade log (which is displayed during upgrades)
     */
    private void hideUpgradeSplash()
    {
        /**
         * The PID file seems to sometimes mysteriously disappear so also check for the HTML file
         */
        File upgradePidFile  = new File(UPGRADE_PID_FILE);
        File upgradeHtmlFile = new File(UPGRADE_HTML_FILE);

        /* If the upgrade is in progress */
        if (upgradePidFile.exists() || upgradeHtmlFile.exists()) {
            logger.info("Upgrade complete. Removing upgrade splash screen...");

            try {
                Integer exitValue = this.execManager().execResult( UPGRADE_SPLASH_SCRIPT + " stop" );
                if (0 != exitValue) {
                    logger.warn("Upgrade complete. Removing upgrade splash screen... failed");
                } else {
                    logger.info("Upgrade complete. Removing upgrade splash screen... done");
                }
            }
            catch (Exception e) {
                logger.warn("Upgrade complete. Removing upgrade splash screen... failed", e);
            }
        }
    }

    @SuppressWarnings("unchecked") //Query
    private void runQuery(String query, Session s, List l, int limit, Map<String, Object> params)
    {
        logger.debug("runQuery: " + query);
        Query q = s.createQuery(query);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        q.setMaxResults(limit);

        int c = 0;
        for (Iterator i = q.iterate(); i.hasNext() && c < limit; c++) {
            Object sb = i.next();
            if (sb == null)
                logger.warn("Query (" + query + ") returned null item");
            Hibernate.initialize(sb);
            l.add(sb);
        }
    }
    
}
