/**
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
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import org.jabsorb.JSONSerializer;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.TomcatManager;
import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.OemManager;
import com.untangle.uvm.AlertManager;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.SessionMonitor;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.argon.Argon;
import com.untangle.uvm.argon.ArgonManagerImpl;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.servlet.ServletUtils;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.util.JsonClient;

/**
 * This is the root API providing the Untangle VM functionality for applications and the user interface
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

    private static final Logger logger = Logger.getLogger(UvmContextImpl.class);

    private static String uid;
    
    private UvmState state;
    private AdminManagerImpl adminManager;
    private ArgonManagerImpl argonManager;
    private LoggingManagerImpl loggingManager;
    private MailSenderImpl mailSender;
    private NetworkManagerImpl networkManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private ToolboxManagerImpl toolboxManager;
    private NodeManagerImpl nodeManager;
    private CronManager cronManager;
    private CertificateManagerImpl certificateManager;
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
    private SystemManagerImpl systemManager;
    private JSONSerializer serializer;
    private Reporting reportingNode = null;
    private HostTableImpl hostTableImpl = null;
    private long lastLoggedWarningTime = System.currentTimeMillis();
    
    private volatile List<String> annotatedClasses = new LinkedList<String>();
    
    // constructor ------------------------------------------------------------

    private UvmContextImpl()
    {
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

    public CertificateManager certificateManager()
    {
        return this.certificateManager;
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

    public MailSenderImpl mailSender()
    {
        return this.mailSender;
    }

    public AdminManagerImpl adminManager()
    {
        return this.adminManager;
    }

    public SystemManagerImpl systemManager()
    {
        return this.systemManager;
    }
    
    public NetworkManager networkManager()
    {
        return this.networkManager;
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

    public HostTable hostTable()
    {
        return this.hostTableImpl;
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
                    try {
                        runnable.run();
                    } catch (OutOfMemoryError exn) {
                        UvmContextImpl.getInstance().fatalError("UvmContextImpl", exn);
                    } catch (Exception exn) {
                        logger.error("Exception running: " + runnable, exn);
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
                    } catch (InterruptedException exn) { }
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

    public CronJob makeCronJob(DayOfWeekMatcher days, int hour, int minute, Runnable r)
    {
        return cronManager.makeCronJob(days, hour, minute, r);
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
    
    public String getCompanyName()
    {
        return this.brandingManager.getCompanyName();
    }

    public void logEvent(LogEvent evt)
    {
        if (this.reportingNode == null)
            getReportingNode();
        if (this.reportingNode == null)
            return;

        this.reportingNode.logEvent(evt);
    }

    // UvmContextBase methods --------------------------------------------------

    @Override
    protected void init()
    {
        this.serializer = new JSONSerializer();
        serializer.setFixupDuplicates(false);
        serializer.setMarshallNullAttributes(false);

        this.execManager = new ExecManagerImpl();

        this.settingsManager = new SettingsManagerImpl();
        
        this.sessionMonitor = new SessionMonitorImpl();

        this.hostTableImpl = new HostTableImpl();
        
        try {
            ServletUtils.getInstance().registerSerializers(serializer);
            settingsManager.setSerializer(serializer);
            sessionMonitor.setSerializer(serializer);
            execManager.setSerializer(serializer);
        } catch (Exception e) {
            throw new IllegalStateException("register serializers should never fail!", e);
        }
        
        this.backupManager = new BackupManager();
        
        this.uploadManager = new UploadManagerImpl();
        uploadManager.registerHandler(new RestoreUploadHandler());
        
        this.oemManager = new OemManagerImpl();

        this.cronManager = new CronManager();

        this.loggingManager = new LoggingManagerImpl();

        InheritableThreadLocal<HttpServletRequest> threadRequest = new InheritableThreadLocal<HttpServletRequest>();

        this.tomcatManager = new TomcatManagerImpl(this, threadRequest, System.getProperty("uvm.home"), System.getProperty("uvm.web.dir"), System.getProperty("uvm.log.dir"));

        this.adminManager = new AdminManagerImpl();

        this.networkManager = new NetworkManagerImpl();

        this.defaultLicenseManager = new DefaultLicenseManagerImpl();

        this.mailSender = MailSenderImpl.mailSender();

        this.toolboxManager = ToolboxManagerImpl.toolboxManager();

        this.pipelineFoundry = PipelineFoundryImpl.foundry();

        this.localDirectory = new LocalDirectoryImpl();
        
        this.brandingManager = new BrandingManagerImpl();

        this.languageManager = new LanguageManagerImpl();

        this.skinManager = new SkinManagerImpl();

        this.systemManager = new SystemManagerImpl();

        this.nodeManager = new NodeManagerImpl();

        this.messageManager = new MessageManagerImpl();

        // Retrieve the connectivity tester
        this.connectivityTester = ConnectivityTesterImpl.getInstance();

        // Retrieve the argon manager
        this.argonManager = ArgonManagerImpl.getInstance();

        this.certificateManager = new CertificateManagerImpl(this);

        this.alertManager = new AlertManagerImpl();
        
        // start vectoring
        Argon.getInstance().run( );

        // Start statistic gathering
        messageManager.start();

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
        // Mailsender can now query the hostname
        mailSender.postInit();

        logger.debug("restarting nodes");
        nodeManager.init();
        pipelineFoundry.clearChains();

        /* Reload Apache */
        tomcatManager.writeWelcomeFile();

        //Inform the Certificate manager that everything
        //else is started.
        certificateManager.postInit();

        logger.debug("postInit complete");
        synchronized (startupWaitLock) {
            state = UvmState.RUNNING;
            startupWaitLock.notifyAll();
        }

        hideUpgradeSplash();

        networkManager.refreshNetworkConfig();
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
        // - argonManager
        toolboxManager = null;

        // XXX destroy methods for:
        // - mailSender

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

        logger.info("UvmContext destroyed");
    }

    // package protected methods ----------------------------------------------

    boolean refreshToolbox()
    {
        boolean result = main.refreshToolbox();
        return result;
    }

    void fatalError(String throwingLocation, Throwable x)
    {
        main.fatalError(throwingLocation, x);
    }

    public TomcatManager tomcatManager()
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
    
    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        if (this.reportingNode == null)
            getReportingNode();
        if (this.reportingNode == null)
            return null;

        return this.reportingNode.getEvents( query, policyId, limit );
    }

    // private methods --------------------------------------------------------

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

        /**
         * Re-read network config
         */
        networkManager.refreshNetworkConfig();
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

    private void getReportingNode()
    {
        synchronized(this) {
            if (this.reportingNode == null) {
                try {
                    this.reportingNode = (Reporting) this.nodeManager().node("untangle-node-reporting");
                    if (this.reportingNode == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reporting node not found, discarding event(s)");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Unable to initialize reportingNode", e);
                    return;
                }
            }
        }
    }
}
