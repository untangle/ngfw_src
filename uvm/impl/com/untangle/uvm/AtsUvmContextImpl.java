package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsUvmContextImpl implements AtsUvmContext
{
    private final SafeUvmContext safe;
    private final AtsAppManager appManager;
    private final AtsSystemManager systemManager;
    private final AtsNetworkManager networkManager;
    private final AtsCertificateManager certificateManager;
    private final AtsCloudManager cloudManager;
    private final AtsConfigManager configManager;
    private final AtsGoogleManager googleManager;
    private final AtsGeographyManager geographyManager;
    private final AtsNetspaceManager netspaceManager;
    private final AtsMetricManager metricManager;
    private final AtsUriManager uriManager;
    private final AtsHostTable hostTable;
    private final AtsDeviceTable deviceTable;
    private final AtsUserTable userTable;

    /**
     * Create the Java-only ATS context without registering an RPC endpoint.
     * @param delegate The lazy internal context
     */
    AtsUvmContextImpl(Supplier<UvmContext> delegate)
    {
        Objects.requireNonNull(delegate, "delegate");
        this.safe = new SafeUvmContextImpl(delegate);
        this.appManager = new AtsAppManagerImpl(safe.appManager(), () -> delegate.get().appManager());
        this.systemManager = new AtsSystemManagerImpl(safe.systemManager(), () -> delegate.get().systemManager());
        this.networkManager = new AtsNetworkManagerImpl(safe.networkManager(), () -> delegate.get().networkManager());
        this.certificateManager = new AtsCertificateManagerImpl(safe.certificateManager(), () -> delegate.get().certificateManager());
        this.cloudManager = new AtsCloudManagerImpl(safe.cloudManager(), () -> delegate.get().cloudManager());
        this.configManager = new AtsConfigManagerImpl(safe.configManager(), () -> delegate.get().configManager());
        this.googleManager = new AtsGoogleManagerImpl(safe.googleManager(), () -> delegate.get().googleManager());
        this.geographyManager = new AtsGeographyManagerImpl(safe.geographyManager(), () -> delegate.get().geographyManager());
        this.netspaceManager = new AtsNetspaceManagerImpl(safe.netspaceManager(), () -> delegate.get().netspaceManager());
        this.metricManager = new AtsMetricManagerImpl(safe.metricManager(), () -> delegate.get().metricManager());
        this.uriManager = new AtsUriManagerImpl(safe.uriManager(), () -> delegate.get().uriManager());
        this.hostTable = new AtsHostTableImpl(safe.hostTable(), () -> delegate.get().hostTable());
        this.deviceTable = new AtsDeviceTableImpl(safe.deviceTable(), () -> delegate.get().deviceTable());
        this.userTable = new AtsUserTableImpl(safe.userTable(), () -> delegate.get().userTable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeAdminManager adminManager()
    {
        return safe.adminManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsAppManager appManager()
    {
        return appManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BrandingManager brandingManager()
    {
        return safe.brandingManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsCertificateManager certificateManager()
    {
        return certificateManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsCloudManager cloudManager()
    {
        return cloudManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsConfigManager configManager()
    {
        return configManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeDaemonManager daemonManager()
    {
        return safe.daemonManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeDashboardManager dashboardManager()
    {
        return safe.dashboardManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsDeviceTable deviceTable()
    {
        return deviceTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeEventManager eventManager()
    {
        return safe.eventManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int forceTimeSync()
    {
        return safe.forceTimeSync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gc()
    {
        safe.gc();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsGeographyManager geographyManager()
    {
        return geographyManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApplianceModel()
    {
        return safe.getApplianceModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCmdUrl()
    {
        return safe.getCmdUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeConnectivityTester getConnectivityTester()
    {
        return safe.getConnectivityTester();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFeedbackUrl()
    {
        return safe.getFeedbackUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullVersion()
    {
        return safe.getFullVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpUrl()
    {
        return safe.getHelpUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLegalUrl()
    {
        return safe.getLegalUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRegionName()
    {
        return safe.getRegionName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerSerialNumber()
    {
        return safe.getServerSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerUID()
    {
        return safe.getServerUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getSetupStartupInfo()
    {
        return safe.getSetupStartupInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStoreUrl()
    {
        return safe.getStoreUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTempBackupTimer()
    {
        return safe.getTempBackupTimer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getWebuiStartupInfo()
    {
        return safe.getWebuiStartupInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WizardSettings getWizardSettings()
    {
        return safe.getWizardSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsGoogleManager googleManager()
    {
        return googleManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsHostTable hostTable()
    {
        return hostTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppliance()
    {
        return safe.isAppliance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAts()
    {
        return safe.isAts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCCHidden()
    {
        return safe.isCCHidden();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDevel()
    {
        return safe.isDevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDiskless()
    {
        return safe.isDiskless();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExpertMode()
    {
        return safe.isExpertMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNetBoot()
    {
        return safe.isNetBoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered()
    {
        return safe.isRegistered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemoteSetup()
    {
        return safe.isRemoteSetup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreAvailable()
    {
        return safe.isStoreAvailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWizardComplete()
    {
        return safe.isWizardComplete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeLanguageManager languageManager()
    {
        return safe.languageManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeLicenseManager licenseManager()
    {
        return safe.licenseManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeLocalDirectory localDirectory()
    {
        return safe.localDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logEvent(LogEvent evt)
    {
        safe.logEvent(evt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logJavascriptException(JSONObject o)
    {
        safe.logJavascriptException(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeLoggingManager loggingManager()
    {
        return safe.loggingManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeMailSender mailSender()
    {
        return safe.mailSender();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsMetricManager metricManager()
    {
        return metricManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeNetcapManager netcapManager()
    {
        return safe.netcapManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsNetspaceManager netspaceManager()
    {
        return netspaceManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsNetworkManager networkManager()
    {
        return networkManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeNotificationManager notificationManager()
    {
        return safe.notificationManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeOemManager oemManager()
    {
        return safe.oemManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafePipelineFoundry pipelineFoundry()
    {
        return safe.pipelineFoundry();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafePluginManager pluginManager()
    {
        return safe.pluginManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebootBox()
    {
        safe.rebootBox();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeSessionMonitor sessionMonitor()
    {
        return safe.sessionMonitor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegistered()
    {
        safe.setRegistered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRemoteSetup(boolean enabled)
    {
        safe.setRemoteSetup(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWizardSettings(WizardSettings wizardSettings)
    {
        safe.setWizardSettings(wizardSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeSettingsManager settingsManager()
    {
        return safe.settingsManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdownBox()
    {
        safe.shutdownBox();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UvmState state()
    {
        return safe.state();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsSystemManager systemManager()
    {
        return systemManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsUriManager uriManager()
    {
        return uriManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean useTempFileSystem()
    {
        return safe.useTempFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtsUserTable userTable()
    {
        return userTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String version()
    {
        return safe.version();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void wizardComplete()
    {
        safe.wizardComplete();
    }
}
