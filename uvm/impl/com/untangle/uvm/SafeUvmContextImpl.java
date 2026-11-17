/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.logging.LogEvent;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeUvmContextImpl implements SafeUvmContext
{
    private final Supplier<UvmContext> delegate;
    private static final Logger logger = LogManager.getLogger(SafeUvmContextImpl.class);
    private static final SafeUvmContextImpl CONTEXT = new SafeUvmContextImpl(UvmContextFactory::context);
    private final SafeAppManager appManager;
    private final SafeLoggingManager loggingManager;
    private final SafeAdminManager adminManager;
    private final SafeSystemManager systemManager;
    private final SafeNetworkManager networkManager;
    private final SafeConnectivityTester getConnectivityTester;
    private final SafeMailSender mailSender;
    private final SafeCertificateManager certificateManager;
    private final SafeConfigManager configManager;
    private final SafeGoogleManager googleManager;
    private final SafeGeographyManager geographyManager;
    private final SafeNetspaceManager netspaceManager;
    private final SafeDaemonManager daemonManager;
    private final SafeLocalDirectory localDirectory;
    private final SafeMetricManager metricManager;
    private final SafeLanguageManager languageManager;
    private final SafeLicenseManager licenseManager;
    private final SafeSettingsManager settingsManager;
    private final SafeDashboardManager dashboardManager;
    private final SafeSessionMonitor sessionMonitor;
    private final SafeOemManager oemManager;
    private final SafeNotificationManager notificationManager;
    private final SafeCloudManager cloudManager;
    private final SafePluginManager pluginManager;
    private final SafeNetcapManager netcapManager;
    private final SafeEventManager eventManager;
    private final SafeUriManager uriManager;
    private final SafeHostTable hostTable;
    private final SafeDeviceTable deviceTable;
    private final SafeUserTable userTable;
    private final SafePipelineFoundry pipelineFoundry;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeUvmContextImpl(Supplier<UvmContext> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.appManager = new SafeAppManagerImpl(() -> this.delegate.get().appManager());
        this.loggingManager = new SafeLoggingManagerImpl(() -> this.delegate.get().loggingManager());
        this.adminManager = new SafeAdminManagerImpl(() -> this.delegate.get().adminManager());
        this.systemManager = new SafeSystemManagerImpl(() -> this.delegate.get().systemManager());
        this.networkManager = new SafeNetworkManagerImpl(() -> this.delegate.get().networkManager());
        this.getConnectivityTester = new SafeConnectivityTesterImpl(() -> this.delegate.get().getConnectivityTester());
        this.mailSender = new SafeMailSenderImpl(() -> this.delegate.get().mailSender());
        this.certificateManager = new SafeCertificateManagerImpl(() -> this.delegate.get().certificateManager());
        this.configManager = new SafeConfigManagerImpl(() -> this.delegate.get().configManager());
        this.googleManager = new SafeGoogleManagerImpl(() -> this.delegate.get().googleManager());
        this.geographyManager = new SafeGeographyManagerImpl(() -> this.delegate.get().geographyManager());
        this.netspaceManager = new SafeNetspaceManagerImpl(() -> this.delegate.get().netspaceManager());
        this.daemonManager = new SafeDaemonManagerImpl(() -> this.delegate.get().daemonManager());
        this.localDirectory = new SafeLocalDirectoryImpl(() -> this.delegate.get().localDirectory());
        this.metricManager = new SafeMetricManagerImpl(() -> this.delegate.get().metricManager());
        this.languageManager = new SafeLanguageManagerImpl(() -> this.delegate.get().languageManager());
        this.licenseManager = new SafeLicenseManagerImpl(() -> this.delegate.get().licenseManager());
        this.settingsManager = new SafeSettingsManagerImpl(() -> this.delegate.get().settingsManager());
        this.dashboardManager = new SafeDashboardManagerImpl(() -> this.delegate.get().dashboardManager());
        this.sessionMonitor = new SafeSessionMonitorImpl(() -> this.delegate.get().sessionMonitor());
        this.oemManager = new SafeOemManagerImpl(() -> this.delegate.get().oemManager());
        this.notificationManager = new SafeNotificationManagerImpl(() -> this.delegate.get().notificationManager());
        this.cloudManager = new SafeCloudManagerImpl(() -> this.delegate.get().cloudManager());
        this.pluginManager = new SafePluginManagerImpl(() -> this.delegate.get().pluginManager());
        this.netcapManager = new SafeNetcapManagerImpl(() -> this.delegate.get().netcapManager());
        this.eventManager = new SafeEventManagerImpl(() -> this.delegate.get().eventManager());
        this.uriManager = new SafeUriManagerImpl(() -> this.delegate.get().uriManager());
        this.hostTable = new SafeHostTableImpl(() -> this.delegate.get().hostTable());
        this.deviceTable = new SafeDeviceTableImpl(() -> this.delegate.get().deviceTable());
        this.userTable = new SafeUserTableImpl(() -> this.delegate.get().userTable());
        this.pipelineFoundry = new SafePipelineFoundryImpl(() -> this.delegate.get().pipelineFoundry());
    }

    /**
     * Return the singleton Safe context.
     * @return The restricted context
     */
    public static SafeUvmContext context()
    {
        return CONTEXT;
    }

    /**
     * adminManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeAdminManager adminManager()
    {
        return adminManager;
    }

    /**
     * appManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeAppManager appManager()
    {
        return appManager;
    }

    /**
     * Return the existing branding manager without a separate Safe facade.
     * @return The full branding manager
     */
    @Override
    public BrandingManager brandingManager()
    {
        return delegate.get().brandingManager();
    }

    /**
     * certificateManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeCertificateManager certificateManager()
    {
        return certificateManager;
    }

    /**
     * cloudManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeCloudManager cloudManager()
    {
        return cloudManager;
    }

    /**
     * configManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeConfigManager configManager()
    {
        return configManager;
    }

    /**
     * daemonManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeDaemonManager daemonManager()
    {
        return daemonManager;
    }

    /**
     * dashboardManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeDashboardManager dashboardManager()
    {
        return dashboardManager;
    }

    /**
     * deviceTable through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeDeviceTable deviceTable()
    {
        return deviceTable;
    }

    /**
     * eventManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeEventManager eventManager()
    {
        return eventManager;
    }

    /**
     * forceTimeSync through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public int forceTimeSync()
    {
        return delegate.get().forceTimeSync();
    }

    /**
     * gc through the reviewed RPC contract.
     */
    @Override
    public void gc()
    {
        delegate.get().gc();
    }

    /**
     * geographyManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeGeographyManager geographyManager()
    {
        return geographyManager;
    }

    /**
     * getApplianceModel through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getApplianceModel()
    {
        return delegate.get().getApplianceModel();
    }

    /**
     * getCmdUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getCmdUrl()
    {
        return delegate.get().getCmdUrl();
    }

    /**
     * getConnectivityTester through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeConnectivityTester getConnectivityTester()
    {
        return getConnectivityTester;
    }

    /**
     * getFeedbackUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getFeedbackUrl()
    {
        return delegate.get().getFeedbackUrl();
    }

    /**
     * getFullVersion through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getFullVersion()
    {
        return delegate.get().getFullVersion();
    }

    /**
     * getHelpUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getHelpUrl()
    {
        return delegate.get().getHelpUrl();
    }

    /**
     * getLegalUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getLegalUrl()
    {
        return delegate.get().getLegalUrl();
    }

    /**
     * getRegionName through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getRegionName()
    {
        return delegate.get().getRegionName();
    }

    /**
     * getServerSerialNumber through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getServerSerialNumber()
    {
        return delegate.get().getServerSerialNumber();
    }

    /**
     * getServerUID through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getServerUID()
    {
        return delegate.get().getServerUID();
    }

    /**
     * getSetupStartupInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getSetupStartupInfo()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("adminManager", adminManager());
            json.put("networkManager", networkManager());
            json.put("connectivityTester", getConnectivityTester());
            json.put("systemManager", systemManager());
            json.put("mailSender", mailSender());
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot construct safe setup startup information", exception);
        }
        return json;
    }

    /**
     * getStoreUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getStoreUrl()
    {
        return delegate.get().getStoreUrl();
    }

    /**
     * getTempBackupTimer through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public long getTempBackupTimer()
    {
        return delegate.get().getTempBackupTimer();
    }

    /**
     * getWebuiStartupInfo through the reviewed RPC contract.
     * @return The delegated result
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
            json.put("skinInfo", delegate.get().skinManager().getSkinInfo());
            json.put("hostname", this.networkManager().getNetworkSettings().getHostName());
            json.put("networkSettings", this.networkManager().getNetworkSettings());
            json.put("companyName", delegate.get().brandingManager().getCompanyName());
            json.put("companyURL", delegate.get().brandingManager().getCompanyUrl());

            String serialNumber = this.getServerSerialNumber();
            if(serialNumber == null){
                serialNumber = StringUtils.EMPTY;
            }
            json.put("serverUID", this.getServerUID());
            json.put("serverSerialnumber", serialNumber );
            json.put("fullVersion", this.getFullVersion());
            json.put("fullVersionAndRevision", delegate.get().adminManager().getFullVersionAndRevision());
            json.put("regionName", this.getRegionName());
            json.put("storeUrl", this.getStoreUrl());
            json.put("helpUrl", this.getHelpUrl());
            json.put("feedbackUrl", this.getFeedbackUrl());
            json.put("isRegistered", this.isRegistered());
            json.put("isExpertMode", this.isExpertMode());
            json.put("supportEnabled", this.systemManager().getSettings().getSupportEnabled());
            json.put("timeZoneOffset", delegate.get().systemManager().getTimeZoneOffset());
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
     * getWizardSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public WizardSettings getWizardSettings()
    {
        return delegate.get().getWizardSettings();
    }

    /**
     * googleManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeGoogleManager googleManager()
    {
        return googleManager;
    }

    /**
     * hostTable through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeHostTable hostTable()
    {
        return hostTable;
    }

    /**
     * isAppliance through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isAppliance()
    {
        return delegate.get().isAppliance();
    }

    /**
     * isAts through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isAts()
    {
        return delegate.get().isAts();
    }

    /**
     * isCCHidden through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isCCHidden()
    {
        return delegate.get().isCCHidden();
    }

    /**
     * isDevel through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isDevel()
    {
        return delegate.get().isDevel();
    }

    /**
     * isDiskless through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isDiskless()
    {
        return delegate.get().isDiskless();
    }

    /**
     * isExpertMode through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isExpertMode()
    {
        return delegate.get().isExpertMode();
    }

    /**
     * isNetBoot through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isNetBoot()
    {
        return delegate.get().isNetBoot();
    }

    /**
     * isRegistered through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isRegistered()
    {
        return delegate.get().isRegistered();
    }

    /**
     * isRemoteSetup through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isRemoteSetup()
    {
        return delegate.get().isRemoteSetup();
    }

    /**
     * isStoreAvailable through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isStoreAvailable()
    {
        return delegate.get().isStoreAvailable();
    }

    /**
     * isWizardComplete through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isWizardComplete()
    {
        return delegate.get().isWizardComplete();
    }

    /**
     * languageManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeLanguageManager languageManager()
    {
        return languageManager;
    }

    /**
     * licenseManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeLicenseManager licenseManager()
    {
        return licenseManager;
    }

    /**
     * localDirectory through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeLocalDirectory localDirectory()
    {
        return localDirectory;
    }

    /**
     * logEvent through the reviewed RPC contract.
     * @param evt The evt argument
     */
    @Override
    public void logEvent(LogEvent evt)
    {
        delegate.get().logEvent(evt);
    }

    /**
     * logJavascriptException through the reviewed RPC contract.
     * @param o The o argument
     */
    @Override
    public void logJavascriptException(JSONObject o)
    {
        delegate.get().logJavascriptException(o);
    }

    /**
     * loggingManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeLoggingManager loggingManager()
    {
        return loggingManager;
    }

    /**
     * mailSender through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeMailSender mailSender()
    {
        return mailSender;
    }

    /**
     * metricManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeMetricManager metricManager()
    {
        return metricManager;
    }

    /**
     * netcapManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeNetcapManager netcapManager()
    {
        return netcapManager;
    }

    /**
     * netspaceManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeNetspaceManager netspaceManager()
    {
        return netspaceManager;
    }

    /**
     * networkManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeNetworkManager networkManager()
    {
        return networkManager;
    }

    /**
     * notificationManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeNotificationManager notificationManager()
    {
        return notificationManager;
    }

    /**
     * oemManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeOemManager oemManager()
    {
        return oemManager;
    }

    /**
     * pipelineFoundry through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafePipelineFoundry pipelineFoundry()
    {
        return pipelineFoundry;
    }

    /**
     * pluginManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafePluginManager pluginManager()
    {
        return pluginManager;
    }

    /**
     * rebootBox through the reviewed RPC contract.
     */
    @Override
    public void rebootBox()
    {
        delegate.get().rebootBox();
    }

    /**
     * sessionMonitor through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeSessionMonitor sessionMonitor()
    {
        return sessionMonitor;
    }

    /**
     * setRegistered through the reviewed RPC contract.
     */
    @Override
    public void setRegistered()
    {
        delegate.get().setRegistered();
    }

    /**
     * setRemoteSetup through the reviewed RPC contract.
     * @param enabled The enabled argument
     */
    @Override
    public void setRemoteSetup(boolean enabled)
    {
        delegate.get().setRemoteSetup(enabled);
    }

    /**
     * setWizardSettings through the reviewed RPC contract.
     * @param wizardSettings The wizardSettings argument
     */
    @Override
    public void setWizardSettings(WizardSettings wizardSettings)
    {
        delegate.get().setWizardSettings(wizardSettings);
    }

    /**
     * settingsManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeSettingsManager settingsManager()
    {
        return settingsManager;
    }

    /**
     * shutdownBox through the reviewed RPC contract.
     */
    @Override
    public void shutdownBox()
    {
        delegate.get().shutdownBox();
    }

    /**
     * state through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public UvmState state()
    {
        return delegate.get().state();
    }

    /**
     * systemManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeSystemManager systemManager()
    {
        return systemManager;
    }

    /**
     * uriManager through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeUriManager uriManager()
    {
        return uriManager;
    }

    /**
     * useTempFileSystem through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean useTempFileSystem()
    {
        return delegate.get().useTempFileSystem();
    }

    /**
     * userTable through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SafeUserTable userTable()
    {
        return userTable;
    }

    /**
     * version through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String version()
    {
        return delegate.get().version();
    }

    /**
     * wizardComplete through the reviewed RPC contract.
     */
    @Override
    public void wizardComplete()
    {
        delegate.get().wizardComplete();
    }
}
