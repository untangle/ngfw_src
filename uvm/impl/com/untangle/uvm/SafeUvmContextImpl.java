/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.vnet.PipelineFoundry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.Map;

/** SafeUvmContextImpl */

/** This is the root object "context" providing the Untangle VM functionality for applications and the user interface */
public class SafeUvmContextImpl extends UvmContextBase implements SafeUvmContext
{
    private static final SafeUvmContextImpl CONTEXT = new SafeUvmContextImpl();

    private static final Logger logger = LogManager.getLogger(SafeUvmContextImpl.class);

    private static int threadNumber = 1;

    /**
     * getInstance gets the singleton SafeUvmContextImpl reference
     * @return SafeUvmContextImpl
     */
    protected static SafeUvmContextImpl getInstance()
    {
        return CONTEXT;
    }

    /**
     * context gets the singleton UvmContext reference
     * @return UvmContext
     */
    public static SafeUvmContext context()
    {
        return CONTEXT;
    }

    /**
     * Get LocalDirectory
     * @return LocalDirectory
     */
    @Override
    public LocalDirectory localDirectory()
    {
        return UvmContextFactory.context().localDirectory();
    }

    /**
     * Get BrandingManager
     * @return BrandingManager
     */
    @Override
    public BrandingManager brandingManager()
    {
        return UvmContextFactory.context().brandingManager();
    }

    /**
     * Get SkinManager
     * @return SkinManager
     */
    @Override
    public SkinManagerImpl skinManager()
    {
        return (SkinManagerImpl) UvmContextFactory.context().skinManager();
    }

    /**
     * Get MetricManager
     * @return MetricManager
     */
    @Override
    public MetricManager metricManager()
    {
        return UvmContextFactory.context().metricManager();
    }

    /**
     * Get LanguageManager
     * @return LanguageManager
     */
    @Override
    public LanguageManagerImpl languageManager()
    {
        return (LanguageManagerImpl) UvmContextFactory.context().languageManager();
    }

    /**
     * Get CertificateManager
     * @return CertificateManager
     */
    @Override
    public CertificateManager certificateManager()
    {
        return UvmContextFactory.context().certificateManager();
    }

    /**
     * Get ConfigManager
     * @return ConfigManager
     */
    @Override
    public ConfigManager configManager()
    {
        return UvmContextFactory.context().configManager();
    }

    /**
     * Get GoogleManager
     * @return GoogleManager
     */
    @Override
    public GoogleManager googleManager()
    {
        return UvmContextFactory.context().googleManager();
    }

    /**
     * Get GeographyManager
     * @return GeographyManager
     */
    @Override
    public GeographyManager geographyManager()
    {
        return UvmContextFactory.context().geographyManager();
    }

    /**
     * Get NetspaceManager
     * @return NetspaceManager
     */
    @Override
    public NetspaceManager netspaceManager()
    {
        return UvmContextFactory.context().netspaceManager();
    }

    /**
     * Get DaemonManager
     * @return DaemonManager
     */
    @Override
    public DaemonManager daemonManager()
    {
        return UvmContextFactory.context().daemonManager();
    }

    /**
     * Get AppManager
     * @return AppManager
     */
    @Override
    public AppManager appManager()
    {
        return UvmContextFactory.context().appManager();
    }


    /**
     * Get LoggingManager
     * @return LoggingManager
     */
    @Override
    public LoggingManager loggingManager() {
        return UvmContextFactory.context().loggingManager();
    }

    /**
     * Get MailSender
     * @return MailSender
     */
    @Override
    public MailSenderImpl mailSender()
    {
        return (MailSenderImpl) UvmContextFactory.context().mailSender();
    }

    /**
     * Get AdminManager
     * @return AdminManager
     */
    @Override
    public AdminManagerImpl adminManager()
    {
        return (AdminManagerImpl) UvmContextFactory.context().adminManager();
    }

    /**
     * Get SystemManager
     * @return SystemManager
     */
    @Override
    public SystemManagerImpl systemManager()
    {
        return (SystemManagerImpl) UvmContextFactory.context().systemManager();
    }

    /**
     * Get DashboardManager
     * @return DashboardManager
     */
    @Override
    public DashboardManagerImpl dashboardManager()
    {
        return (DashboardManagerImpl) UvmContextFactory.context().dashboardManager();
    }
    
    /**
     * Get NetworkManager
     * @return NetworkManager
     */
    @Override
    public NetworkManager networkManager()
    {
        return UvmContextFactory.context().networkManager();
    }

    /**
     * Get GetConnectivityTester
     * @return GetConnectivityTester
     */
    @Override
    public ConnectivityTesterImpl getConnectivityTester()
    {
        return (ConnectivityTesterImpl) UvmContextFactory.context().getConnectivityTester();
    }

    /**
     * Get EventManager
     * @return EventManager
     */
    @Override
    public EventManager eventManager()
    {
        return UvmContextFactory.context().eventManager();
    }

    /**
     * Get UriManager
     * @return UriManager
     */
    @Override
    public UriManager uriManager()
    {
        return UvmContextFactory.context().uriManager();
    }

    /**
     * Get SettingsManager
     * @return SettingsManager
     */
    @Override
    public SettingsManager settingsManager()
    {
        return UvmContextFactory.context().settingsManager();
    }

    /**
     * Get NotificationManager
     * @return NotificationManager
     */
    @Override
    public NotificationManagerImpl notificationManager()
    {
        return (NotificationManagerImpl) UvmContextFactory.context().notificationManager();
    }

    /**
     * Get CloudManager
     * @return CloudManager
     */
    @Override
    public CloudManager cloudManager()
    {
        return UvmContextFactory.context().cloudManager();
    }

    /**
     * Get PluginManager
     * @return PluginManager
     */
    @Override
    public PluginManager pluginManager() {
        return UvmContextFactory.context().pluginManager();
    }

    /**
     * Get NetcapManager
     * @return NetcapManager
     */
    @Override
    public NetcapManager netcapManager() {
        return UvmContextFactory.context().netcapManager();
    }

    /**
     * Get SessionMonitor
     * @return SessionMonitor
     */
    @Override
    public SessionMonitor sessionMonitor()
    {
        return UvmContextFactory.context().sessionMonitor();
    }

    /**
     * Get OemManager
     * @return OemManager
     */
    @Override
    public OemManager oemManager() {
        return UvmContextFactory.context().oemManager();
    }

    /**
     * Get HostTable
     * @return HostTable
     */
    @Override
    public HostTable hostTable()
    {
        return UvmContextFactory.context().hostTable();
    }

    /**
     * Get DeviceTable
     * @return DeviceTable
     */
    @Override
    public DeviceTable deviceTable()
    {
        return UvmContextFactory.context().deviceTable();
    }

    /**
     * Get UserTable
     * @return UserTable
     */
    @Override
    public UserTable userTable()
    {
        return UvmContextFactory.context().userTable();
    }

    /**
     * Get LicenseManager 
     * @return LicenseManager 
     */
    @Override
    public LicenseManager licenseManager()
    {
        return UvmContextFactory.context().licenseManager();
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @return Thread running at normal priority
     */
    @Override
    public Thread newThread(final Runnable runnable)
    {
        return newThread(runnable, Thread.NORM_PRIORITY);
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @param priority 
     * @return Thread running at specified priority.
     */
    @Override
    public Thread newThread(final Runnable runnable, int priority)
    {
        int threadNum;
        synchronized( SafeUvmContextImpl.class ) {
            threadNum = threadNumber++;
        }
        Thread thread = new Thread(runnable, "UTThread-" + threadNum);
        thread.setPriority(priority);
        return thread;
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @param name
     * @return Thread running at normal priority
     */
    @Override
    public Thread newThread(final Runnable runnable, final String name)
    {
        return newThread(runnable, name, Thread.NORM_PRIORITY);
    }

    /**
     * newThread starts a new thread
     * @param runnable
     * @param name
     * @param priority Thread priority
     * @return Thread running at specified priority.
     */
    @Override
    public Thread newThread(final Runnable runnable, final String name, int priority)
    {
        Runnable task = () -> {
                try {
                    runnable.run();
                } catch (OutOfMemoryError exn) {
                    SafeUvmContextImpl.getInstance().fatalError("SafeUvmContextImpl", exn);
                } catch (Exception exn) {
                    logger.error("Exception running: {}", runnable, exn);
            }
        };
        Thread thread = new Thread(task, name);
        thread.setPriority(priority);
        return thread;
    }

    /**
     * Get PipelineFoundry
     * @return PipelineFoundry
     */
    @Override
    public PipelineFoundry pipelineFoundry() {
        return UvmContextFactory.context().pipelineFoundry();
    }

    /**
     * shutdown stops the UVM
     */
    @Override
    public void shutdown()
    {
        UvmContextFactory.context().shutdown();
    }

    /**
     * rebootBox reboots the server
     */
    @Override
    public void rebootBox()
    {
        UvmContextFactory.context().rebootBox();
    }

    /**
     * shutdownBox shuts down the server
     */
    @Override
    public void shutdownBox()
    {
        UvmContextFactory.context().shutdownBox();
    }

    /**
     * Forces the system to synchronize time with the internet
     * @return exit code value
     */
    @Override
    public int forceTimeSync()
    {
        return UvmContextFactory.context().forceTimeSync();
    }

    /**
     * state returns the current UVM run state
     * @return UvmState
     */
    @Override
    public UvmState state()
    {
        return UvmContextFactory.context().state();
    }

    /**
     * version returns the version (short version) as a string 
     * @return version
     */
    @Override
    public String version()
    {
        return UvmContextFactory.context().version();
    }

    /**
     * gc initiates the garbage collector
     */
    @Override
    public void gc()
    {
        UvmContextFactory.context().gc();
    }
    
    /**
     * getFullVersion returns the version (long version) as a string 
     * @return version
     */
    @Override
    public String getFullVersion()
    {
        return UvmContextFactory.context().getFullVersion();
    }

    /**
     * isExpertMode returns true if the system has the expert mode flag, false otherwise
     * @return bool
     */
    @Override
    public boolean isExpertMode()
    {
        return UvmContextFactory.context().isExpertMode();
    }
    
    /**
     * isWizardComplete returns true if the wizard is complete, false otherwise
     * @return bool
     */
    @Override
    public boolean isWizardComplete()
    {
        return UvmContextFactory.context().isWizardComplete();
    }

    /**
     * wizardComplete sets the wizardComplete flag to true
     */
    @Override
    public void wizardComplete()
    {
        UvmContextFactory.context().wizardComplete();
    }
    
    /**
     * getWizardSettings gets the current wizard settings
     * @return WizardSettings
     */
    @Override
    public WizardSettings getWizardSettings()
    {
        return UvmContextFactory.context().getWizardSettings();
    }
    
    /**
     * setWizardSettings sets the new wizard settings
     * @param wizardSettings
     */
    @Override
    public void setWizardSettings( WizardSettings wizardSettings )
    {
        UvmContextFactory.context().setWizardSettings(wizardSettings);
    }

    /**
     * isRegistered returns true if registration is completed, false otherwise
     * @return bool
     */
    @Override
    public boolean isRegistered()
    {
        return UvmContextFactory.context().isRegistered();
    }

    /**
     * setRegistered - set the registration completed flag to true
     */
    @Override
    public void setRegistered()
    {
        UvmContextFactory.context().setRegistered();
    }

    /**
     * isRemoteSetup returns true if Setup Wizard is to be run remotely, false for local.
     * @return bool
     */
    @Override
    public boolean isRemoteSetup()
    {
        return UvmContextFactory.context().isRemoteSetup();
    }

    /**
     * setRemoteSetup - set the registration completed flag to true
     * @param enabled   If true, remove the flag.  If false, create it.
     */
    @Override
    public void setRemoteSetup(boolean enabled)
    {
        UvmContextFactory.context().setRemoteSetup(enabled);
    }

    /**
     * isAppliance returns true if this is an official untangle appliance, false otherwise
     * @return bool
     */
    @Override
    public boolean isAppliance()
    {
        return UvmContextFactory.context().isAppliance();
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
    @Override
    public String getApplianceModel()
    {
        return UvmContextFactory.context().getApplianceModel();
    }


    /**
     * isCCHidden - returns true if the CC hidden flag file exists
     * @return boolean - True if we should hide CC elements, false otherwise
     */
    @Override
    public boolean isCCHidden()
    {
        return UvmContextFactory.context().isCCHidden();
    }

    /**
     * isDiskless - returns true if this is a diskless system
     * @return bool
     */
    @Override
    public boolean isDiskless()
    {
        return UvmContextFactory.context().isDiskless();
    }

    /**
     * useTempFileSystem - returns true if this is a system where we need special
     * handling of frequently written files to reduce wear on limited write media
     * @return bool
     */
    @Override
    public boolean useTempFileSystem()
    {
        return UvmContextFactory.context().useTempFileSystem();
    }

    /**
     * getTempBackupTimer - returns the number of seconds to be used for the
     * periodic backup interval of the tempfs database or zero if the
     * configuration file does not exist.
     *
     * @return long
     */
    @Override
    public long getTempBackupTimer()
    {
        return UvmContextFactory.context().getTempBackupTimer();
    }

    /**
     * Returns true if this is a developer build in the development
     * environment
     * @return <doc>
     */
    @Override
    public boolean isDevel()
    {
        return UvmContextFactory.context().isDevel();
    }

    /**
     * Returns true if running with the ATS utilities installed
     * @return <doc>
     */
    @Override
    public boolean isAts()
    {
        return UvmContextFactory.context().isAts();
    }

    /**
     * isNetBoot
     * Returns true if this is a netbooted install on Untangle internal network
     * If this is netbooted on our internal network (using netboot.preseed)
     * We automatically enable some access rules to make it easier to access
     * via SSH and HTTPS
     * @return bool
     */
    @Override
    public boolean isNetBoot()
    {
        return UvmContextFactory.context().isNetBoot();
    }

    /**
     * getTranslations get the translation map for the specified module
     * @param module
     * @return map
     */
    @Override
    public Map<String, String> getTranslations(String module)
    {
        return this.languageManager().getTranslations(module);
    }

    /**
     * getCompanyName - gets the company name from branding manager
     * @return String
     */
    @Override
    public String getCompanyName()
    {
        return this.brandingManager().getCompanyName();
    }

    /**
     * No op init method
     */
    @Override
    protected void init() {
        // no-op
    }

    /**
     * No op postInit method
     */
    @Override
    protected void postInit() {
        // no-op
    }

    /**
     * No op destroy method
     */
    @Override
    protected void destroy() {
        // no-op
    }

    /**
     * getStoreUrl - gets the store (untangle cloud) API url
     * @return String
     */
    @Override
    public String getStoreUrl()
    {
        return UvmContextFactory.context().getStoreUrl();
    }

    /**
     * getCmdUrl - gets the store (untangle cloud) API url
     * @return String
     */
    @Override
    public String getCmdUrl()
    {
        return UvmContextFactory.context().getCmdUrl();
    }

    /**
     * isStoreAvailable - checks to see if the Untangle cloud API is reachable
     * @return bool
     */
    @Override
    public boolean isStoreAvailable()
    {
        return UvmContextFactory.context().isStoreAvailable();
    }

    /**
     * getHelpUrl - get the help URL for the UI
     * @return String
     */
    @Override
    public String getHelpUrl()
    {
        return UvmContextFactory.context().getHelpUrl();
    }

    /**
     * getFeedbackUrl - get the feedback URL for the UI
     * @return String
     */
    @Override
    public String getFeedbackUrl()
    {
        return UvmContextFactory.context().getFeedbackUrl();
    }

    /**
     * getLegalUrl - get the URL for legal information in the UI
     * @return String
     */
    @Override
    public String getLegalUrl()
    {
        return UvmContextFactory.context().getLegalUrl();
    }

    /**
     * logEvent - sends an event to reports for logging
     * @param evt
     */
    @Override
    public void logEvent(LogEvent evt)
    {
        UvmContextFactory.context().logEvent(evt);
    }

    /**
     * logJavascriptException logs an exception to the logs
     * This is used by the UI to append javascript exceptions to the logs
     * so when debugging we see them in the logs instead of them
     * going to the admin's browser console only
     * @param json - the jsonobject
     */
    @Override
    public void logJavascriptException(JSONObject json)
    {
        UvmContextFactory.context().logJavascriptException(json);
    }

    /**
     * getServerUID - returns the server UID (aaaa-xxxx-bbbb-xxxx)
     * @return the UID
     */
    @Override
    public String getServerUID()
    {
        return UvmContextFactory.context().getServerUID();
    }

    /**
     * getRegion - returns the name of the region
     * @return the region
     */
    @Override
    public String getRegionName()
    {
        return UvmContextFactory.context().getRegionName();
    }


    /**
     * getServerSerialNumber - returns the server serial number (aaaa-xxxx-bbbb-xxxx)
     * @return the serial number
     */
    @Override
    public String getServerSerialNumber()
    {
        return UvmContextFactory.context().getServerSerialNumber();
    }

    /**
     * getWebuiStartupInfo
     * This call returns one big JSONObject with references to all the
     * important information This is used to avoid lots of separate
     * synchronous calls via the Web UI. Reducing all these separate
     * calls to initialize the UI reduces startup time
     * @return JSONObject
     */
    @Override
    public JSONObject getWebuiStartupInfo()
    {
        // Since getWebuiStartupInfo from UvmContext holds reference to execManager, not returning as is.
        // Instead, constructing the separate response by adding only the required object references.
        JSONObject json = new JSONObject();

        try {
            json.put("languageManager", this.languageManager());
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
            json.put("settingsManager", this.settingsManager());
            json.put("appsViews", this.appManager().getAppsViews());

            json.put("languageSettings", this.languageManager().getLanguageSettings());
            json.put("version", this.version());
            json.put("architecture", System.getProperty("os.arch"));
            json.put("applianceModel", this.getApplianceModel());
            json.put("translations", this.languageManager().getTranslations("untangle"));
            json.put("skinInfo", this.skinManager().getSkinInfo());
            json.put("hostname", this.networkManager().getNetworkSettings().getHostName());
            json.put("networkSettings", this.networkManager().getNetworkSettings());
            json.put("companyName", this.brandingManager().getCompanyName());
            json.put("companyURL", this.brandingManager().getCompanyUrl());
            
            String serialNumber = this.getServerSerialNumber();
            if(serialNumber == null){
                serialNumber = StringUtils.EMPTY;
            }
            json.put("serverUID", this.getServerUID());
            json.put("serverSerialnumber", serialNumber );
            json.put("fullVersion", this.getFullVersion());
            json.put("fullVersionAndRevision", this.adminManager().getFullVersionAndRevision());
            json.put("regionName", this.getRegionName());
            json.put("storeUrl", this.getStoreUrl());
            json.put("helpUrl", this.getHelpUrl());
            json.put("feedbackUrl", this.getFeedbackUrl());
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
     * synchronous calls via the Setup UI. Reducing all these separate
     * calls to initialize the UI reduces startup time
     * @return JSONObject
     */
    @Override
    public JSONObject getSetupStartupInfo()
    {
        // Setup startup objects from UvmContext do not hold reference to any malicious command. Hence returning as is.
        return UvmContextFactory.context().getSetupStartupInfo();
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
}
