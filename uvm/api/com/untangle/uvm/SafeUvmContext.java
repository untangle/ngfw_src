/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;

/**
 * Reviewed callable operations for SafeUvmContext.
 */
public interface SafeUvmContext
{
    /**
     * adminManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeAdminManager adminManager();

    /**
     * appManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeAppManager appManager();

    /**
     * All branding getters are intentionally available over JSON-RPC, so this
     * manager returns its full interface without a separate Safe facade.
     * @return The full branding manager
     */
    BrandingManager brandingManager();

    /**
     * certificateManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeCertificateManager certificateManager();

    /**
     * cloudManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeCloudManager cloudManager();

    /**
     * configManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeConfigManager configManager();

    /**
     * daemonManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeDaemonManager daemonManager();

    /**
     * dashboardManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeDashboardManager dashboardManager();

    /**
     * deviceTable through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeDeviceTable deviceTable();

    /**
     * eventManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeEventManager eventManager();

    /**
     * forceTimeSync through the reviewed RPC contract.
     * @return The delegated result
     */
    int forceTimeSync();

    /**
     * gc through the reviewed RPC contract.
     */
    void gc();

    /**
     * geographyManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeGeographyManager geographyManager();

    /**
     * getApplianceModel through the reviewed RPC contract.
     * @return The delegated result
     */
    String getApplianceModel();

    /**
     * getCmdUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getCmdUrl();

    /**
     * getConnectivityTester through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeConnectivityTester getConnectivityTester();

    /**
     * getFeedbackUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getFeedbackUrl();

    /**
     * getFullVersion through the reviewed RPC contract.
     * @return The delegated result
     */
    String getFullVersion();

    /**
     * getHelpUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getHelpUrl();

    /**
     * getLegalUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getLegalUrl();

    /**
     * getRegionName through the reviewed RPC contract.
     * @return The delegated result
     */
    String getRegionName();

    /**
     * getServerSerialNumber through the reviewed RPC contract.
     * @return The delegated result
     */
    String getServerSerialNumber();

    /**
     * getServerUID through the reviewed RPC contract.
     * @return The delegated result
     */
    String getServerUID();

    /**
     * getSetupStartupInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getSetupStartupInfo();

    /**
     * getStoreUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getStoreUrl();

    /**
     * getTempBackupTimer through the reviewed RPC contract.
     * @return The delegated result
     */
    long getTempBackupTimer();

    /**
     * getWebuiStartupInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getWebuiStartupInfo();

    /**
     * getWizardSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    WizardSettings getWizardSettings();

    /**
     * googleManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeGoogleManager googleManager();

    /**
     * hostTable through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeHostTable hostTable();

    /**
     * isAppliance through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isAppliance();

    /**
     * isAts through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isAts();

    /**
     * isCCHidden through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isCCHidden();

    /**
     * isDevel through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isDevel();

    /**
     * isDiskless through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isDiskless();

    /**
     * isExpertMode through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isExpertMode();

    /**
     * isNetBoot through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isNetBoot();

    /**
     * isRegistered through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isRegistered();

    /**
     * isRemoteSetup through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isRemoteSetup();

    /**
     * isStoreAvailable through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isStoreAvailable();

    /**
     * isWizardComplete through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isWizardComplete();

    /**
     * languageManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeLanguageManager languageManager();

    /**
     * licenseManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeLicenseManager licenseManager();

    /**
     * localDirectory through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeLocalDirectory localDirectory();

    /**
     * logEvent through the reviewed RPC contract.
     * @param evt The evt argument
     */
    void logEvent(LogEvent evt);

    /**
     * logJavascriptException through the reviewed RPC contract.
     * @param o The o argument
     */
    void logJavascriptException(JSONObject o);

    /**
     * loggingManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeLoggingManager loggingManager();

    /**
     * mailSender through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeMailSender mailSender();

    /**
     * metricManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeMetricManager metricManager();

    /**
     * netcapManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeNetcapManager netcapManager();

    /**
     * netspaceManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeNetspaceManager netspaceManager();

    /**
     * networkManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeNetworkManager networkManager();

    /**
     * notificationManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeNotificationManager notificationManager();

    /**
     * oemManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeOemManager oemManager();

    /**
     * pipelineFoundry through the reviewed RPC contract.
     * @return The delegated result
     */
    SafePipelineFoundry pipelineFoundry();

    /**
     * pluginManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafePluginManager pluginManager();

    /**
     * rebootBox through the reviewed RPC contract.
     */
    void rebootBox();

    /**
     * sessionMonitor through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeSessionMonitor sessionMonitor();

    /**
     * setRegistered through the reviewed RPC contract.
     */
    void setRegistered();

    /**
     * setRemoteSetup through the reviewed RPC contract.
     * @param enabled The enabled argument
     */
    void setRemoteSetup(boolean enabled);

    /**
     * setWizardSettings through the reviewed RPC contract.
     * @param wizardSettings The wizardSettings argument
     */
    void setWizardSettings(WizardSettings wizardSettings);

    /**
     * settingsManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeSettingsManager settingsManager();

    /**
     * shutdownBox through the reviewed RPC contract.
     */
    void shutdownBox();

    /**
     * state through the reviewed RPC contract.
     * @return The delegated result
     */
    UvmState state();

    /**
     * systemManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeSystemManager systemManager();

    /**
     * uriManager through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeUriManager uriManager();

    /**
     * useTempFileSystem through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean useTempFileSystem();

    /**
     * userTable through the reviewed RPC contract.
     * @return The delegated result
     */
    SafeUserTable userTable();

    /**
     * version through the reviewed RPC contract.
     * @return The delegated result
     */
    String version();

    /**
     * wizardComplete through the reviewed RPC contract.
     */
    void wizardComplete();
}
