/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import com.untangle.uvm.generic.SystemSettingsGeneric;
import com.untangle.uvm.network.NetworkSettings;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.DayOfWeekMatcher;

import org.apache.commons.lang3.StringUtils;
/**
 * System settings.
 */
@SuppressWarnings("serial")
public class SystemSettings implements Serializable, JSONString
{
    private int version = 4;
    private int httpsPort;

    private boolean supportEnabled = false;
    private boolean cloudEnabled = true;
    private boolean httpAdministrationAllowed = true;
    private String administrationSubnets = null;

    private boolean autoUpgrade;
    private DayOfWeekMatcher autoUpgradeDays;
    private int autoUpgradeHour   = 2;
    private int autoUpgradeMinute = 0;

    private SnmpSettings snmpSettings;

    private String timeSource = "ntp";
    private String timeZone = null;

    private int logRetention = 7;
    
    /**
     * These are used to indicate which certificate is assigned to each of
     * the services that are provided by this server. We assign apache.pem
     * to each by default, since that is the name of the cert that is
     * created and signed by our internal CA during the installation.
     */
    private String webCertificate = "apache.pem";
    private String mailCertificate = "apache.pem";
    private String ipsecCertificate = "apache.pem";
    private String radiusCertificate = "apache.pem";

    /**
     * These are used for LocalDirectory RADIUS server support
     */
    private boolean radiusServerEnabled = false;
    private String radiusServerSecret = "SharedSecret";

    /**
     * These are used for RADIUS proxy support
     */
    private boolean radiusProxyEnabled = false;
    private String radiusProxyServer = StringUtils.EMPTY;
    private String radiusProxyWorkgroup = StringUtils.EMPTY;
    private String radiusProxyRealm = StringUtils.EMPTY;
    private String radiusProxyUsername = StringUtils.EMPTY;
    private String radiusProxyPassword = StringUtils.EMPTY;
    private String radiusProxyEncryptedPassword = StringUtils.EMPTY;


    private String installType = "";

    public SystemSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Get whether or not local insecure administration is enabled.
     */
    public boolean getHttpAdministrationAllowed() { return this.httpAdministrationAllowed; }
    public void setHttpAdministrationAllowed( boolean newValue ) { this.httpAdministrationAllowed = newValue; }

    /**
     * Get whether or not local insecure administration is enabled.
     */
    public String getAdministrationSubnets() { return this.administrationSubnets; }
    public void setAdministrationSubnets( String newValue ) { this.administrationSubnets = newValue; }
    
    /**
     * Untangle cloud access flag
     */
    public boolean getCloudEnabled() { return this.cloudEnabled; }
    public void setCloudEnabled( boolean newValue ) { this.cloudEnabled = newValue; }

    /**
     * Untangle support access flag
     */
    public boolean getSupportEnabled() { return this.supportEnabled; }
    public void setSupportEnabled( boolean newValue ) { this.supportEnabled = newValue; }

    /**
     * The SMNP settings
     */
    public SnmpSettings getSnmpSettings() { return this.snmpSettings; }
    public void setSnmpSettings( SnmpSettings newValue ) { this.snmpSettings = newValue; }
    
    /**
     * Specifies if apt-get upgrade should be run automatically after
     * an update.
     */
    public boolean getAutoUpgrade() { return autoUpgrade; }
    public void setAutoUpgrade( boolean newValue ) { this.autoUpgrade = newValue; }

    /**
     * What days to auto-upgrade
     */
    public DayOfWeekMatcher getAutoUpgradeDays() { return autoUpgradeDays; }
    public void setAutoUpgradeDays( DayOfWeekMatcher newValue) { this.autoUpgradeDays = newValue; }

    /**
     * What time to auto-upgrade
     */
    public int getAutoUpgradeHour() { return autoUpgradeHour; }
    public void setAutoUpgradeHour( int newValue) { this.autoUpgradeHour = newValue; }

    /**
     * What time to auto-upgrade
     */
    public int getAutoUpgradeMinute() { return autoUpgradeMinute; }
    public void setAutoUpgradeMinute( int newValue) { this.autoUpgradeMinute = newValue; }

    /**
     * Get the current timeZone
     */
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    /**
     * Get the current settings version
     */
    public int getVersion() { return version; }
    public void setVersion( int newValue) { this.version = newValue; }
    
    /**
    * Get time source
    */
    public String getTimeSource(){ return timeSource; }
    public void setTimeSource( String newValue) { this.timeSource = newValue; }

    /**
    * Get log retention
    */
    public Integer getLogRetention(){ return logRetention; }
    public void setLogRetention( Integer newValue) { this.logRetention = newValue; }

    /**
     * Install Type
     */
    public String getInstallType(){ return installType; }
    public void setInstallType( String newValue) { this.installType = newValue; }

    /**
     * These are used to get and set the certificates used by the
     * different SSL secured services provided by this server
     */
    public String getWebCertificate() { return webCertificate; }
    public String getMailCertificate() { return mailCertificate; }
    public String getIpsecCertificate() { return ipsecCertificate; }
    public String getRadiusCertificate() { return radiusCertificate; }
    public void setWebCertificate(String newValue) { this.webCertificate = newValue; }
    public void setMailCertificate(String newValue) { this.mailCertificate = newValue; }
    public void setIpsecCertificate(String newValue) { this.ipsecCertificate = newValue; }
    public void setRadiusCertificate(String newValue) { this.radiusCertificate = newValue; }

    /**
     * These are used to get and set the radius server flag and secret
     */
    public boolean getRadiusServerEnabled() { return radiusServerEnabled; }
    public void setRadiusServerEnabled(boolean newValue) { this.radiusServerEnabled = newValue; }
    public String getRadiusServerSecret() { return radiusServerSecret; }
    public void setRadiusServerSecret(String newValue) { this.radiusServerSecret = newValue; }

    /**
     * These are used to get and set the radius proxy settings
     */
    public boolean getRadiusProxyEnabled() { return radiusProxyEnabled; }
    public void setRadiusProxyEnabled(boolean newValue) { this.radiusProxyEnabled = newValue; }
    public String getRadiusProxyServer() { return radiusProxyServer; }
    public void setRadiusProxyServer(String newValue) { this.radiusProxyServer = newValue; }
    public String getRadiusProxyWorkgroup() { return radiusProxyWorkgroup; }
    public void setRadiusProxyWorkgroup(String newValue) { this.radiusProxyWorkgroup = newValue; }
    public String getRadiusProxyRealm() { return radiusProxyRealm; }
    public void setRadiusProxyRealm(String newValue) { this.radiusProxyRealm = newValue; }
    public String getRadiusProxyUsername() { return radiusProxyUsername; }
    public void setRadiusProxyUsername(String newValue) { this.radiusProxyUsername = newValue; }
    public String getRadiusProxyPassword() { return radiusProxyPassword; }
    public void setRadiusProxyPassword(String newValue) { this.radiusProxyPassword = newValue; }
    public String getRadiusProxyEncryptedPassword() { return radiusProxyEncryptedPassword; }
    public void setRadiusProxyEncryptedPassword(String newValue) { this.radiusProxyEncryptedPassword = newValue; }

    /* DEPRECATED in 12.2 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    private String publicUrlMethod;
    private String publicUrlAddress;
    private int publicUrlPort = 443;
    public String deprecated_getPublicUrlMethod() { return this.publicUrlMethod; }
    public void setPublicUrlMethod( String newValue ) { this.publicUrlMethod = newValue; }
    public String deprecated_getPublicUrlAddress() { return this.publicUrlAddress; }
    public void setPublicUrlAddress( String newValue ) { this.publicUrlAddress = newValue; }
    public int deprecated_getPublicUrlPort() { return this.publicUrlPort; }
    public void setPublicUrlPort( int newValue ) { this.publicUrlPort = newValue; }
    /* DEPRECATED in 12.2 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */

    private Double thresholdTemperature = 105.0;
    public void setThresholdTemperature( Double newValue ) { this.thresholdTemperature = newValue; }
    public Double getThresholdTemperature() { return this.thresholdTemperature; }

    /**
     * Transforms a {@link SystemSettings} and {@link NetworkSettings} object
     * into its generic counterpart {@link SystemSettingsGeneric},
     * @param networkSettings {@link NetworkSettings} object to populate hostname and services fields
     * @return a new {@link SystemSettingsGeneric} instance containing the generic representation of systemSettings for vue UI
     */
    public SystemSettingsGeneric transformLegacyToGenericSettings(NetworkSettings networkSettings) {
        SystemSettingsGeneric systemSettingsGeneric = new SystemSettingsGeneric();
        systemSettingsGeneric.setCCHidden(UvmContextFactory.context().isCCHidden());

        if (networkSettings != null) {
            // Local Services Settings
            systemSettingsGeneric.setHttpPort(networkSettings.getHttpPort());
            systemSettingsGeneric.setHttpsPort(networkSettings.getHttpsPort());

            // Hostname Settings
            systemSettingsGeneric.setHostName(networkSettings.getHostName());
            systemSettingsGeneric.setDomainName(networkSettings.getDomainName());

            systemSettingsGeneric.setDynamicDnsServiceEnabled(networkSettings.getDynamicDnsServiceEnabled());
            systemSettingsGeneric.setDynamicDnsServiceUsername(networkSettings.getDynamicDnsServiceUsername());
            systemSettingsGeneric.setDynamicDnsServiceName(networkSettings.getDynamicDnsServiceName());
            systemSettingsGeneric.setDynamicDnsServiceHostnames(networkSettings.getDynamicDnsServiceHostnames());
            systemSettingsGeneric.setDynamicDnsServicePassword(networkSettings.getDynamicDnsServicePassword());
            systemSettingsGeneric.setDynamicDnsServiceZone(networkSettings.getDynamicDnsServiceZone());
            systemSettingsGeneric.setDynamicDnsServiceWan(networkSettings.getDynamicDnsServiceWan());

            systemSettingsGeneric.setPublicUrlAddress(networkSettings.getPublicUrlAddress());
            systemSettingsGeneric.setPublicUrlMethod(networkSettings.getPublicUrlMethod());
            systemSettingsGeneric.setPublicUrlPort(networkSettings.getPublicUrlPort());
        }
        systemSettingsGeneric.setTimeZone(new SystemSettingsGeneric.TimeZone(this.timeZone, StringUtils.EMPTY));

        systemSettingsGeneric.setCloudEnabled(this.getCloudEnabled());
        systemSettingsGeneric.setSupportEnabled(this.getSupportEnabled());
        return systemSettingsGeneric;
    }
}
