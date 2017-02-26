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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;
import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings.NodeState;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.servlet.ServletFileManager;
import com.untangle.uvm.servlet.ServletUtils;

/**
 * This is the root API providing the Untangle VM functionality for applications
 * and the user interface
 */
public class UvmContextImpl extends UvmContextBase implements UvmContext
{
    private static final UvmContextImpl CONTEXT = new UvmContextImpl();

    private static final String UVM_STATUS_FILE = "/var/run/uvm.status";
    private static final String UPGRADE_PID_FILE = "/var/run/upgrade.pid";
    private static final String UPGRADE_SPLASH_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-show-upgrade-splash";;
    private static final String UID_FILE = System.getProperty("uvm.conf.dir") + "/uid";
    private static final String WIZARD_SETTINGS_FILE = System.getProperty("uvm.conf.dir") + "/" + "wizard.js";
    private static final String DISKLESS_MODE_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/diskless-mode-flag";
    private static final String IS_REGISTERED_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/is-registered-flag";
    private static final String APPLIANCE_FLAG_FILE = System.getProperty("uvm.conf.dir") + "/appliance-flag";
    private static final String APPLIANCE_MODEL_FILE = System.getProperty("uvm.conf.dir") + "/appliance-model";

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
    private static String applianceModel = null;

    private UvmState state;
    private AdminManagerImpl adminManager;
    private LoggingManagerImpl loggingManager;
    private MailSenderImpl mailSender;
    private NetworkManagerImpl networkManager;
    private ConnectivityTesterImpl connectivityTester;
    private PipelineFoundryImpl pipelineFoundry;
    private NodeManagerImpl nodeManager;
    private CertificateManagerImpl certificateManager;
    private GeographyManagerImpl geographyManager;
    private NetcapManagerImpl netcapManager;
    private DaemonManagerImpl daemonManager;
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
    private AlertManagerImpl alertManager;
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
    private Reporting reportsNode = null;
    private HostTableImpl hostTableImpl = null;
    private DeviceTableImpl deviceTableImpl = null;
    private NetFilterLogger netFilterLogger = null;
    private InheritableThreadLocal<HttpServletRequest> threadRequest;
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

    public MetricManager metricManager()
    {
        return this.metricManager;
    }

    public LanguageManagerImpl languageManager()
    {
        return this.languageManager;
    }

    public CertificateManager certificateManager()
    {
        return this.certificateManager;
    }

    public GeographyManager geographyManager()
    {
        return this.geographyManager;
    }

    public DaemonManager daemonManager()
    {
        return this.daemonManager;
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

    public DashboardManagerImpl dashboardManager()
    {
        return this.dashboardManager;
    }
    
    public NetworkManager networkManager()
    {
        return this.networkManager;
    }

    public ConnectivityTesterImpl getConnectivityTester()
    {
        return this.connectivityTester;
    }

    public NetcapManager netcapManager()
    {
        return this.netcapManager;
    }

    public ServletFileManager servletFileManager()
    {
        return this.servletFileManager;
    }

    public SettingsManager settingsManager()
    {
        return this.settingsManager;
    }

    public CertCacheManager certCacheManager()
    {
        return this.certCacheManager;
    }

    public OemManager oemManager()
    {
        return this.oemManager;
    }

    public AlertManager alertManager()
    {
        return this.alertManager;
    }

    public BackupManager backupManager()
    {
        return this.backupManager;
    }

    public HookManager hookManager()
    {
        return this.hookManager;
    }

    public CloudManager cloudManager()
    {
        return this.cloudManager;
    }

    public PluginManager pluginManager()
    {
        return this.pluginManager;
    }
    
    public PipelineFoundryImpl pipelineFoundry()
    {
        return this.pipelineFoundry;
    }

    public SessionMonitor sessionMonitor()
    {
        return this.sessionMonitor;
    }

    public ConntrackMonitorImpl conntrackMonitor()
    {
        return this.conntrackMonitor;
    }
    
    public ExecManager execManager()
    {
        return this.execManager;
    }

    public TomcatManager tomcatManager()
    {
        return tomcatManager;
    }

    public HostTable hostTable()
    {
        return this.hostTableImpl;
    }

    public DeviceTable deviceTable()
    {
        return this.deviceTableImpl;
    }

    public LicenseManager licenseManager()
    {
        NodeManager nodeManager = this.nodeManager();
        if ( nodeManager == null )
            return this.defaultLicenseManager;
        if (this.licenseManager == null ) {
            this.licenseManager = (LicenseManager) nodeManager.node("untangle-node-license");
            if (this.licenseManager == null) {
                logger.debug("Failed to initialize license manager.");
                return this.defaultLicenseManager;
            }
        }

        return licenseManager;
    }
    
    public ExecManager createExecManager()
    {
        ExecManagerImpl execManager = new ExecManagerImpl();
        execManager.setSerializer(serializer);
        return execManager;
    }

    public InheritableThreadLocal<HttpServletRequest> threadRequest()
    {
        return threadRequest;
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

    public JSONSerializer getSerializer()
    {
        return this.serializer;
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
                } catch (InterruptedException exn) {
                }
                logger.info("Exiting");
                System.exit(0);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void rebootBox()
    {
        Integer exitValue = this.execManager().execResult(REBOOT_SCRIPT);
        if (0 != exitValue) {
            logger.error("Unable to reboot (" + exitValue + ")");
        } else {
            logger.info("Rebooted at admin request");
        }
    }

    public void shutdownBox()
    {
        Integer exitValue = this.execManager().execResult(SHUTDOWN_SCRIPT + " -h now");
        if (0 != exitValue) {
            logger.error("Unable to shutdown (" + exitValue + ")");
        } else {
            logger.info("Shutdown at admin request");
        }
    }

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

    public UvmState state()
    {
        return state;
    }

    public String version()
    {
        return com.untangle.uvm.Version.getVersion();
    }

    public void gc()
    {
        System.gc();
    }
    
    public String getFullVersion()
    {
        return com.untangle.uvm.Version.getFullVersion();
    }

    public boolean isExpertMode()
    {
        File expertModeFlagFile = new File( System.getProperty("uvm.conf.dir") + "/expert-mode-flag" );
        return expertModeFlagFile.exists();
    }
    
    public boolean isWizardComplete()
    {
        return getWizardSettings().getWizardComplete();
    }

    public void wizardComplete()
    {
        WizardSettings wizardSettings = getWizardSettings();
        wizardSettings.setWizardComplete( true );
        wizardSettings.setCompletedStep(null);
        setWizardSettings( wizardSettings );
    }
    
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
    
    public void setWizardSettings( WizardSettings wizardSettings )
    {
        String settingsFileName = System.getProperty("uvm.conf.dir") + "/" + "wizard.js";
        try {
            settingsManager.save( WIZARD_SETTINGS_FILE, wizardSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }
    }

    public boolean isRegistered()
    {
        File keyFile = new File(IS_REGISTERED_FLAG_FILE);
        return keyFile.exists();
    }

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

    public boolean isAppliance()
    {
        File keyFile = new File(APPLIANCE_FLAG_FILE);
        return keyFile.exists();
    }

    public String getApplianceModel()
    {
        if (UvmContextImpl.applianceModel == null) {
            try {
                File keyFile = new File(APPLIANCE_MODEL_FILE);
                if (keyFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                    UvmContextImpl.applianceModel = reader.readLine();
                } else {
                    UvmContextImpl.applianceModel = "";
                }
            } catch (IOException x) {
                logger.error("Unable to get UID", x);
            }
        }
        return UvmContextImpl.applianceModel;
    }

    public boolean isDiskless()
    {
        File keyFile = new File(DISKLESS_MODE_FLAG_FILE);
        return keyFile.exists();
    }
    
    /**
     * Returns true if this is a developer build in the development environment
     */
    public boolean isDevel()
    {
        String val = System.getProperty("com.untangle.isDevel");
        if (val == null)
            return false;
        return "true".equals(val.trim().toLowerCase());
    }

    /**
     * Returns true if this is a netbooted install on Untangle internal network
     */
    public boolean isNetBoot()
    {
        File installerSyslog = new File("/var/log/installer/syslog");
        if (installerSyslog.exists()) {
            try {
                java.util.Scanner scanner = new java.util.Scanner(installerSyslog);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (line.contains("BOOTIF") && line.contains("netboot.preseed"))
                        return true;
                }
            } catch (Exception e) {
                logger.warn("Exception in isNetBoot()", e);
            }
        }

        return false;
    }

    public Map<String, String> getTranslations(String module)
    {
        return languageManager.getTranslations(module);
    }

    public String getCompanyName()
    {
        return this.brandingManager.getCompanyName();
    }

    public String getStoreUrl()
    {
        String url = System.getProperty(PROPERTY_STORE_URL);
        if (url == null)
            url = DEFAULT_STORE_URL;
        return url;
    }

    public boolean isStoreAvailable()
    {
        for (int tries = 0; tries < 3; tries++) {
            try {
                URL storeUrl = new URL(getStoreUrl());
                String host = storeUrl.getHost();
                InetAddress addr = InetAddress.getByName(host);
                InetSocketAddress remoteAddress = new InetSocketAddress(addr, 80);
                Socket sock = new Socket();
                sock.connect(remoteAddress, 5000);
                sock.close();
                return true;
            } catch (Exception e) {
                logger.warn("Failed to connect to store: " + e);
            }
        }
        return false;
    }

    public String getHelpUrl()
    {
        String url = System.getProperty(PROPERTY_HELP_URL);
        if (url == null)
            url = DEFAULT_HELP_URL;
        return url;
    }

    public String getLegalUrl()
    {
        String url = System.getProperty(PROPERTY_LEGAL_URL);
        if (url == null)
            url = DEFAULT_LEGAL_URL;
        return url;
    }

    public void logEvent(LogEvent evt)
    {
        if (this.reportsNode == null)
            getReportsNode();
        if (this.reportsNode == null)
            return;

        this.reportsNode.logEvent(evt);
    }

    public String getServerUID()
    {
        if (UvmContextImpl.uid == null) {
            try {
                File keyFile = new File(UID_FILE);
                if (keyFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                    UvmContextImpl.uid = reader.readLine();
                    return UvmContextImpl.uid;
                }
            } catch (IOException x) {
                logger.error("Unable to get pop id: ", x);
            }
        }
        return UvmContextImpl.uid;
    }

    /**
     * This call returns one big JSONObject with references to all the important
     * information This is used to avoid lots of separate synchornous calls via
     * the Web UI. Reducing all these seperate calls to initialize the UI
     * reduces startup time
     */
    public org.json.JSONObject getWebuiStartupInfo()
    {
        org.json.JSONObject json = new org.json.JSONObject();

        try {
            json.put("languageManager", this.languageManager());
            json.put("skinManager", this.skinManager());
            json.put("nodeManager", this.nodeManager());
            json.put("alertManager", this.alertManager());
            json.put("adminManager", this.adminManager());
            json.put("systemManager", this.systemManager());
            json.put("dashboardManager", this.dashboardManager());
            json.put("hostTable", this.hostTable());
            json.put("deviceTable", this.deviceTable());
            json.put("sessionMonitor", this.sessionMonitor());
            json.put("networkManager", this.networkManager());
            json.put("metricManager", this.metricManager());
            json.put("brandingManager", this.brandingManager());
            json.put("execManager", this.execManager());
            json.put("settingsManager", this.settingsManager());
            json.put("appsViews", this.nodeManager().getAppsViews());

            json.put("languageSettings", this.languageManager().getLanguageSettings());
            json.put("version", this.version());
            json.put("applianceModel", this.getApplianceModel());
            json.put("translations", this.languageManager().getTranslations("untangle"));
            json.put("skinSettings", this.skinManager().getSettings());
            json.put("skinInfo", this.skinManager().getSkinInfo());
            json.put("hostname", this.networkManager().getNetworkSettings().getHostName());
            json.put("networkSettings", this.networkManager().getNetworkSettings());
            json.put("companyName", this.brandingManager().getCompanyName());
            
            json.put("serverUID", this.getServerUID());
            json.put("fullVersion", this.getFullVersion());
            json.put("fullVersionAndRevision", this.adminManager().getFullVersionAndRevision());
            json.put("storeUrl", this.getStoreUrl());
            json.put("helpUrl", this.getHelpUrl());
            json.put("isRegistered", this.isRegistered());
            json.put("isExpertMode", this.isExpertMode());
            json.put("timeZoneOffset", this.systemManager().getTimeZoneOffset());

            boolean reportsEnabled = false;
            Node reportsNode = UvmContextFactory.context().nodeManager().node("untangle-node-reports");
            if(reportsNode != null && NodeState.RUNNING.equals(reportsNode.getRunState())) {
                reportsEnabled = true;
            }
            json.put("reportsEnabled", reportsEnabled);

        } catch (Exception e) {
            logger.error("Error generating WebUI startup object", e);
        }
        return json;
    }

    /**
     * provide metadata for reports quick add condition logic
     * XXX This should probably live in reports manager
     */
    public org.json.JSONObject getConditionQuickAddHints()
    {
        LinkedList<HostTableEntry> hosts = this.hostTableImpl.getHosts();

        PolicyManager policyManager = (PolicyManager)this.nodeManager().node("untangle-node-policy-manager");
        
        LinkedList<String> hostnames = new LinkedList<String>();
        LinkedList<String> usernames = new LinkedList<String>();
        LinkedList<String> addresses = new LinkedList<String>();

        for ( HostTableEntry host : hosts ) {
            String username = host.getUsername();
            String hostname = host.getHostname();
            String address  = host.getAddress().getHostAddress();
            if ( hostname != null && !"".equals(hostname) && !hostnames.contains(hostname) )
                hostnames.add(hostname);
            if ( username != null && !"".equals(username) && !usernames.contains(username) )
                usernames.add(username);
            if ( address != null && !"".equals(address) && !addresses.contains(address) )
                addresses.add(address);
        }

        try {
            org.json.JSONObject json = new org.json.JSONObject();

            json.put("hostname", hostnames.toArray( new String[0] ));
            json.put("username", usernames.toArray( new String[0] ));
            json.put("c_client_addr", addresses.toArray( new String[0] ));

            if ( policyManager != null ) {
                ArrayList<JSONObject> policiesInfo = policyManager.getPoliciesInfo();
                LinkedList<Long> ids = new LinkedList<Long>();
                for(JSONObject policyInfo : policiesInfo)
                {
                    ids.push(policyInfo.getLong("policyId"));
                }
                json.put("policy_id", ids.toArray( new Long[0]));
            }
            
            return json;
        } catch (Exception e) {
            logger.warn("Error generating quick add hints",e);
        }

        return null;
    }

    /**
     * This call returns one big JSONObject with references to all the important
     * information This is used to avoid lots of separate synchornous calls via
     * the Setup UI. Reducing all these seperate calls to initialize the UI
     * reduces startup time
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
        
        this.cloudManager = CloudManagerImpl.getInstance();

        this.servletFileManager = new ServletFileManagerImpl();

        this.languageManager = new LanguageManagerImpl();

        this.backupManager = new BackupManagerImpl();

        this.oemManager = new OemManagerImpl();

        this.loggingManager = new LoggingManagerImpl();

        this.threadRequest = new InheritableThreadLocal<HttpServletRequest>();

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
        
        this.nodeManager = new NodeManagerImpl();

        this.metricManager = new MetricManagerImpl();

        this.netFilterLogger = new NetFilterLogger();
        
        // Retrieve the connectivity tester
        this.connectivityTester = ConnectivityTesterImpl.getInstance();

        this.certificateManager = new CertificateManagerImpl();

        this.geographyManager = new GeographyManagerImpl();

        this.daemonManager = new DaemonManagerImpl();

        this.alertManager = new AlertManagerImpl();

        this.pluginManager = PluginManagerImpl.getInstance();

        // start vectoring
        NetcapManagerImpl.getInstance().run();

        // Start statistic gathering
        metricManager.start();

        state = UvmState.INITIALIZED;
    }

    @Override
    protected void postInit()
    {
        writeStatusFile( "starting" );

        mailSender.postInit();

        logger.debug("restarting nodes");
        nodeManager.init();

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

        // call startup hook
        UvmContextFactory.context().hookManager().callCallbacks( HookManager.UVM_STARTUP_COMPLETE, null );
    }

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
            if ( metricManager != null )
                metricManager.stop();
        } catch (Exception exn) {
            logger.error("could not stop MetricManager", exn);
        }

        // stop nodes
        try {
            if ( nodeManager != null )
                nodeManager.destroy();
        } catch (Exception exn) {
            logger.error("could not destroy NodeManager", exn);
        }

        // stop netcap
        try {
            NetcapManagerImpl.getInstance().destroy();
        } catch (Exception exn) {
            logger.error("could not destroy Netcap", exn);
        }

        // stop tomcat
        try {
            if ( tomcatManager != null )
            tomcatManager.stopTomcat();
        } catch (Exception exn) {
            logger.error("could not stop tomcat", exn);
        }

        // one last garbage collection - for valgrind
        if (isDevel())
            gc();

        writeStatusFile( "stopped" );
        logger.info("UvmContext destroyed");
    }

    protected void fatalError(String throwingLocation, Throwable x)
    {
        main.fatalError(throwingLocation, x);
    }

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
     * "starting" - nodes starting (phase 2 of startup)
     * "running" 
     * "stopped" 
     */
    private void writeStatusFile( String status )
    {
        try {
            File statusFile = new File(UVM_STATUS_FILE);
            if (!statusFile.exists())
                statusFile.createNewFile();
            java.io.PrintWriter writer = new java.io.PrintWriter(statusFile, "UTF-8");
            writer.println(status);
            writer.close();
        } catch (Exception e) {
            logger.warn("Failed to write status file.",e);
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
        this.execManager().execResult("/etc/init.d/untangle-pyconnector restart");

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

    private void getReportsNode()
    {
        synchronized (this) {
            if (this.reportsNode == null) {
                try {
                    // nodeManager not initialized yet
                    if ( this.nodeManager == null ) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reports node not found, discarding event(s)");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                    
                    this.reportsNode = (Reporting) this.nodeManager.node("untangle-node-reports");
                    // no reports node
                    if (this.reportsNode == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reports node not found, discarding event(s)");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Unable to initialize reports Node", e);
                    return;
                }
            }
        }
    }
}
