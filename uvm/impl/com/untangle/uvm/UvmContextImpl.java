/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import org.jabsorb.JSONSerializer;
import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.app.Reporting;
import com.untangle.uvm.servlet.ServletFileManager;
import com.untangle.uvm.servlet.ServletUtils;

/** UvmContextImpl */
/** This is the root object "context" providing the Untangle VM functionality for applications and the user interface */
public class UvmContextImpl extends UvmContextBase implements UvmContext
{
    private static final UvmContextImpl CONTEXT = new UvmContextImpl();

    private static final String UVM_STATUS_FILE = "/var/run/uvm.status";
    private static final String UPGRADE_PID_FILE = "/var/run/upgrade.pid";
    private static final String UPGRADE_SPLASH_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-show-upgrade-splash";
    private static final String UID_FILE = System.getProperty("uvm.conf.dir") + "/uid";
    private static final String SERIAL_NUMBER_FILE = "/sys/devices/virtual/dmi/id/product_serial";
    private static final String SERIAL_NUMBER_REGEX = "[0-1][0-9][0-9][0-9][0-9A-Z][0W][0-9A-Z][0-9]{7}";
    private static final String WIZARD_SETTINGS_FILE = System.getProperty("uvm.conf.dir") + "/" + "wizard.js";
    private static final String DISKLESS_MODE_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/diskless-mode-flag";
    private static final String IS_REGISTERED_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/is-registered-flag";
    private static final String APPLIANCE_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/appliance-flag";
    private static final String APPLIANCE_MODEL_FILE = System.getProperty("uvm.conf.dir") + "/appliance-model";
    private static final String APPLIANCE_VIRTUAL_DETECT_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-virtual-detect.py";
    private static final String POST_STARTUP_SCRIPT_DIR = "/etc/untangle/post-uvm-hook.d";

    private static final String CREATE_UID_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-createUID.py";
    private static final String REBOOT_SCRIPT = "/sbin/reboot";
    private static final String SHUTDOWN_SCRIPT = "/sbin/shutdown";
    private static final String TIMESYNC_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-force-time-sync";
    
    private static final String PROPERTY_STORE_URL = "uvm.store.url";
    private static final String DEFAULT_STORE_URL = "https://www.untangle.com/api/v1";
    private static final String PROPERTY_HELP_URL = "uvm.help.url";
    private static final String DEFAULT_HELP_URL = "http://wiki.untangle.com/get.php";
    private static final String PROPERTY_LEGAL_URL = "uvm.legal.url";
    private static final String DEFAULT_LEGAL_URL = "http://www.untangle.com/legal";

    private static final Object startupWaitLock = new Object();

    private static final Logger logger = Logger.getLogger(UvmContextImpl.class);

    private static String uid = null;
    private static String serialNumber = null;
    private static String applianceModel = null;
    private static int threadNumber = 1;

    private UvmState state;
    private AdminManagerImpl adminManager;
    private LoggingManagerImpl loggingManager;
    private MailSenderImpl mailSender;
    private NetworkManagerImpl networkManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private AppManagerImpl appManager;
    private CertificateManagerImpl certificateManager;
    private GeographyManagerImpl geographyManager;
    private NetcapManagerImpl netcapManager;
    private EventManagerImpl eventManager;
    private UriManagerImpl uriManager;
    private DaemonManagerImpl daemonManager;
    private HostsFileManagerImpl hostsFileManager;
    private BrandingManagerImpl brandingManager;
    private SkinManagerImpl skinManager;
    private MetricManagerImpl metricManager;
    private LanguageManagerImpl languageManager;
    private DefaultLicenseManagerImpl defaultLicenseManager;
    private LicenseManager licenseManager = null;
    private TomcatManagerImpl tomcatManager;
    private ServletFileManagerImpl servletFileManager;
    private SettingsManagerImpl settingsManager;
    private CertCacheManagerImpl certCacheManager;
    private OemManagerImpl oemManager;
    private NotificationManagerImpl notificationManager;
    private SessionMonitorImpl sessionMonitor;
    private ConntrackMonitorImpl conntrackMonitor;
    private BackupManagerImpl backupManager;
    private HookManagerImpl hookManager;
    private CloudManagerImpl cloudManager;
    private PluginManagerImpl pluginManager;
    private LocalDirectoryImpl localDirectory;
    private ExecManagerImpl execManager;
    private SystemManagerImpl systemManager;
    private DashboardManagerImpl dashboardManager;
    private JSONSerializer serializer;
    private Reporting reportsApp = null;
    private HostTableImpl hostTableImpl = null;
    private DeviceTableImpl deviceTableImpl = null;
    private UserTableImpl userTableImpl = null;
    private NetFilterLogger netFilterLogger = null;
    private InheritableThreadLocal<HttpServletRequest> threadRequest;

    private long lastLoggedWarningTime = System.currentTimeMillis();

    private volatile List<String> annotatedClasses = new LinkedList<>();
    
    /**
     * UvmContextImpl - private because its a singleton and cannot be instantiated
     */
    private UvmContextImpl()
    {
        state = UvmState.LOADED;
    }

    /**
     * getInstance gets the singleton UvmContextImpl reference
     * @return UvmContextImpl
     */
    protected static UvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    /**
     * context gets the singleton UvmContext reference
     * @return UvmContext
     */
    public static UvmContext context()
    {
        return CONTEXT;
    }

    /**
     * Get LocalDirectory
     * @return LocalDirectory
     */
    public LocalDirectory localDirectory()
    {
        return this.localDirectory;
    }

    /**
     * Get BrandingManager
     * @return BrandingManager
     */
    public BrandingManager brandingManager()
    {
        return this.brandingManager;
    }

    /**
     * Get SkinManager
     * @return SkinManager
     */
    public SkinManagerImpl skinManager()
    {
        return this.skinManager;
    }

    /**
     * Get MetricManager
     * @return MetricManager
     */
    public MetricManager metricManager()
    {
        return this.metricManager;
    }

    /**
     * Get LanguageManager
     * @return LanguageManager
     */
    public LanguageManagerImpl languageManager()
    {
        return this.languageManager;
    }

    /**
     * Get CertificateManager
     * @return CertificateManager
     */
    public CertificateManager certificateManager()
    {
        return this.certificateManager;
    }

    /**
     * Get GeographyManager
     * @return GeographyManager
     */
    public GeographyManager geographyManager()
    {
        return this.geographyManager;
    }

    /**
     * Get DaemonManager
     * @return DaemonManager
     */
    public DaemonManager daemonManager()
    {
        return this.daemonManager;
    }

    /**
     * Get HostsFileManager
     * @return HostsFileManager
     */
    public HostsFileManager hostsFileManager()
    {
        return this.hostsFileManager;
    }

    /**
     * Get AppManager
     * @return AppManager
     */
    public AppManager appManager()
    {
        return this.appManager;
    }

    /**
     * Get NodeManager
     * @return NodeManager
     */
    public AppManager nodeManager()
    {
        logger.warn("nodeManager() will be deprecated soon.", new Exception());
        return appManager();
    }

    /**
     * Get LoggingManager
     * @return LoggingManager
     */
    public LoggingManagerImpl loggingManager()
    {
        return this.loggingManager;
    }

    /**
     * Get MailSender
     * @return MailSender
     */
    public MailSenderImpl mailSender()
    {
        return this.mailSender;
    }

    /**
     * Get AdminManager
     * @return AdminManager
     */
    public AdminManagerImpl adminManager()
    {
        return this.adminManager;
    }

    /**
     * Get SystemManager
     * @return SystemManager
     */
    public SystemManagerImpl systemManager()
    {
        return this.systemManager;
    }

    /**
     * Get DashboardManager
     * @return DashboardManager
     */
    public DashboardManagerImpl dashboardManager()
    {
        return this.dashboardManager;
    }
    
    /**
     * Get NetworkManager
     * @return NetworkManager
     */
    public NetworkManager networkManager()
    {
        return this.networkManager;
    }

    /**
     * Get GetConnectivityTester
     * @return GetConnectivityTester
     */
    public ConnectivityTesterImpl getConnectivityTester()
    {
        return this.connectivityTester;
    }

    /**
     * Get NetcapManager
     * @return NetcapManager
     */
    public NetcapManager netcapManager()
    {
        return this.netcapManager;
    }

    /**
     * Get EventManager
     * @return EventManager
     */
    public EventManager eventManager()
    {
        return this.eventManager;
    }

    /**
     * Get UriManager
     * @return UriManager
     */
    public UriManager uriManager()
    {
        return this.uriManager;
    }

    /**
     * Get ServletFileManager
     * @return ServletFileManager
     */
    public ServletFileManager servletFileManager()
    {
        return this.servletFileManager;
    }

    /**
     * Get SettingsManager
     * @return SettingsManager
     */
    public SettingsManager settingsManager()
    {
        return this.settingsManager;
    }

    /**
     * Get CertCacheManager
     * @return CertCacheManager
     */
    public CertCacheManager certCacheManager()
    {
        return this.certCacheManager;
    }

    /**
     * Get OemManager
     * @return OemManager
     */
    public OemManager oemManager()
    {
        return this.oemManager;
    }

    /**
     * Get NotificationManager
     * @return NotificationManager
     */
    public NotificationManagerImpl notificationManager()
    {
        return this.notificationManager;
    }

    /**
     * Get BackupManager
     * @return BackupManager
     */
    public BackupManager backupManager()
    {
        return this.backupManager;
    }

    /**
     * Get HookManager
     * @return HookManager
     */
    public HookManager hookManager()
    {
        return this.hookManager;
    }

    /**
     * Get CloudManager
     * @return CloudManager
     */
    public CloudManager cloudManager()
    {
        return this.cloudManager;
    }

    /**
     * Get PluginManager
     * @return PluginManager
     */
    public PluginManager pluginManager()
    {
        return this.pluginManager;
    }
    
    /**
     * Get PipelineFoundry
     * @return PipelineFoundry
     */
    public PipelineFoundryImpl pipelineFoundry()
    {
        return this.pipelineFoundry;
    }

    /**
     * Get SessionMonitor
     * @return SessionMonitor
     */
    public SessionMonitor sessionMonitor()
    {
        return this.sessionMonitor;
    }

    /**
     * Get ConntrackMonitor
     * @return ConntrackMonitor
     */
    public ConntrackMonitorImpl conntrackMonitor()
    {
        return this.conntrackMonitor;
    }
    
    /**
     * Get ExecManager
     * @return ExecManager
     */
    public ExecManager execManager()
    {
        return this.execManager;
    }

    /**
     * Get TomcatManager
     * @return TomcatManager
     */
    public TomcatManager tomcatManager()
    {
        return tomcatManager;
    }

    /**
     * Get HostTable
     * @return HostTable
     */
    public HostTable hostTable()
    {
        return this.hostTableImpl;
    }

    /**
     * Get DeviceTable
     * @return DeviceTable
     */
    public DeviceTable deviceTable()
    {
        return this.deviceTableImpl;
    }

    /**
     * Get UserTable
     * @return UserTable
     */
    public UserTable userTable()
    {
        return this.userTableImpl;
    }
    
    /**
     * Get LicenseManager 
     * @return LicenseManager 
     */
    public LicenseManager licenseManager()
    {
        AppManager appManager = this.appManager();
        if ( appManager == null )
            return this.defaultLicenseManager;
        if (this.licenseManager == null ) {
            this.licenseManager = (LicenseManager) appManager().app("license");
            if (this.licenseManager == null) {
                logger.debug("Failed to initialize license manager.");
                return this.defaultLicenseManager;
            }
        }

        return licenseManager;
    }
    
    /**
     * createExecManager creates a new ExecManager instance
     * @return ExecManager
     */
    public ExecManager createExecManager()
    {
        ExecManagerImpl execManager = new ExecManagerImpl();
        execManager.setSerializer(serializer);
        return execManager;
    }

    /**
     * threadRequest returns the TLS
     * @return TLS
     */
    public InheritableThreadLocal<HttpServletRequest> threadRequest()
    {
        return threadRequest;
    }

    /**
     * waitForStartup blocks until startup is complete
     */
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

    /**
     * getSerializer gets the standard JSON serializer
     * @return JSONSerializer
     */
    public JSONSerializer getSerializer()
    {
        return this.serializer;
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @return Thread
     */
    public Thread newThread(final Runnable runnable)
    {
        int threadNum;
        synchronized( UvmContextImpl.class ) {
            threadNum = threadNumber++;
        }
        return newThread(runnable, "UTThread-" + threadNum);
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @param name
     * @return Thread
     */
    public Thread newThread(final Runnable runnable, final String name)
    {
        Runnable task = new Runnable()
        {
            /** run */
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

    /**
     * shutdown stops the UVM
     */
    public void shutdown()
    {
        Thread t = newThread(new Runnable()
        {
            /** run */
            public void run()
            {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException exn) {
                }
                logger.info("Exiting");
                System.exit(0);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * rebootBox reboots the server
     */
    public void rebootBox()
    {
        Integer exitValue = this.execManager().execResult(REBOOT_SCRIPT);
        if (0 != exitValue) {
            logger.error("Unable to reboot (" + exitValue + ")");
        } else {
            logger.info("Rebooted at admin request");
        }
    }

    /**
     * shutdownBox shuts down the server
     */
    public void shutdownBox()
    {
        Integer exitValue = this.execManager().execResult(SHUTDOWN_SCRIPT + " -h now");
        if (0 != exitValue) {
            logger.error("Unable to shutdown (" + exitValue + ")");
        } else {
            logger.info("Shutdown at admin request");
        }
    }

    /**
     * Forces the system to synchronize time with the internet
     * @return exit code value
     */
    public int forceTimeSync()
    {
        Integer exitValue = this.execManager().execResult(TIMESYNC_SCRIPT);
        if (0 != exitValue) {
            logger.error("Unable to synchronize time (" + exitValue + ")");
        } else {
            logger.info("Synchronized time");
        }
        return exitValue;
    }

    /**
     * state returns the current UVM run state
     * @return UvmState
     */
    public UvmState state()
    {
        return state;
    }

    /**
     * version returns the version (short version) as a string 
     * @return version
     */
    public String version()
    {
        return com.untangle.uvm.Version.getVersion();
    }

    /**
     * gc initiates the garbage collector
     */
    public void gc()
    {
        System.gc();
    }
    
    /**
     * getFullVersion returns the version (long version) as a string 
     * @return version
     */
    public String getFullVersion()
    {
        return com.untangle.uvm.Version.getFullVersion();
    }

    /**
     * isExpertMode returns true if the system has the expert mode flag, false otherwise
     * @return bool
     */
    public boolean isExpertMode()
    {
        File expertModeFlagFile = new File( System.getProperty("uvm.conf.dir") + "/expert-mode-flag" );
        return expertModeFlagFile.exists();
    }
    
    /**
     * isWizardComplete returns true if the wizard is complete, false otherwise
     * @return bool
     */
    public boolean isWizardComplete()
    {
        return getWizardSettings().getWizardComplete();
    }

    /**
     * wizardComplete sets the wizardComplete flag to true
     */
    public void wizardComplete()
    {
        WizardSettings wizardSettings = getWizardSettings();
        wizardSettings.setWizardComplete( true );
        wizardSettings.setCompletedStep(null);
        setWizardSettings( wizardSettings );

        // start pyconnector if needed
        systemManager().pyconnectorSync();
    }
    
    /**
     * getWizardSettings gets the current wizard settings
     * @return WizardSettings
     */
    public WizardSettings getWizardSettings()
    {
        WizardSettings wizardSettings = null;
        try {
            wizardSettings = settingsManager.load( WizardSettings.class, WIZARD_SETTINGS_FILE );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        if ( wizardSettings == null ) {
            wizardSettings = new WizardSettings();

            /* If the old flag file exists, the wizard is complete */
            try {
                File wizardCompleteFlagFile = new File( System.getProperty("uvm.conf.dir") + "/wizard-complete-flag" );
                if (wizardCompleteFlagFile.exists())
                    wizardSettings.setWizardComplete( true );
            } catch (Exception e) {}

            setWizardSettings( wizardSettings );
        }


        return wizardSettings;
    }
    
    /**
     * setWizardSettings sets the new wizard settings
     * @param wizardSettings
     */
    public void setWizardSettings( WizardSettings wizardSettings )
    {
        String settingsFileName = System.getProperty("uvm.conf.dir") + "/" + "wizard.js";
        try {
            settingsManager.save( WIZARD_SETTINGS_FILE, wizardSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }
    }

    /**
     * isRegistered returns true if registration is completed, false otherwise
     * @return bool
     */
    public boolean isRegistered()
    {
        File keyFile = new File(IS_REGISTERED_FLAG_FILE);
        return keyFile.exists();
    }

    /**
     * setRegistered - set the registration completed flag to true
     */
    public void setRegistered()
    {
        File keyFile = new File(IS_REGISTERED_FLAG_FILE);
        if (!keyFile.exists()) {
            try {
                keyFile.createNewFile();
            } catch (Exception e) {
                logger.error("Failed to create registration file", e);
            }
        }
    }

    /**
     * isAppliance returns true if this is an official untangle appliance, false otherwise
     * @return bool
     */
    public boolean isAppliance()
    {
        File keyFile = new File(APPLIANCE_FLAG_FILE);
        return keyFile.exists();
    }

    /**
     * Get the hardware model type.
     *
     * First see if this is an Untangle hardware appliance product 
     * and if not, see if we're running under a virtual environment.
     *
     * @return
     *  String containing Untangle appliance name or virtual environment. 
     *  If empty, unknown (likely baremetal)
     */
    public String getApplianceModel()
    {
        if (UvmContextImpl.applianceModel == null) {
            /*
             * Untangle hardware appliance.
             */
            BufferedReader reader = null;
            try {
                File keyFile = new File(APPLIANCE_MODEL_FILE);
                if (keyFile.exists()) {
                    reader = new BufferedReader(new FileReader(keyFile));
                    UvmContextImpl.applianceModel = reader.readLine();
                } else {
                    /*
                     * Detect virtual.
                     */
                    UvmContextImpl.applianceModel = this.execManager().execOutput(APPLIANCE_VIRTUAL_DETECT_SCRIPT).trim();
                }
            } catch (IOException x) {
                logger.error("Unable to get appliance model", x);
            }finally{
                try {
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close appliance model file", ex);
                }
            }
        }
        return UvmContextImpl.applianceModel;
    }

    /**
     * isDiskless - returns true if this is a diskless system
     * @return bool
     */
    public boolean isDiskless()
    {
        File keyFile = new File(DISKLESS_MODE_FLAG_FILE);
        return keyFile.exists();
    }
    
    /**
     * Returns true if this is a developer build in the development
     * environment
     * @return <doc>
     */
    public boolean isDevel()
    {
        String val = System.getProperty("com.untangle.isDevel");
        if (val == null)
            return false;
        return "true".equals(val.trim().toLowerCase());
    }

    /**
     * isNetBoot
     * Returns true if this is a netbooted install on Untangle internal network
     * If this is netbooted on our internal network (using netboot.preseed)
     * We automatically enable some access rules to make it easier to access
     * via SSH and HTTPS
     * @return bool
     */
    public boolean isNetBoot()
    {
        boolean result = false;
        File installerSyslog = new File("/var/log/installer/syslog");
        if (installerSyslog.exists()) {
            java.util.Scanner scanner = null;
            try {
                scanner = new java.util.Scanner(installerSyslog);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.contains("BOOTIF") && line.contains("netboot.preseed"))
                        result = true;
                }
            } catch (Exception e) {
                logger.warn("Exception in isNetBoot()", e);
            } finally {
                try {
                    if(scanner != null){
                        scanner.close();
                    }
                } catch (Exception ex) {
                    logger.error("Unable to close scanner", ex);
                }
            }
        }

        return result;
    }

    /**
     * getTranslations get the translation map for the specified module
     * @param module
     * @return map
     */
    public Map<String, String> getTranslations(String module)
    {
        return languageManager.getTranslations(module);
    }

    /**
     * getCompanyName - gets the company name from branding manager
     * @return String
     */
    public String getCompanyName()
    {
        return this.brandingManager.getCompanyName();
    }

    /**
     * getStoreUrl - gets the store (untangle cloud) API url
     * @return String
     */
    public String getStoreUrl()
    {
        String url = System.getProperty(PROPERTY_STORE_URL);
        if (url == null)
            url = DEFAULT_STORE_URL;
        return url;
    }

    /**
     * isStoreAvailable - checks to see if the Untangle cloud API is reachable
     * @return bool
     */
    public boolean isStoreAvailable()
    {
        boolean result = false;
        Socket sock = null;
        for (int tries = 0; tries < 3; tries++) {
            try {
                URL storeUrl = new URL(getStoreUrl());
                String host = storeUrl.getHost();
                InetAddress addr = InetAddress.getByName(host);
                InetSocketAddress remoteAddress = new InetSocketAddress(addr, 80);
                sock = new Socket();
                sock.connect(remoteAddress, 5000);
                result = true;
            } catch (Exception e) {
                logger.warn("Failed to connect to store: " + e);
            }finally{
                try {
                    if(sock != null){
                        sock.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close socket", ex);
                }
            }
            if(result == true){
                return result;
            }
        }
        return result;
    }

    /**
     * getHelpUrl - get the help URL for the UI
     * @return String
     */
    public String getHelpUrl()
    {
        String url = System.getProperty(PROPERTY_HELP_URL);
        if (url == null)
            url = DEFAULT_HELP_URL;
        return url;
    }

    /**
     * getLegalUrl - get the URL for legal information in the UI
     * @return String
     */
    public String getLegalUrl()
    {
        String url = System.getProperty(PROPERTY_LEGAL_URL);
        if (url == null)
            url = DEFAULT_LEGAL_URL;
        return url;
    }

    /**
     * logEvent - sends an event to reports for logging
     * @param evt
     */
    public void logEvent(LogEvent evt)
    {
        if (this.reportsApp == null)
            getReportsApp();
        if (this.reportsApp == null)
            return;

        this.reportsApp.logEvent(evt);
        this.eventManager.logEvent(evt);
    }

    /**
     * logJavascriptException logs an exception to the logs
     * This is used by the UI to append javascript exceptions to the logs
     * so when debugging we see them in the logs instead of them
     * going to the admin's browser console only
     * @param json - the jsonobject
     */
    public void logJavascriptException(JSONObject json)
    {
        logger.warn("Javascript Exception");
        if (json == null)
            return;

        String[] names = JSONObject.getNames(json);
        if (names == null)
            return;

        for ( String name : names ) {
            try {
                Object o = json.get(name);
                if (o == null)
                    continue;
                logger.warn("Javascript Exception [" + name + "]: " + o.toString());
            } catch(Exception e) {}
        }
    }

    /**
     * getServerUID - returns the server UID (aaaa-xxxx-bbbb-xxxx)
     * @return the UID
     */
    public String getServerUID()
    {
        if (UvmContextImpl.uid == null) {
            BufferedReader reader = null;
            try {
                File keyFile = new File(UID_FILE);
                if (keyFile.exists()) {
                    reader = new BufferedReader(new FileReader(keyFile));
                    UvmContextImpl.uid = reader.readLine();
                }
            } catch (IOException x) {
                logger.error("Unable to get pop id: ", x);
            }finally{
                try {
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }
        return UvmContextImpl.uid;
    }

    /**
     * getServerSerialNumber - returns the server serial number (aaaa-xxxx-bbbb-xxxx)
     * @return the serial number
     */
    public String getServerSerialNumber()
    {
        if (UvmContextImpl.serialNumber == null) {
            BufferedReader reader = null;
            try {
                File serialNumberFile = new File(SERIAL_NUMBER_FILE);
                String serialNumber = null;
                if (serialNumberFile.exists()) {
                    reader = new BufferedReader(new FileReader(serialNumberFile));
                    serialNumber = reader.readLine().replaceAll("[\\-]","");
                }
                if( (serialNumber != null ) && serialNumber.length() == 14 && serialNumber.matches(SERIAL_NUMBER_REGEX)){
                    UvmContextImpl.serialNumber = serialNumber;
                }
            } catch (IOException x) {
                logger.error("Unable to get serial number: ", x);
            }finally{
                try {
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }
        return UvmContextImpl.serialNumber;
    }

    /**
     * getWebuiStartupInfo
     * This call returns one big JSONObject with references to all the
     * important information This is used to avoid lots of separate
     * synchornous calls via the Web UI. Reducing all these seperate
     * calls to initialize the UI reduces startup time
     * @return JSONObject
     */
    public org.json.JSONObject getWebuiStartupInfo()
    {
        org.json.JSONObject json = new org.json.JSONObject();

        try {
            json.put("languageManager", this.languageManager());
            json.put("skinManager", this.skinManager());
            json.put("appManager", this.appManager());
            json.put("notificationManager", this.notificationManager());
            json.put("adminManager", this.adminManager());
            json.put("eventManager", this.eventManager());
            json.put("uriManager", this.uriManager());
            json.put("systemManager", this.systemManager());
            json.put("dashboardManager", this.dashboardManager());
            json.put("hostTable", this.hostTable());
            json.put("deviceTable", this.deviceTable());
            json.put("userTable", this.userTable());
            json.put("sessionMonitor", this.sessionMonitor());
            json.put("networkManager", this.networkManager());
            json.put("metricManager", this.metricManager());
            json.put("brandingManager", this.brandingManager());
            json.put("execManager", this.execManager());
            json.put("settingsManager", this.settingsManager());
            json.put("appsViews", this.appManager().getAppsViews());

            json.put("languageSettings", this.languageManager().getLanguageSettings());
            json.put("version", this.version());
            json.put("architecture", System.getProperty("os.arch"));
            json.put("applianceModel", this.getApplianceModel());
            json.put("translations", this.languageManager().getTranslations("untangle"));
            json.put("skinSettings", this.skinManager().getSettings());
            json.put("skinInfo", this.skinManager().getSkinInfo());
            json.put("hostname", this.networkManager().getNetworkSettings().getHostName());
            json.put("networkSettings", this.networkManager().getNetworkSettings());
            json.put("companyName", this.brandingManager().getCompanyName());
            
            String serialNumber = this.getServerSerialNumber();
            if(serialNumber == null){
                serialNumber = "";
            }
            json.put("serverUID", this.getServerUID());
            json.put("serverSerialnumber", serialNumber );
            json.put("fullVersion", this.getFullVersion());
            json.put("fullVersionAndRevision", this.adminManager().getFullVersionAndRevision());
            json.put("storeUrl", this.getStoreUrl());
            json.put("helpUrl", this.getHelpUrl());
            json.put("isRegistered", this.isRegistered());
            json.put("isExpertMode", this.isExpertMode());
            json.put("supportEnabled", this.systemManager().getSettings().getSupportEnabled());
            json.put("timeZoneOffset", this.systemManager().getTimeZoneOffset());
            json.put("installType", (this.systemManager().getSettings()==null?"":this.systemManager().getSettings().getInstallType()));

            boolean reportsEnabled = false;
            App reportsApp = UvmContextFactory.context().appManager().app("reports");
            if(reportsApp != null && AppState.RUNNING.equals(reportsApp.getRunState())) {
                reportsEnabled = true;
            }
            json.put("reportsEnabled", reportsEnabled);

        } catch (Exception e) {
            logger.error("Error generating WebUI startup object", e);
        }
        return json;
    }

    /**
     * getSetupStartupInfo
     * This call returns one big JSONObject with references to all the
     * important information This is used to avoid lots of separate
     * synchornous calls via the Setup UI. Reducing all these seperate
     * calls to initialize the UI reduces startup time
     * @return JSONObject
     */
    public org.json.JSONObject getSetupStartupInfo()
    {
        org.json.JSONObject json = new org.json.JSONObject();

        try {
            json.put("adminManager", this.adminManager());
            json.put("networkManager", this.networkManager());
            json.put("connectivityTester", this.getConnectivityTester());
            json.put("systemManager", this.systemManager());
            json.put("mailSender", this.mailSender());
        } catch (Exception e) {
            logger.error("Error generating Setup startup object", e);
        }
        return json;
    }
    
    /**
     * init initializes the untangle VM
     * init is "phase 1" of the startup
     */
    @Override
    protected void init()
    {
        writeStatusFile( "booting" );

        this.serializer = new JSONSerializer();
        serializer.setFixupDuplicates(false);
        serializer.setMarshallNullAttributes(false);

        this.execManager = new ExecManagerImpl();

        this.settingsManager = new SettingsManagerImpl();

        try {
            ServletUtils.getInstance().registerSerializers(serializer);
            settingsManager.setSerializer(serializer);
            execManager.setSerializer(serializer);
        } catch (Exception e) {
            throw new IllegalStateException("register serializers should never fail!", e);
        }

        this.netcapManager = NetcapManagerImpl.getInstance();
        
        createUID();

        this.hookManager = HookManagerImpl.getInstance();

        this.certCacheManager = new CertCacheManagerImpl();

        this.sessionMonitor = new SessionMonitorImpl();

        this.deviceTableImpl = new DeviceTableImpl();

        this.hostTableImpl = new HostTableImpl();

        this.userTableImpl = new UserTableImpl();
        
        this.cloudManager = CloudManagerImpl.getInstance();

        this.servletFileManager = new ServletFileManagerImpl();

        this.languageManager = new LanguageManagerImpl();

        this.backupManager = new BackupManagerImpl();

        this.oemManager = new OemManagerImpl();

        this.loggingManager = new LoggingManagerImpl();

        this.threadRequest = new InheritableThreadLocal<>();

        this.tomcatManager = new TomcatManagerImpl(this, threadRequest, System.getProperty("uvm.tomcat.dir"), System.getProperty("uvm.web.dir"), System.getProperty("uvm.log.dir"));

        this.adminManager = new AdminManagerImpl();

        this.systemManager = new SystemManagerImpl();

        this.networkManager = new NetworkManagerImpl();

        this.conntrackMonitor = ConntrackMonitorImpl.getInstance();

        this.defaultLicenseManager = new DefaultLicenseManagerImpl();

        this.mailSender = MailSenderImpl.mailSender();

        this.pipelineFoundry = PipelineFoundryImpl.foundry();

        this.localDirectory = new LocalDirectoryImpl();

        this.brandingManager = new BrandingManagerImpl();

        this.skinManager = new SkinManagerImpl();

        this.dashboardManager = new DashboardManagerImpl();
        
        this.appManager = new AppManagerImpl();

        this.metricManager = new MetricManagerImpl();

        this.netFilterLogger = new NetFilterLogger();
        
        // Retrieve the connectivity tester
        this.connectivityTester = ConnectivityTesterImpl.getInstance();

        this.certificateManager = new CertificateManagerImpl();

        this.geographyManager = new GeographyManagerImpl();

        this.daemonManager = new DaemonManagerImpl();

        this.hostsFileManager = new HostsFileManagerImpl();

        this.notificationManager = new NotificationManagerImpl();

        this.pluginManager = PluginManagerImpl.getInstance();

        this.eventManager = new EventManagerImpl();

        this.uriManager = new UriManagerImpl();

        // start vectoring
        NetcapManagerImpl.getInstance().run();

        // Start statistic gathering
        metricManager.start();

        state = UvmState.INITIALIZED;
    }

    /**
     * postInit starts UVM services after initialization
     * postInit is "phase 2" of the startup
     */
    @Override
    protected void postInit()
    {
        writeStatusFile( "starting" );

        mailSender.postInit();

        logger.debug("restarting apps");
        appManager.init();

        tomcatManager.startTomcat();
        tomcatManager.writeWelcomeFile();
        tomcatManager.apacheReload();
        
        synchronized (startupWaitLock) {
            state = UvmState.RUNNING;
            startupWaitLock.notifyAll();
        }

        hideUpgradeSplash();

        // Full System GC so the JVM gives memory back
        UvmContextFactory.context().gc();

        // start capturing traffic last
        networkManager.insertRules();

        // load any plugins
        pluginManager.loadPlugins();

        // write status
        writeStatusFile( "running" );

        cloudManager.startZeroTouchMonitor();

        // call startup hook
        callPostStartupHooks();
    }

    /**
     * destroy will stop all untangle-vm processes and services and exit
     */
    @Override
    protected void destroy()
    {
        // the will be removed again by the wrapper
        // this is just so traffic will pass while the untangle-vm shutsdown
        try {
            if ( networkManager != null )
                networkManager.removeRules();
        } catch (Exception exn) {
            logger.error("Failed to remove rules", exn);
        }
            
        try {
            if ( hostTableImpl != null )
                hostTableImpl.saveHosts();
            if ( deviceTableImpl != null )
                deviceTableImpl.saveDevices();
        } catch (Exception exn) {
            logger.error("Failed to save hosts/devices", exn);
        }

        state = UvmState.DESTROYED;

        // stop conntrack monitor
        try {
            if ( conntrackMonitor != null )
                conntrackMonitor.stop();
        } catch (Exception exn) {
            logger.error("could not stop MetricManager", exn);
        }

        // stop metric monitor
        try {
            if (metricManager != null)
                metricManager.stop();
        } catch (Exception exn) {
            logger.error("could not stop MetricManager", exn);
        }

        // stop apps
        try {
            if (appManager != null)
                appManager.destroy();
        } catch (Exception exn) {
            logger.error("could not destroy AppManager", exn);
        }

        // stop netcap
        try {
            NetcapManagerImpl.getInstance().destroy();
        } catch(Exception exn) {
            logger.error("could not destroy Netcap", exn);
        }

        // stop tomcat
        try {
            if (tomcatManager != null)
                tomcatManager.stopTomcat();
        } catch (Exception exn) {
            logger.error("could not stop tomcat", exn);
        }

        // one last garbage collection - for valgrind
        if (isDevel())
            gc();

        writeStatusFile(        "stopped" );
        logger.info("UvmContext destroyed");
    }

    /**
     * fatalError - prints an error and exits
     * @param str
     * @param x - the exception cause or null
     */
    protected void fatalError(String str, Throwable x)
    {
        main.fatalError(str, x);
    }

    /**
     * loadClass loads a class with the global classloader
     * @param name - name of class
     * @return Class
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    protected Class loadClass(String name)
    {
        return main.loadClass(name);
    }
    
    /**
     * Writes the status file.
     * The status file should be in one of a few states:
     *
     * non-existent (uvm not running)
     * "launching" - UVM is being launched (by uvm script)
     * "booting" - UVM booting (phase 1 of startup)
     * "starting" - apps starting (phase 2 of startup)
     * "running" 
     * "stopped"
     * @param status - the status as a string
     */
    private void writeStatusFile( String status )
    {
        java.io.PrintWriter writer = null;
        try {
            File statusFile = new File(UVM_STATUS_FILE);
            if (!statusFile.exists())
                statusFile.createNewFile();
            writer = new java.io.PrintWriter(statusFile, "UTF-8");
            writer.println(status);
        } catch (Exception e) {
            logger.warn("Failed to write status file.",e);
        }finally{
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (Exception ex) {
                logger.error("Unable to close file", ex);
            }
        }
    }

    /**
     * Create a UID file if one does not already exist
     */
    private void createUID()
    {
        File uidFile = new File(UID_FILE);
        
        // If the UID file exists and it is the correct length
        // We already have a valid one
        if (uidFile.exists() && uidFile.length() > 0)
            return;

        // if its devel env just return
        if (isDevel())
            return;

        // if its an untangle netboot, point to internal package server
        String extraOptions = "";
        if (isNetBoot()) {
            extraOptions += " -u \"package-server.untangle.int\" ";
            extraOptions += " -d \"nightly\" ";
        } else {
            extraOptions += " -d \"stable-" + com.untangle.uvm.Version.getVersion().replaceAll("\\.","") + "\" ";
        }

        extraOptions += " -f \"" + System.getProperty("uvm.conf.dir") + "/uid" + "\" ";

        Integer exitValue = this.execManager().execResult(CREATE_UID_SCRIPT + extraOptions);
        if ( exitValue != 0 ) {
            logger.error("Unable to create UID (" + exitValue + ")");
            return;
        } else {
            logger.info("UID Created.");
        }

        // restart pyconnector now that the UID has been generated
        this.execManager().execResult("systemctl restart untangle-pyconnector");

        return;
    }

    /**
     * This changes apache to show the regular screen again if it is
     * currently showing the upgrade log (which is displayed during
     * upgrades)
     */
    private void hideUpgradeSplash()
    {
        /**
         * The PID file seems to sometimes mysteriously disappear so also check
         * if the upgrade page is enabled
         */
        File upgradePidFile = new File(UPGRADE_PID_FILE);
        File upgradeConfFile = new File("/etc/apache2/sites-enabled/upgrade.conf");

        /* If the upgrade is in progress */
        if (upgradePidFile.exists() || upgradeConfFile.exists()) {
            logger.info("Upgrade complete. Removing upgrade splash screen...");

            try {
                Integer exitValue = this.execManager().execResult(UPGRADE_SPLASH_SCRIPT + " stop");
                if (0 != exitValue) {
                    logger.warn("Upgrade complete. Removing upgrade splash screen... failed");
                } else {
                    logger.info("Upgrade complete. Removing upgrade splash screen... done");
                }
            } catch (Exception e) {
                logger.warn("Upgrade complete. Removing upgrade splash screen... failed", e);
            }
        }
    }

    /**
     * getReportsApp finds the reports application sets this.reportsApp to it
     */
    private void getReportsApp()
    {
        synchronized (this) {
            if (this.reportsApp == null) {
                try {
                    // appManager not initialized yet
                    if (this.appManager == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reports app not found, discarding event(s)");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                    
                    this.reportsApp = (Reporting) this.appManager().app("reports");
                    // no reports app
                    if (this.reportsApp == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reports app not found, discarding event(s)");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Unable to initialize reports app", e);
                    return;
                }
            }
        }
    }

    /**
     * callPostStartupHooks calls any registered UVM_START_COMPLETE hooks with the hookManager
     * and also runs run-parts on /etc/untangle/post-uvm-hook.d
     */
    private void callPostStartupHooks()
    {
        UvmContextFactory.context().hookManager().callCallbacks( HookManager.UVM_STARTUP_COMPLETE, 1 );

        File dir = new File(POST_STARTUP_SCRIPT_DIR);
        if (dir.listFiles() != null && dir.listFiles().length > 0) {
            ExecManagerResult result;
            String cmd = "/bin/run-parts -v " + POST_STARTUP_SCRIPT_DIR ;
            try {
                ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(cmd);
                reader.waitFor();
                for ( String output = reader.readFromOutput() ; output != null ; output = reader.readFromOutput() ) {
                    String lines[] = output.split("\\n");
                    for ( String line : lines ) {
                        logger.info("run-parts: " + line);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to run post-startup hooks",e);
            }
        }
    }
}
